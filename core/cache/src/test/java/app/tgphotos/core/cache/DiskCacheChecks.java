package app.tgphotos.core.cache;

import app.tgphotos.core.crypto.AeadEnvelope;
import app.tgphotos.core.crypto.ScopedKeyHierarchy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;

/** Filesystem fixture checks; not Android device/filesystem evidence or an independent audit. */
public final class DiskCacheChecks {
    private static int passed;
    private interface Action { void run() throws Exception; }
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Check failed"); }
    private static void check(String label, Action action) throws Exception {
        action.run(); passed++; System.out.println("PASS " + label);
    }
    private static void rejects(Action action) throws Exception {
        try { action.run(); }
        catch (IOException | GeneralSecurityException | IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Invalid cache read/write was accepted");
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("tgphotos-cache-checks-");
        AeadEnvelope cipher = new AeadEnvelope();
        byte[] cloud = cipher.generateKey(), vault = cipher.generateKey(), salt = cipher.generateKey();
        byte[] payload = "ব্যক্তিগত thumbnail fixture".getBytes(StandardCharsets.UTF_8);
        UUID id = UUID.randomUUID();
        try (ScopedKeyHierarchy hierarchy = new ScopedKeyHierarchy(cloud, vault, salt)) {
            Path directory = root.resolve("cloud");
            AuthenticatedDiskCache cache = new AuthenticatedDiskCache(directory, hierarchy, ScopedKeyHierarchy.Domain.CLOUD, 1024);
            Path entry = directory.resolve(id + ".cache");
            check("Disk cache round trip survives a fresh service instance", () -> {
                cache.put(id, 1, payload);
                AuthenticatedDiskCache fresh = new AuthenticatedDiskCache(directory, hierarchy, ScopedKeyHierarchy.Domain.CLOUD, 1024);
                require(Arrays.equals(payload, fresh.get(id, 1, payload.length)));
                require(fresh.get(UUID.randomUUID(), 1, 0) == null);
            });
            check("Persistent entry holds only authenticated ciphertext with private permissions", () -> {
                byte[] disk = Files.readAllBytes(entry);
                require(disk.length == payload.length + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES);
                require(!new String(disk, StandardCharsets.UTF_8).contains(new String(payload, StandardCharsets.UTF_8)));
                require(Files.getPosixFilePermissions(entry).equals(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                try (var paths = Files.list(directory)) { require(paths.count() == 1); }
            });
            check("Changed manifest version and length reject cached replay", () -> {
                rejects(() -> cache.get(id, 2, payload.length));
                rejects(() -> cache.get(id, 1, payload.length + 1));
                rejects(() -> cache.get(id, 1, -1));
            });
            check("Every on-disk envelope byte rejects corruption before plaintext release", () -> {
                byte[] original = Files.readAllBytes(entry);
                for (int i = 0; i < original.length; i++) {
                    byte[] damaged = original.clone(); damaged[i] ^= 1; Files.write(entry, damaged);
                    rejects(() -> cache.get(id, 1, payload.length));
                }
                Files.write(entry, original);
            });
            check("Opaque ID substitution and cloud-vault substitution fail", () -> {
                UUID other = UUID.randomUUID(); Path substituted = directory.resolve(other + ".cache");
                Files.copy(entry, substituted);
                rejects(() -> cache.get(other, 1, payload.length)); Files.delete(substituted);
                AuthenticatedDiskCache privateCache = new AuthenticatedDiskCache(directory, hierarchy, ScopedKeyHierarchy.Domain.VAULT, 1024);
                rejects(() -> privateCache.get(id, 1, payload.length));
            });
            check("Locked vault cannot decrypt its persistent cache; cloud continues", () -> {
                AuthenticatedDiskCache privateCache = new AuthenticatedDiskCache(root.resolve("vault"), hierarchy, ScopedKeyHierarchy.Domain.VAULT, 1024);
                privateCache.put(id, 1, payload);
                hierarchy.lockVault(); rejects(() -> privateCache.get(id, 1, payload.length));
                require(Arrays.equals(payload, cache.get(id, 1, payload.length)));
                hierarchy.unlockVault(vault);
                require(Arrays.equals(payload, privateCache.get(id, 1, payload.length)));
            });
            check("Symbolic links cannot expose or overwrite an external file", () -> {
                Path external = root.resolve("external"); Files.write(external, payload);
                UUID linked = UUID.randomUUID(); Path link = directory.resolve(linked + ".cache");
                Files.createSymbolicLink(link, external);
                rejects(() -> cache.get(linked, 1, payload.length));
                rejects(() -> cache.put(linked, 1, payload));
                require(Arrays.equals(payload, Files.readAllBytes(external)));
                cache.evict(linked); require(Files.exists(external));
                Path directoryLink = root.resolve("linked-directory"); Files.createSymbolicLink(directoryLink, directory);
                rejects(() -> new AuthenticatedDiskCache(directoryLink, hierarchy, ScopedKeyHierarchy.Domain.CLOUD, 1024));
            });
            check("Quota rejection preserves the previously committed cache entry", () -> {
                AuthenticatedDiskCache tiny = new AuthenticatedDiskCache(directory, hierarchy, ScopedKeyHierarchy.Domain.CLOUD,
                        payload.length + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES);
                rejects(() -> tiny.put(UUID.randomUUID(), 1, payload));
                rejects(() -> tiny.put(id, 2, new byte[payload.length + 1]));
                require(Arrays.equals(payload, tiny.get(id, 1, payload.length)));
                tiny.put(id, 2, payload); require(Arrays.equals(payload, tiny.get(id, 2, payload.length)));
            });
            check("Interrupted ciphertext temporary files are discarded on restart", () -> {
                Path interrupted = Files.createTempFile(directory, "tgcache-", ".pending"); Files.write(interrupted, new byte[] {1, 2, 3});
                new AuthenticatedDiskCache(directory, hierarchy, ScopedKeyHierarchy.Domain.CLOUD, 1024);
                require(!Files.exists(interrupted)); require(Arrays.equals(payload, cache.get(id, 2, payload.length)));
            });
            check("Eviction and clearing remove entries without modifying other private files", () -> {
                cache.evict(id); require(cache.get(id, 2, payload.length) == null);
                Path unrelated = directory.resolve("keep.txt"); Files.write(unrelated, new byte[] {7});
                cache.put(id, 3, payload); cache.clear();
                require(cache.get(id, 3, payload.length) == null); require(Files.exists(unrelated));
            });
        } finally {
            Arrays.fill(cloud, (byte) 0); Arrays.fill(vault, (byte) 0); Arrays.fill(salt, (byte) 0);
            try (var paths = Files.walk(root)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
        System.out.println("RESULT " + passed + " checks passed. Linux/JCA fixtures; Android/provider/lifecycle QA pending.");
    }
}
