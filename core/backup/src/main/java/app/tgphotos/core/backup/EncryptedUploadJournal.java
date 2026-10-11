package app.tgphotos.core.backup;

import app.tgphotos.core.crypto.AeadEnvelope;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;

/**
 * Encrypted write-ahead chunk journal. One owner instance per trusted app-private directory.
 * The supplied independent journal key must come from the cloud key hierarchy, never file/manifest keys.
 * No plaintext fingerprints, pointers, user paths or keys are persisted. Opaque identities/size are visible.
 * Missing/corrupt records never trigger automatic key regeneration. Not a remote deduplication guarantee.
 * Android filesystem/power-loss/multiprocess QA and WorkManager integration remain required.
 */
public final class EncryptedUploadJournal implements EncryptedBackupCoordinator.Checkpoints, AutoCloseable {
    private static final int FORMAT = 1;
    private static final int MAX_RECORD = AeadEnvelope.MAX_PLAINTEXT_BYTES;
    private static final byte[] DOMAIN = "TGPhotos/UploadJournal/v1\0".getBytes(StandardCharsets.US_ASCII);
    private final Path directory;
    private final byte[] key;
    private final long quota;
    private final AeadEnvelope cipher = new AeadEnvelope();
    private boolean closed;

    public EncryptedUploadJournal(Path trustedPrivateDirectory, byte[] journalKey, long quotaBytes) throws IOException {
        if (trustedPrivateDirectory == null || journalKey == null || journalKey.length != 32 ||
                quotaBytes < 1024 || quotaBytes > 1024L * 1024 * 1024) throw new IllegalArgumentException("Invalid journal configuration");
        directory = trustedPrivateDirectory.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(directory)) throw new IOException("Journal directory cannot be a symbolic link");
        Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Private journal unavailable");
        Files.setPosixFilePermissions(directory, PosixFilePermissions.fromString("rwx------"));
        key = journalKey.clone(); quota = quotaBytes;
        // Interrupted temp files contain ciphertext only. They are never treated as prepared state.
    }

    @Override public synchronized EncryptedBackupCoordinator.Prepared findPrepared(UUID id, long version, int index) throws IOException {
        Entry entry = read(id, version, index);
        return entry == null ? null : entry.prepared;
    }

    @Override public synchronized EncryptedBackupCoordinator.Checkpoint find(UUID id, long version, int index) throws IOException {
        Entry entry = read(id, version, index);
        if (entry == null || entry.pointer == null) return null;
        return new EncryptedBackupCoordinator.Checkpoint(entry.prepared.plaintextSha256(), entry.prepared.plaintextBytes, entry.pointer);
    }

    @Override public synchronized void prepare(UUID id, long version, int index, EncryptedBackupCoordinator.Prepared prepared) throws IOException {
        if (prepared == null) throw new IllegalArgumentException("Prepared upload required");
        Entry previous = read(id, version, index);
        if (previous != null) {
            if (!same(previous.prepared, prepared)) throw new IOException("Prepared identity cannot be replaced");
            return;
        }
        write(id, version, index, new Entry(prepared, null));
    }

    @Override public synchronized void save(UUID id, long version, int index, EncryptedBackupCoordinator.Checkpoint checkpoint) throws IOException {
        Entry previous = read(id, version, index);
        if (previous == null || checkpoint == null || checkpoint.plaintextBytes != previous.prepared.plaintextBytes ||
                !MessageDigest.isEqual(checkpoint.plaintextSha256, previous.prepared.plaintextSha256())) {
            throw new IOException("Acknowledgement has no matching prepared upload");
        }
        validatePointer(checkpoint.pointer);
        if (previous.pointer != null) {
            if (!previous.pointer.equals(checkpoint.pointer)) throw new IOException("Confirmed pointer cannot be replaced");
            return;
        }
        write(id, version, index, new Entry(previous.prepared, checkpoint.pointer));
    }

    private Entry read(UUID id, long version, int index) throws IOException {
        Path path = path(id, version, index);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        byte[] envelope;
        try (FileChannel input = FileChannel.open(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            long length = input.size();
            if (length < AeadEnvelope.ENVELOPE_OVERHEAD_BYTES || length > MAX_RECORD + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES)
                throw new IOException("Invalid encrypted journal length");
            envelope = new byte[(int) length];
            ByteBuffer target = ByteBuffer.wrap(envelope);
            while (target.hasRemaining()) if (input.read(target) < 0) throw new IOException("Truncated encrypted journal");
            if (input.read(ByteBuffer.allocate(1)) != -1) throw new IOException("Trailing encrypted journal bytes");
        }
        byte[] plaintext;
        try { plaintext = cipher.open(key, envelope, context(id, version, index)); }
        catch (GeneralSecurityException e) { throw new IOException("Journal authentication failed"); }
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(plaintext));
            if (input.readInt() != FORMAT) throw new IOException("Unsupported journal format");
            byte[] hash = new byte[32]; input.readFully(hash);
            int bytes = input.readInt();
            UUID operation = new UUID(input.readLong(), input.readLong());
            int length = input.readInt();
            if (bytes < 1 || bytes > MAX_RECORD || length != bytes + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES || length > input.available())
                throw new IOException("Invalid prepared journal payload");
            byte[] ciphertext = new byte[length]; input.readFully(ciphertext);
            String pointer = input.readUTF();
            if (!pointer.isEmpty()) validatePointer(pointer);
            if (input.available() != 0) throw new IOException("Trailing journal payload");
            return new Entry(new EncryptedBackupCoordinator.Prepared(hash, bytes, operation, ciphertext), pointer.isEmpty() ? null : pointer);
        } finally { Arrays.fill(plaintext, (byte) 0); }
    }

    private void write(UUID id, long version, int index, Entry entry) throws IOException {
        Path target = path(id, version, index);
        byte[] payload;
        byte[] ciphertext = entry.prepared.ciphertext();
        long estimated = 4L + 32 + 4 + 16 + 4 + ciphertext.length + 2 + (entry.pointer == null ? 0 : entry.pointer.length());
        if (estimated > MAX_RECORD) throw new IOException("Journal chunk exceeds record bound");
        WipingBuffer bytes = new WipingBuffer((int) estimated);
        DataOutputStream output = new DataOutputStream(bytes);
        try {
        output.writeInt(FORMAT); output.write(entry.prepared.plaintextSha256()); output.writeInt(entry.prepared.plaintextBytes);
        output.writeLong(entry.prepared.operationId.getMostSignificantBits()); output.writeLong(entry.prepared.operationId.getLeastSignificantBits());
        output.writeInt(ciphertext.length); output.write(ciphertext); output.writeUTF(entry.pointer == null ? "" : entry.pointer); output.flush();
        payload = bytes.toByteArray();
        } finally { bytes.wipe(); }
        byte[] envelope;
        try { envelope = cipher.seal(key, payload, context(id, version, index)); }
        catch (GeneralSecurityException e) { throw new IOException("Journal encryption failed"); }
        finally { Arrays.fill(payload, (byte) 0); }
        long retained = 0;
        try (var entries = Files.newDirectoryStream(directory)) {
            for (Path path : entries) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid private journal entry");
                if (!path.equals(target)) retained = Math.addExact(retained, Files.size(path));
            }
        }
        if (retained > quota - envelope.length) throw new IOException("Encrypted upload journal quota exceeded");
        Path temporary = Files.createTempFile(directory, "upload-", ".pending",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.wrap(envelope);
                while (buffer.hasRemaining()) file.write(buffer);
                file.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            try (FileChannel parent = FileChannel.open(directory, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) { parent.force(true); }
        } finally { Files.deleteIfExists(temporary); }
    }

    private Path path(UUID id, long version, int index) throws IOException {
        if (closed) throw new IOException("Upload journal locked");
        if (id == null || version <= 0 || index < 0 || index > 100000) throw new IllegalArgumentException("Invalid journal identity");
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Private journal directory changed");
        Path path = directory.resolve(id + "-" + version + "-" + index + ".upload");
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS) && !Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Invalid journal entry");
        return path;
    }

    private static byte[] context(UUID id, long version, int index) {
        return ByteBuffer.allocate(DOMAIN.length + 16 + 8 + 4).put(DOMAIN)
                .putLong(id.getMostSignificantBits()).putLong(id.getLeastSignificantBits()).putLong(version).putInt(index).array();
    }
    private static void validatePointer(String pointer) throws IOException {
        if (pointer == null || pointer.isEmpty() || pointer.length() > 1024 || !pointer.matches("[A-Za-z0-9_:/.-]+"))
            throw new IOException("Invalid opaque upload pointer");
    }
    private static boolean same(EncryptedBackupCoordinator.Prepared a, EncryptedBackupCoordinator.Prepared b) {
        return a.operationId.equals(b.operationId) && a.plaintextBytes == b.plaintextBytes &&
                MessageDigest.isEqual(a.plaintextSha256(), b.plaintextSha256()) && Arrays.equals(a.ciphertext(), b.ciphertext());
    }
    @Override public synchronized void close() { closed = true; Arrays.fill(key, (byte) 0); }
    private static final class WipingBuffer extends ByteArrayOutputStream {
        WipingBuffer(int capacity) { super(capacity); }
        void wipe() { Arrays.fill(buf, (byte) 0); reset(); }
    }
    private static final class Entry {
        final EncryptedBackupCoordinator.Prepared prepared;
        final String pointer;
        Entry(EncryptedBackupCoordinator.Prepared prepared, String pointer) { this.prepared = prepared; this.pointer = pointer; }
    }
}
