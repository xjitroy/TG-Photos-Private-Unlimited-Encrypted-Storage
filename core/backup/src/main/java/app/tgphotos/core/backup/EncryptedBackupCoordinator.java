package app.tgphotos.core.backup;

import app.tgphotos.core.crypto.AeadEnvelope;
import app.tgphotos.core.crypto.ChunkContext;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Draft bounded backup protocol. Transport/checkpoints must be owner-authorized and encrypted.
 * Not wired to WorkManager/TDLib. Android durability and transport reconciliation remain pending.
 */
public final class EncryptedBackupCoordinator {
    public static final int DEFAULT_CHUNK_BYTES = 1024 * 1024;
    private static final int MAX_CHUNKS = 100000;
    private static final int MAX_METADATA_BYTES = 65536;
    private static final int MAX_POINTER_BYTES = 1024;

    public interface Transport {
        /** Returns only after authoritative upload completion. Opaque identifier; no real filename/caption. */
        String uploadCiphertext(String opaqueName, byte[] ciphertext) throws IOException;
        /** Fetches from remote, never substitutes the local upload cache. Bound maximumResponseBytes. */
        byte[] downloadCiphertext(String pointer, int maximumResponseBytes) throws IOException;
    }
    public interface Checkpoints {
        /** Persist implementation must encrypt this record, including the content fingerprint and pointer. */
        Checkpoint find(UUID fileId, long version, int index) throws IOException;
        void save(UUID fileId, long version, int index, Checkpoint checkpoint) throws IOException;
        Prepared findPrepared(UUID fileId, long version, int index) throws IOException;
        /** Durably retain ciphertext and operation identity BEFORE contacting the remote service. */
        void prepare(UUID fileId, long version, int index, Prepared prepared) throws IOException;
    }
    public static final class Prepared {
        private final byte[] hash;
        private final byte[] ciphertext;
        public final int plaintextBytes;
        public final UUID operationId;
        public Prepared(byte[] hash, int bytes, UUID operationId, byte[] ciphertext) {
            if (hash == null || hash.length != 32 || bytes < 1 || bytes > AeadEnvelope.MAX_PLAINTEXT_BYTES ||
                    operationId == null || ciphertext == null || ciphertext.length != bytes + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES) {
                throw new IllegalArgumentException("Invalid prepared chunk");
            }
            this.hash = hash.clone(); this.plaintextBytes = bytes;
            this.operationId = operationId; this.ciphertext = ciphertext.clone();
        }
        public byte[] plaintextSha256() { return hash.clone(); }
        public byte[] ciphertext() { return ciphertext.clone(); }
    }
    public static final class Checkpoint {
        public final byte[] plaintextSha256;
        public final int plaintextBytes;
        public final String pointer;
        public Checkpoint(byte[] hash, int bytes, String pointer) {
            this.plaintextSha256 = hash.clone(); this.plaintextBytes = bytes; this.pointer = pointer;
        }
    }
    public static final class Commit {
        public final UUID fileId;
        public final long version;
        public final long bytes;
        public final String manifestPointer;
        public final int manifestPlaintextBytes;
        private Commit(UUID id, long version, long bytes, String pointer, int manifestBytes) {
            this.fileId = id; this.version = version; this.bytes = bytes;
            this.manifestPointer = pointer; this.manifestPlaintextBytes = manifestBytes;
        }
    }
    public static final class Verification {
        public final UUID fileId;
        public final long version;
        public final long restoredBytes;
        private final byte[] sha256;
        private Verification(UUID id, long version, long bytes, byte[] hash) {
            this.fileId = id; this.version = version; this.restoredBytes = bytes; this.sha256 = hash.clone();
        }
        public byte[] sha256() { return sha256.clone(); }
        // Not device deletion consent. No deletion API exists in this coordinator.
    }
    private static final class Chunk {
        final int bytes; final String pointer;
        Chunk(int bytes, String pointer) { this.bytes = bytes; this.pointer = pointer; }
    }
    private final AeadEnvelope aead;
    private final Transport transport;
    private final Checkpoints checkpoints;
    private final int chunkBytes;

    public EncryptedBackupCoordinator(AeadEnvelope aead, Transport transport, Checkpoints checkpoints, int chunkBytes) {
        if (aead == null || transport == null || checkpoints == null || chunkBytes < 1 || chunkBytes > AeadEnvelope.MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Invalid backup configuration");
        }
        this.aead = aead; this.transport = transport; this.checkpoints = checkpoints; this.chunkBytes = chunkBytes;
    }

    /** Stream source once per attempt. Re-reading on retry detects changed content; caller owns stream/key lifecycle. */
    public Commit upload(InputStream source, UUID id, long version, byte[] fileKey, byte[] manifestKey,
                         byte[] sensitiveMetadata) throws IOException, GeneralSecurityException {
        validateKeys(fileKey, manifestKey);
        if (source == null || id == null || version <= 0 || sensitiveMetadata == null || sensitiveMetadata.length > MAX_METADATA_BYTES) {
            throw new IllegalArgumentException("Invalid backup source");
        }
        MessageDigest fullHash = sha256();
        List<Chunk> chunks = new ArrayList<>();
        long total = 0;
        byte[] buffer = new byte[chunkBytes];
        try {
            while (true) {
                int count = readChunk(source, buffer);
                if (count == 0) break;
                if (chunks.size() >= MAX_CHUNKS) throw new IOException("Backup exceeds manifest chunk limit");
                byte[] plaintext = Arrays.copyOf(buffer, count);
                try {
                    fullHash.update(plaintext);
                    byte[] chunkHash = sha256().digest(plaintext);
                    int index = chunks.size();
                    Checkpoint saved = checkpoints.find(id, version, index);
                    String pointer;
                    if (saved != null) {
                        if (saved.plaintextBytes != count || !MessageDigest.isEqual(saved.plaintextSha256, chunkHash)) {
                            throw new IOException("Source changed during resumable backup; create a fresh content version/key");
                        }
                        pointer = saved.pointer;
                        validatePointer(pointer);
                        byte[] remoteEnvelope = transport.downloadCiphertext(pointer, count + 64);
                        byte[] remotePlaintext = aead.open(fileKey, remoteEnvelope,
                                new ChunkContext(id, version, index, count, ChunkContext.Scope.FILE).encode());
                        try {
                            if (!MessageDigest.isEqual(sha256().digest(remotePlaintext), chunkHash)) {
                                throw new IOException("Resumable chunk failed independent verification");
                            }
                        } finally { Arrays.fill(remotePlaintext, (byte) 0); }
                    } else {
                        byte[] context = new ChunkContext(id, version, index, count, ChunkContext.Scope.FILE).encode();
                        Prepared prepared = checkpoints.findPrepared(id, version, index);
                        if (prepared == null) {
                            prepared = new Prepared(chunkHash, count, UUID.randomUUID(), aead.seal(fileKey, plaintext, context));
                            checkpoints.prepare(id, version, index, prepared);
                        }
                        if (prepared.plaintextBytes != count || !MessageDigest.isEqual(prepared.plaintextSha256(), chunkHash)) {
                            throw new IOException("Source changed after upload preparation; create a fresh content version/key");
                        }
                        byte[] ciphertext = prepared.ciphertext();
                        // Authenticate retained ciphertext with the current file key before any retry upload.
                        byte[] retained = aead.open(fileKey, ciphertext, context);
                        try {
                            if (!MessageDigest.isEqual(sha256().digest(retained), chunkHash)) throw new IOException("Prepared chunk mismatch");
                        } finally { Arrays.fill(retained, (byte) 0); }
                        // Transport must reconcile this identity after an uncertain acknowledgement.
                        // Telegram's filename alone does NOT guarantee remote deduplication.
                        pointer = transport.uploadCiphertext(prepared.operationId.toString(), ciphertext);
                        validatePointer(pointer);
                        checkpoints.save(id, version, index, new Checkpoint(chunkHash, count, pointer));
                    }
                    validatePointer(pointer);
                    chunks.add(new Chunk(count, pointer));
                    total = Math.addExact(total, count);
                } finally { Arrays.fill(plaintext, (byte) 0); }
            }
        } finally { Arrays.fill(buffer, (byte) 0); }
        // A previously uploaded trailing chunk indicates a shorter source, not an eligible completed version.
        if (checkpoints.find(id, version, chunks.size()) != null || checkpoints.findPrepared(id, version, chunks.size()) != null)
            throw new IOException("Source was truncated during resumable backup");
        byte[] manifest = encodeManifest(id, version, total, fullHash.digest(), sensitiveMetadata, chunks);
        try {
            byte[] encrypted = aead.seal(manifestKey, manifest,
                    new ChunkContext(id, version, 0, manifest.length, ChunkContext.Scope.MANIFEST).encode());
            String pointer = transport.uploadCiphertext(UUID.randomUUID().toString(), encrypted);
            validatePointer(pointer);
            return new Commit(id, version, total, pointer, manifest.length);
        } finally { Arrays.fill(manifest, (byte) 0); }
    }

    /** Authenticates a remote manifest for viewing/seeking; does NOT prove complete file restore. */
    public AuthenticatedManifest openManifest(Commit commit, byte[] manifestKey) throws IOException, GeneralSecurityException {
        if (commit == null || manifestKey == null || manifestKey.length != 32) throw new IllegalArgumentException("Invalid manifest request");
        int maximum = Math.addExact(commit.manifestPlaintextBytes, AeadEnvelope.ENVELOPE_OVERHEAD_BYTES);
        byte[] encrypted = transport.downloadCiphertext(commit.manifestPointer, maximum);
        if (encrypted == null || encrypted.length != maximum) throw new IOException("Invalid remote manifest length");
        byte[] manifest = aead.open(manifestKey, encrypted,
                new ChunkContext(commit.fileId, commit.version, 0, commit.manifestPlaintextBytes, ChunkContext.Scope.MANIFEST).encode());
        try { return AuthenticatedManifest.parse(manifest, commit); }
        finally { Arrays.fill(manifest, (byte) 0); }
    }

    /** Independent complete remote authentication+whole-file hash; never substitutes viewing for deletion proof. */
    public Verification verifyRemote(Commit commit, byte[] fileKey, byte[] manifestKey) throws IOException, GeneralSecurityException {
        validateKeys(fileKey, manifestKey);
        try (AuthenticatedManifest manifest = openManifest(commit, manifestKey)) {
            int[] lengths = manifest.chunkLengths(); String[] pointers = manifest.chunkPointers();
            MessageDigest restoredHash = sha256(); long bytes = 0;
            for (int index = 0; index < lengths.length; index++) {
                int maximum = lengths[index] + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES;
                byte[] encrypted = transport.downloadCiphertext(pointers[index], maximum);
                if (encrypted == null || encrypted.length != maximum) throw new IOException("Invalid remote chunk length");
                byte[] plaintext = aead.open(fileKey, encrypted,
                        new ChunkContext(commit.fileId, commit.version, index, lengths[index], ChunkContext.Scope.FILE).encode());
                try {
                    if (plaintext.length != lengths[index]) throw new IOException("Restored chunk length mismatch");
                    restoredHash.update(plaintext); bytes = Math.addExact(bytes, plaintext.length);
                } finally { Arrays.fill(plaintext, (byte) 0); }
            }
            byte[] actualHash = restoredHash.digest();
            if (bytes != commit.bytes || !MessageDigest.isEqual(manifest.sha256(), actualHash))
                throw new IOException("Complete remote restore verification failed");
            return new Verification(commit.fileId, commit.version, bytes, actualHash);
        }
    }

    private static byte[] encodeManifest(UUID id, long version, long bytes, byte[] hash, byte[] metadata, List<Chunk> chunks) throws IOException {
        // Bound before allocation: writeUTF pointers are restricted to ASCII and at most 1024 bytes.
        long estimated = 4 + 16 + 8 + 8 + 32 + 4 + metadata.length + 4;
        for (Chunk chunk : chunks) estimated += 4 + 2 + chunk.pointer.length();
        if (estimated > AeadEnvelope.MAX_PLAINTEXT_BYTES) throw new IOException("Manifest exceeds encryption envelope limit");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream((int) estimated);
        DataOutputStream out = new DataOutputStream(buffer);
        out.writeInt(1); out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits());
        out.writeLong(version); out.writeLong(bytes); out.write(hash); out.writeInt(metadata.length); out.write(metadata);
        out.writeInt(chunks.size());
        for (Chunk chunk : chunks) { out.writeInt(chunk.bytes); out.writeUTF(chunk.pointer); }
        out.flush();
        return buffer.toByteArray();
    }
    private static int readChunk(InputStream source, byte[] buffer) throws IOException {
        int count = 0;
        while (count < buffer.length) {
            int read = source.read(buffer, count, buffer.length - count);
            if (read < 0) break;
            if (read == 0) {
                int one = source.read(); if (one < 0) break; buffer[count++] = (byte) one;
            } else count += read;
        }
        return count;
    }
    private static void validatePointer(String pointer) throws IOException {
        if (pointer == null || pointer.isEmpty() || pointer.length() > MAX_POINTER_BYTES || !pointer.matches("[A-Za-z0-9_:/.-]+")) {
            throw new IOException("Invalid opaque transport pointer");
        }
    }
    private static void validateKeys(byte[] fileKey, byte[] manifestKey) {
        if (fileKey == null || manifestKey == null || fileKey.length != 32 || manifestKey.length != 32 || MessageDigest.isEqual(fileKey, manifestKey)) {
            throw new IllegalArgumentException("Separate 256-bit file and manifest keys are required");
        }
    }
    private static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
