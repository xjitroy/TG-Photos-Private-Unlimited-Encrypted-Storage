package app.tgphotos.core.cache;

import app.tgphotos.core.crypto.AeadEnvelope;
import app.tgphotos.core.crypto.ChunkContext;
import app.tgphotos.core.crypto.ScopedKeyHierarchy;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Draft bounded ciphertext-only disk cache. One owner instance per app-private directory.
 * Caller supplies trusted manifest version/length; decryption returns authenticated bytes in memory.
 * Not app-wired; Android filesystem/provider, quota orchestration and lifecycle QA remain required.
 */
public final class AuthenticatedDiskCache {
    private final Path directory;
    private final ScopedKeyHierarchy hierarchy;
    private final ScopedKeyHierarchy.Domain domain;
    private final AeadEnvelope cipher = new AeadEnvelope();
    private final long quota;

    public AuthenticatedDiskCache(Path appPrivateDirectory, ScopedKeyHierarchy hierarchy,
            ScopedKeyHierarchy.Domain domain, long quotaBytes) throws IOException {
        if (appPrivateDirectory == null || hierarchy == null || domain == null || quotaBytes < AeadEnvelope.ENVELOPE_OVERHEAD_BYTES ||
                quotaBytes > 1024L * 1024 * 1024) throw new IllegalArgumentException("Invalid private cache configuration");
        directory = appPrivateDirectory.toAbsolutePath().normalize();
        if (Files.isSymbolicLink(directory)) throw new IOException("Cache directory cannot be a symbolic link");
        Files.createDirectories(directory, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Private cache directory unavailable");
        this.hierarchy = hierarchy;
        this.domain = domain;
        quota = quotaBytes;
        // An interrupted write contains ciphertext only and is never promoted on startup.
        try (var entries = Files.newDirectoryStream(directory, "tgcache-*.pending")) {
            for (Path path : entries) Files.delete(path);
        }
    }

    public synchronized void put(UUID id, long version, byte[] plaintext) throws IOException, GeneralSecurityException {
        if (plaintext == null || plaintext.length > AeadEnvelope.MAX_PLAINTEXT_BYTES) throw new IllegalArgumentException("Invalid cache payload");
        Path target = path(id, version, plaintext.length);
        byte[] key = hierarchy.derive(domain, ScopedKeyHierarchy.Purpose.CACHE, id, version);
        byte[] envelope;
        try { envelope = cipher.seal(key, plaintext, context(id, version, plaintext.length)); }
        finally { Arrays.fill(key, (byte) 0); }
        long retained = 0;
        try (var entries = Files.newDirectoryStream(directory, "*.cache")) {
            for (Path entry : entries) {
                if (!Files.isRegularFile(entry, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Invalid private cache entry");
                if (!entry.equals(target)) retained = Math.addExact(retained, Files.size(entry));
            }
        }
        if (retained > quota - envelope.length) throw new IOException("Encrypted cache quota exceeded");
        Path temporary = Files.createTempFile(directory, "tgcache-", ".pending",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        try {
            try (FileChannel output = FileChannel.open(temporary, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer bytes = ByteBuffer.wrap(envelope);
                while (bytes.hasRemaining()) output.write(bytes);
                output.force(true);
            }
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            syncDirectory();
        } finally { Files.deleteIfExists(temporary); }
    }

    public synchronized byte[] get(UUID id, long version, int expectedBytes) throws IOException, GeneralSecurityException {
        Path target = path(id, version, expectedBytes);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return null;
        // Never follow a malicious entry into shared storage. Bounds hold even if the entry changes.
        byte[] envelope = new byte[expectedBytes + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES];
        try (FileChannel input = FileChannel.open(target, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            if (input.size() != envelope.length) throw new IOException("Invalid encrypted cache length");
            ByteBuffer bytes = ByteBuffer.wrap(envelope);
            while (bytes.hasRemaining()) {
                if (input.read(bytes) < 0) throw new IOException("Truncated encrypted cache entry");
            }
            if (input.read(ByteBuffer.allocate(1)) != -1) throw new IOException("Trailing encrypted cache bytes");
        }
        byte[] key = hierarchy.derive(domain, ScopedKeyHierarchy.Purpose.CACHE, id, version);
        try { return cipher.open(key, envelope, context(id, version, expectedBytes)); }
        finally { Arrays.fill(key, (byte) 0); }
    }

    public synchronized void evict(UUID id) throws IOException {
        if (id == null) throw new IllegalArgumentException("Opaque cache ID required");
        Files.deleteIfExists(directory.resolve(id + ".cache"));
        syncDirectory();
    }

    public synchronized void clear() throws IOException {
        try (var entries = Files.newDirectoryStream(directory, "*.cache")) {
            for (Path entry : entries) Files.delete(entry);
        }
        syncDirectory();
    }

    private Path path(UUID id, long version, int bytes) {
        if (id == null || version <= 0 || bytes < 0 || bytes > AeadEnvelope.MAX_PLAINTEXT_BYTES) {
            throw new IllegalArgumentException("Invalid authenticated cache scope");
        }
        return directory.resolve(id + ".cache");
    }

    private byte[] context(UUID id, long version, int bytes) {
        return new ChunkContext(id, version, 0, bytes, ChunkContext.Scope.CACHE).encode();
    }

    private void syncDirectory() throws IOException {
        try (FileChannel parent = FileChannel.open(directory, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
            parent.force(true);
        }
    }
}
