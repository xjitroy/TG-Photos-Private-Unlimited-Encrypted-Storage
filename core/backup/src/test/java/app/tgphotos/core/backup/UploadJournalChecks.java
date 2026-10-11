package app.tgphotos.core.backup;

import app.tgphotos.core.crypto.AeadEnvelope;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Real local filesystem + controlled transport faults; no Telegram/device/power-loss acceptance. */
public final class UploadJournalChecks {
    private static int passed;
    private interface Action { void run() throws Exception; }
    private static void require(boolean value) { if (!value) throw new AssertionError("Journal check failed"); }
    private static void rejects(Action action) throws Exception {
        try { action.run(); } catch (IOException | GeneralSecurityException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid journal state accepted");
    }
    private static void check(String name, Action action) throws Exception { action.run(); passed++; System.out.println("PASS " + name); }
    private static final class Remote implements EncryptedBackupCoordinator.Transport {
        final Map<String, byte[]> objects = new LinkedHashMap<>();
        boolean loseAck = true;
        @Override public String uploadCiphertext(String identity, byte[] ciphertext) throws IOException {
            byte[] prior = objects.putIfAbsent(identity, ciphertext.clone());
            if (prior != null) require(Arrays.equals(prior, ciphertext));
            if (loseAck) { loseAck = false; throw new IOException("Injected uncertain remote acknowledgement"); }
            return identity;
        }
        @Override public byte[] downloadCiphertext(String pointer, int bound) throws IOException {
            byte[] bytes = objects.get(pointer);
            if (bytes == null || bytes.length > bound) throw new IOException("Missing remote object");
            return bytes.clone();
        }
    }
    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory("tgphotos-journal-checks-");
        AeadEnvelope aead = new AeadEnvelope();
        byte[] key = aead.generateKey();
        UUID id = UUID.randomUUID();
        byte[] content = "private bytes and secret filename.jpg".getBytes(StandardCharsets.UTF_8);
        byte[] hash = MessageDigest.getInstance("SHA-256").digest(content);
        EncryptedBackupCoordinator.Prepared prepared = new EncryptedBackupCoordinator.Prepared(hash, content.length, UUID.randomUUID(),
                aead.seal(aead.generateKey(), content, new byte[0]));
        try {
            check("Encrypted prepare survives reopen; no plaintext fingerprint or filename persisted", () -> {
                Path dir = root.resolve("persist");
                try (var journal = new EncryptedUploadJournal(dir, key, 4096)) { journal.prepare(id, 1, 0, prepared); }
                byte[] disk = Files.readAllBytes(dir.resolve(id + "-1-0.upload"));
                require(!new String(disk, StandardCharsets.ISO_8859_1).contains("filename.jpg"));
                require(!contains(disk, hash));
                require(Files.getPosixFilePermissions(dir).equals(PosixFilePermissions.fromString("rwx------")));
                require(Files.getPosixFilePermissions(dir.resolve(id + "-1-0.upload")).equals(PosixFilePermissions.fromString("rw-------")));
                try (var reopened = new EncryptedUploadJournal(dir, key, 4096)) {
                    var found = reopened.findPrepared(id, 1, 0);
                    require(found.operationId.equals(prepared.operationId));
                    require(Arrays.equals(found.ciphertext(), prepared.ciphertext()));
                    require(reopened.find(id, 1, 0) == null);
                    reopened.save(id, 1, 0, new EncryptedBackupCoordinator.Checkpoint(hash, content.length, "remote:1"));
                }
                try (var reopened = new EncryptedUploadJournal(dir, key, 4096)) { require(reopened.find(id, 1, 0).pointer.equals("remote:1")); }
            });
            check("Wrong journal key and ciphertext corruption fail closed", () -> {
                Path dir = root.resolve("persist");
                try (var journal = new EncryptedUploadJournal(dir, aead.generateKey(), 4096)) { rejects(() -> journal.findPrepared(id, 1, 0)); }
                Path path = dir.resolve(id + "-1-0.upload"); byte[] bytes = Files.readAllBytes(path); bytes[bytes.length - 1] ^= 1; Files.write(path, bytes);
                try (var journal = new EncryptedUploadJournal(dir, key, 4096)) { rejects(() -> journal.findPrepared(id, 1, 0)); }
            });
            check("Copying a valid record to another chunk identity fails authentication", () -> {
                Path dir = root.resolve("binding");
                try (var journal = new EncryptedUploadJournal(dir, key, 4096)) {
                    journal.prepare(id, 1, 0, prepared);
                    Files.copy(dir.resolve(id + "-1-0.upload"), dir.resolve(id + "-1-1.upload"));
                    rejects(() -> journal.findPrepared(id, 1, 1));
                }
            });
            check("Prepared identity and confirmed pointer cannot be replaced", () -> {
                try (var journal = new EncryptedUploadJournal(root.resolve("immutable"), key, 4096)) {
                    rejects(() -> journal.save(id, 1, 0, new EncryptedBackupCoordinator.Checkpoint(hash, content.length, "remote:1")));
                    journal.prepare(id, 1, 0, prepared);
                    rejects(() -> journal.prepare(id, 1, 0, new EncryptedBackupCoordinator.Prepared(hash, content.length, UUID.randomUUID(), prepared.ciphertext())));
                    journal.save(id, 1, 0, new EncryptedBackupCoordinator.Checkpoint(hash, content.length, "remote:1"));
                    rejects(() -> journal.save(id, 1, 0, new EncryptedBackupCoordinator.Checkpoint(hash, content.length, "remote:2")));
                }
            });
            check("Uncertain remote acknowledgement retries exact operation/ciphertext after reopen", () -> {
                Path dir = root.resolve("resume"); Remote remote = new Remote();
                byte[] fileKey = aead.generateKey(), manifestKey = aead.generateKey();
                try (var journal = new EncryptedUploadJournal(dir, key, 8192)) {
                    var engine = new EncryptedBackupCoordinator(aead, remote, journal, 16);
                    rejects(() -> engine.upload(new ByteArrayInputStream(content), id, 1, fileKey, manifestKey, new byte[0]));
                    require(remote.objects.size() == 1); require(journal.find(id, 1, 0) == null);
                }
                String operation = remote.objects.keySet().iterator().next(); byte[] original = remote.objects.get(operation).clone();
                try (var journal = new EncryptedUploadJournal(dir, key, 8192)) {
                    var engine = new EncryptedBackupCoordinator(aead, remote, journal, 16);
                    var commit = engine.upload(new ByteArrayInputStream(content), id, 1, fileKey, manifestKey, new byte[0]);
                    require(journal.findPrepared(id, 1, 0).operationId.toString().equals(operation));
                    require(Arrays.equals(remote.objects.get(operation), original));
                    require(remote.objects.size() == (content.length + 15) / 16 + 1);
                    require(engine.verifyRemote(commit, fileKey, manifestKey).restoredBytes == content.length);
                }
            });
            check("Changed source or key cannot retry a prepared chunk", () -> {
                Path dir = root.resolve("changed"); Remote remote = new Remote();
                byte[] fileKey = aead.generateKey(), manifestKey = aead.generateKey();
                try (var journal = new EncryptedUploadJournal(dir, key, 8192)) {
                    var engine = new EncryptedBackupCoordinator(aead, remote, journal, 16);
                    rejects(() -> engine.upload(new ByteArrayInputStream(content), id, 1, fileKey, manifestKey, new byte[0]));
                    byte[] changed = content.clone(); changed[0] ^= 1;
                    rejects(() -> engine.upload(new ByteArrayInputStream(changed), id, 1, fileKey, manifestKey, new byte[0]));
                    rejects(() -> engine.upload(new ByteArrayInputStream(content), id, 1, aead.generateKey(), manifestKey, new byte[0]));
                    rejects(() -> engine.upload(new ByteArrayInputStream(new byte[0]), id, 1, fileKey, manifestKey, new byte[0]));
                    require(remote.objects.size() == 1);
                }
            });
            check("Quota failure precedes remote upload; journal lock rejects reads", () -> {
                Path dir = root.resolve("quota"); Remote remote = new Remote();
                var journal = new EncryptedUploadJournal(dir, key, 1024);
                byte[] large = new byte[2048]; Arrays.fill(large, (byte) 7);
                var engine = new EncryptedBackupCoordinator(aead, remote, journal, 2048);
                rejects(() -> engine.upload(new ByteArrayInputStream(large), id, 1, aead.generateKey(), aead.generateKey(), new byte[0]));
                require(remote.objects.isEmpty()); journal.close(); rejects(() -> journal.findPrepared(id, 1, 0));
            });
            check("Symlink entry is refused without touching its target", () -> {
                Path dir = root.resolve("symlink"), outside = root.resolve("outside"); Files.writeString(outside, "owner-data");
                try (var journal = new EncryptedUploadJournal(dir, key, 4096)) {
                    Files.createSymbolicLink(dir.resolve(id + "-1-0.upload"), outside);
                    rejects(() -> journal.prepare(id, 1, 0, prepared));
                    require(Files.readString(outside).equals("owner-data"));
                }
            });
        } finally {
            Arrays.fill(key, (byte) 0);
            try (var entries = Files.walk(root)) {
                for (Path path : entries.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("RESULT " + passed + " journal checks passed. Linux filesystem/controlled transport only; Android/Telegram reconciliation pending.");
    }
    private static boolean contains(byte[] bytes, byte[] pattern) {
        for (int i = 0; i <= bytes.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) if (bytes[i + j] != pattern[j]) { match = false; break; }
            if (match) return true;
        }
        return false;
    }
}
