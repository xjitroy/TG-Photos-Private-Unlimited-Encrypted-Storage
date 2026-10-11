package app.tgphotos.core.media;

import app.tgphotos.core.backup.AuthenticatedManifest;
import app.tgphotos.core.backup.EncryptedBackupCoordinator;
import app.tgphotos.core.backup.EncryptedUploadJournal;
import app.tgphotos.core.crypto.AeadEnvelope;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Executes upload journal -> remote manifest -> media reader, without Android/account/user media. */
public final class EncryptedMediaPipelineChecks {
    private static int passed;
    private interface Action { void run() throws Exception; }
    private static void check(String name, Action action) throws Exception { action.run(); passed++; System.out.println("PASS " + name); }
    private static void require(boolean condition) { if (!condition) throw new AssertionError("Encrypted media pipeline check failed"); }
    private static void rejects(Action action) throws Exception {
        try { action.run(); } catch (IOException | GeneralSecurityException | IllegalStateException | IllegalArgumentException expected) { return; }
        throw new AssertionError("Invalid media pipeline state accepted");
    }
    private static final class Remote implements EncryptedBackupCoordinator.Transport {
        final Map<String, byte[]> objects = new LinkedHashMap<>();
        int downloads;
        CountDownLatch entered, release;
        @Override public String uploadCiphertext(String name, byte[] ciphertext) { objects.put(name, ciphertext.clone()); return name; }
        @Override public byte[] downloadCiphertext(String pointer, int bound) throws IOException {
            downloads++;
            if (entered != null) {
                entered.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IOException("Fixture timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("Fixture interrupted"); }
            }
            byte[] bytes = objects.get(pointer);
            if (bytes == null || bytes.length > bound) throw new IOException("Invalid bounded remote object");
            return bytes.clone();
        }
    }
    private static final class Fixture implements AutoCloseable {
        final AeadEnvelope cipher = new AeadEnvelope();
        final Remote remote = new Remote();
        final byte[] fileKey = cipher.generateKey(), manifestKey = cipher.generateKey();
        final byte[] content = new byte[53], metadata = new byte[] {13, 9, 20, 3};
        final Path directory = Files.createTempDirectory("tgphotos-media-pipeline-");
        final EncryptedUploadJournal journal = new EncryptedUploadJournal(directory, cipher.generateKey(), 16384);
        final EncryptedBackupCoordinator coordinator = new EncryptedBackupCoordinator(cipher, remote, journal, 16);
        final EncryptedBackupCoordinator.Commit commit;
        Fixture() throws Exception {
            for (int i = 0; i < content.length; i++) content[i] = (byte) (i + 11);
            commit = coordinator.upload(new ByteArrayInputStream(content), UUID.randomUUID(), 1, fileKey, manifestKey, metadata);
        }
        EncryptedBackupMediaSession session() { return new EncryptedBackupMediaSession(coordinator, remote, commit, fileKey, manifestKey); }
        @Override public void close() throws IOException {
            journal.close(); Arrays.fill(fileKey, (byte) 0); Arrays.fill(manifestKey, (byte) 0);
            try (var paths = Files.walk(directory)) { for (Path p : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
    public static void main(String[] args) throws Exception {
        check("Actual journal backup opens authenticated metadata and reconstructs media", () -> {
            try (Fixture f = new Fixture(); var session = f.session(); var reader = session.openReader()) {
                byte[] output = new byte[f.content.length]; int position = 0;
                while (position < output.length) position += reader.readAt(position, output, position, output.length - position);
                require(Arrays.equals(f.content, output));
                try (AuthenticatedManifest manifest = f.coordinator.openManifest(f.commit, f.manifestKey)) {
                    require(Arrays.equals(f.metadata, manifest.metadata())); require(manifest.totalBytes == output.length);
                }
            }
        });
        check("Seek fetches only target chunk; partial playback is not restore evidence", () -> {
            try (Fixture f = new Fixture(); var session = f.session(); var reader = session.openReader()) {
                byte[] output = new byte[3]; require(reader.readAt(33, output, 0, 3) == 3);
                require(Arrays.equals(output, Arrays.copyOfRange(f.content, 33, 36)));
                require(f.remote.downloads == 2);
                require(f.coordinator.verifyRemote(f.commit, f.fileKey, f.manifestKey).restoredBytes == f.content.length);
                require(f.remote.downloads == 7);
            }
        });
        check("Manifest tamper fails before a chunk or reader is published", () -> {
            try (Fixture f = new Fixture(); var session = f.session()) {
                byte[] bytes = f.remote.objects.get(f.commit.manifestPointer); bytes[bytes.length - 1] ^= 1;
                rejects(session::openReader); require(f.remote.downloads == 1);
            }
        });
        check("Wrong manifest key cannot publish a reader", () -> {
            try (Fixture f = new Fixture(); var session = new EncryptedBackupMediaSession(f.coordinator, f.remote, f.commit, f.fileKey, f.cipher.generateKey())) {
                rejects(session::openReader); require(f.remote.downloads == 1);
            }
        });
        check("Wrong file key and corrupted chunks never modify destination", () -> {
            try (Fixture f = new Fixture(); var session = new EncryptedBackupMediaSession(f.coordinator, f.remote, f.commit, f.cipher.generateKey(), f.manifestKey);
                    var reader = session.openReader()) {
                byte[] destination = new byte[] {8, 8, 8}; rejects(() -> reader.readAt(0, destination, 0, 3));
                require(Arrays.equals(destination, new byte[] {8, 8, 8}));
            }
            try (Fixture f = new Fixture(); var session = f.session(); var reader = session.openReader()) {
                String pointer = f.remote.objects.keySet().iterator().next(); f.remote.objects.get(pointer)[25] ^= 1;
                byte[] destination = new byte[] {6}; rejects(() -> reader.readAt(0, destination, 0, 1)); require(destination[0] == 6);
            }
        });
        check("Lock immediately closes all readers and refuses new or cached reads", () -> {
            try (Fixture f = new Fixture(); var session = f.session()) {
                var first = session.openReader(); var second = session.openReader();
                require(first.readAt(0, new byte[1], 0, 1) == 1); session.close();
                require(first.isClosed() && second.isClosed()); rejects(session::openReader);
                rejects(() -> first.readAt(0, new byte[1], 0, 1));
            }
        });
        check("Lock during blocked manifest download prevents late reader publication", () -> {
            try (Fixture f = new Fixture(); var session = f.session()) {
                f.remote.entered = new CountDownLatch(1); f.remote.release = new CountDownLatch(1);
                AtomicBoolean published = new AtomicBoolean(false), rejected = new AtomicBoolean(false);
                Thread pending = new Thread(() -> {
                    try (var reader = session.openReader()) { published.set(true); }
                    catch (IOException expected) { rejected.set(true); }
                    catch (Exception failure) { throw new AssertionError(failure); }
                });
                pending.start();
                try {
                    require(f.remote.entered.await(2, TimeUnit.SECONDS)); session.close();
                } finally { f.remote.release.countDown(); pending.join(5000); }
                require(!pending.isAlive() && !published.get() && rejected.get());
            }
        });
        check("Reader count is bounded and closed readers release admission", () -> {
            try (Fixture f = new Fixture(); var session = f.session()) {
                AuthenticatedRangeReader[] readers = new AuthenticatedRangeReader[8];
                for (int i = 0; i < readers.length; i++) readers[i] = session.openReader();
                rejects(session::openReader); require(f.remote.downloads == 8);
                readers[0].close(); try (var replacement = session.openReader()) { require(replacement.totalBytes == f.content.length); }
            }
        });
        check("Authenticated descriptors return snapshots and reject use after close", () -> {
            try (Fixture f = new Fixture(); AuthenticatedManifest manifest = f.coordinator.openManifest(f.commit, f.manifestKey)) {
                int[] lengths = manifest.chunkLengths(); String[] pointers = manifest.chunkPointers(); byte[] metadata = manifest.metadata();
                lengths[0] = 999; pointers[0] = "wrong"; metadata[0] ^= 1;
                require(manifest.chunkLengths()[0] == 16 && !manifest.chunkPointers()[0].equals("wrong"));
                require(Arrays.equals(manifest.metadata(), f.metadata)); manifest.close(); rejects(manifest::metadata); rejects(manifest::chunkLengths);
            }
        });
        System.out.println("RESULT " + passed + " integrated encrypted media checks passed. Android decoders/player/lifecycle and Telegram still pending.");
    }
}
