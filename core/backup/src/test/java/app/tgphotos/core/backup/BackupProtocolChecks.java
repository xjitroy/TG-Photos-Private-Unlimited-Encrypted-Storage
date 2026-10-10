package app.tgphotos.core.backup;

import app.tgphotos.core.crypto.AeadEnvelope;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Fault-injection transport fixtures only; never app wiring or claims about Telegram. */
public final class BackupProtocolChecks {
    private static int passed;
    private interface Action { void run() throws Exception; }
    private static void check(String name, Action action) throws Exception { action.run(); passed++; System.out.println("PASS " + name); }
    private static void require(boolean value) { if (!value) throw new AssertionError("Check failed"); }
    private static void rejects(Action action) throws Exception {
        try { action.run(); } catch (IOException | GeneralSecurityException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid backup accepted");
    }
    private static final class Remote implements EncryptedBackupCoordinator.Transport {
        final Map<String, byte[]> objects = new LinkedHashMap<>();
        int uploads; int downloads; int failAfter = Integer.MAX_VALUE;
        @Override public String uploadCiphertext(String name, byte[] ciphertext) throws IOException {
            require(name.matches("[a-f0-9-]{36}"));
            if (uploads >= failAfter) throw new IOException("Injected network interruption");
            String pointer = "remote:" + uploads++;
            objects.put(pointer, ciphertext.clone()); return pointer;
        }
        @Override public byte[] downloadCiphertext(String pointer, int maxBytes) throws IOException {
            downloads++;
            byte[] data = objects.get(pointer);
            if (data == null || data.length > maxBytes) throw new IOException("Missing or oversized remote object");
            return data.clone();
        }
    }
    private static final class MemoryCheckpoints implements EncryptedBackupCoordinator.Checkpoints {
        final Map<String, EncryptedBackupCoordinator.Checkpoint> checkpoints = new LinkedHashMap<>();
        String id(UUID id, long version, int index) { return id + ":" + version + ":" + index; }
        @Override public EncryptedBackupCoordinator.Checkpoint find(UUID id, long version, int index) { return checkpoints.get(id(id, version, index)); }
        @Override public void save(UUID id, long version, int index, EncryptedBackupCoordinator.Checkpoint record) { checkpoints.put(id(id, version, index), record); }
    }
    private static final class Fixture {
        final AeadEnvelope aead = new AeadEnvelope();
        final Remote remote = new Remote();
        final MemoryCheckpoints checkpoints = new MemoryCheckpoints();
        final UUID id = UUID.randomUUID();
        final byte[] fileKey = aead.generateKey();
        final byte[] manifestKey = aead.generateKey();
        final byte[] content = "private photo bytes with Unicode বাংলা and binary tail".getBytes(StandardCharsets.UTF_8);
        final byte[] metadata = "ব্যক্তিগত নাম/private-secret-file.jpg".getBytes(StandardCharsets.UTF_8);
        EncryptedBackupCoordinator engine() { return new EncryptedBackupCoordinator(aead, remote, checkpoints, 16); }
        EncryptedBackupCoordinator.Commit upload() throws Exception { return engine().upload(new ByteArrayInputStream(content), id, 1, fileKey, manifestKey, metadata); }
    }
    public static void main(String[] args) throws Exception {
        check("Manifest uploaded last after complete chunks", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.upload();
            require(c.manifestPointer.equals("remote:" + (f.remote.uploads - 1)));
            require(f.checkpoints.checkpoints.size() == (f.content.length + 15) / 16);
        });
        check("Complete independent remote authentication and hash", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.upload();
            EncryptedBackupCoordinator.Verification v = f.engine().verifyRemote(c, f.fileKey, f.manifestKey);
            require(v.restoredBytes == f.content.length);
            require(MessageDigest.isEqual(v.sha256(), MessageDigest.getInstance("SHA-256").digest(f.content)));
            require(f.remote.downloads == f.checkpoints.checkpoints.size() + 1);
        });
        check("Content and sensitive filename not sent as plaintext", () -> {
            Fixture f = new Fixture(); f.upload();
            for (byte[] bytes : f.remote.objects.values()) {
                String text = new String(bytes, StandardCharsets.ISO_8859_1);
                require(!text.contains("private-secret-file.jpg"));
                require(!text.contains("private photo bytes"));
            }
        });
        check("Interrupted upload cannot commit manifest", () -> {
            Fixture f = new Fixture(); f.remote.failAfter = 1; rejects(f::upload);
            require(f.remote.objects.size() == 1); require(f.checkpoints.checkpoints.size() == 1);
        });
        check("Resume uses confirmed saved chunk without duplicate upload", () -> {
            Fixture f = new Fixture(); f.remote.failAfter = 1; rejects(f::upload);
            byte[] first = f.remote.objects.get("remote:0").clone();
            f.remote.failAfter = Integer.MAX_VALUE; EncryptedBackupCoordinator.Commit c = f.upload();
            require(Arrays.equals(first, f.remote.objects.get("remote:0")));
            require(f.remote.uploads == f.checkpoints.checkpoints.size() + 1);
            require(f.engine().verifyRemote(c, f.fileKey, f.manifestKey).restoredBytes == f.content.length);
        });
        check("Modified source cannot reuse same version checkpoint", () -> {
            Fixture f = new Fixture(); f.remote.failAfter = 1; rejects(f::upload);
            f.remote.failAfter = Integer.MAX_VALUE; f.content[0] ^= 1; rejects(f::upload);
            require(f.remote.objects.size() == 1);
        });
        check("Truncated source cannot reuse trailing saved checkpoints", () -> {
            Fixture f = new Fixture(); f.remote.failAfter = 2; rejects(f::upload); f.remote.failAfter = Integer.MAX_VALUE;
            rejects(() -> f.engine().upload(new ByteArrayInputStream(Arrays.copyOf(f.content, 16)), f.id, 1, f.fileKey, f.manifestKey, f.metadata));
            require(f.remote.objects.size() == 2);
        });
        check("Missing saved remote chunk blocks resumed manifest", () -> {
            Fixture f = new Fixture(); f.remote.failAfter = 1; rejects(f::upload); f.remote.objects.clear(); f.remote.failAfter = Integer.MAX_VALUE;
            rejects(f::upload); require(f.remote.objects.isEmpty());
        });
        check("Corrupt ciphertext fails independent remote verification", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.upload();
            byte[] first = f.remote.objects.get("remote:0"); first[first.length - 1] ^= 1;
            rejects(() -> f.engine().verifyRemote(c, f.fileKey, f.manifestKey));
        });
        check("Corrupt manifest fails before downloading file chunks", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.upload();
            byte[] m = f.remote.objects.get(c.manifestPointer); m[m.length - 1] ^= 1;
            rejects(() -> f.engine().verifyRemote(c, f.fileKey, f.manifestKey)); require(f.remote.downloads == 1);
        });
        check("Wrong file/manifest keys cannot verify", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.upload();
            rejects(() -> f.engine().verifyRemote(c, f.aead.generateKey(), f.manifestKey));
            rejects(() -> f.engine().verifyRemote(c, f.fileKey, f.aead.generateKey()));
        });
        check("File and manifest key separation required", () -> {
            Fixture f = new Fixture(); rejects(() -> f.engine().upload(new ByteArrayInputStream(f.content), f.id, 1, f.fileKey, f.fileKey, f.metadata));
            require(f.remote.uploads == 0);
        });
        check("Empty file commits and remotely verifies without phantom chunk", () -> {
            Fixture f = new Fixture(); EncryptedBackupCoordinator.Commit c = f.engine().upload(new ByteArrayInputStream(new byte[0]), f.id, 1, f.fileKey, f.manifestKey, f.metadata);
            require(f.remote.uploads == 1); require(f.checkpoints.checkpoints.isEmpty());
            require(f.engine().verifyRemote(c, f.fileKey, f.manifestKey).restoredBytes == 0);
        });
        System.out.println("RESULT " + passed + " checks passed. In-memory fixtures only; TDLib/durable checkpoints/device restore pending.");
    }
}
