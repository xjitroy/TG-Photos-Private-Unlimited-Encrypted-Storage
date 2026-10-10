package app.tgphotos.core.media;

import app.tgphotos.core.crypto.AeadEnvelope;
import app.tgphotos.core.crypto.ChunkContext;
import java.security.GeneralSecurityException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Arrays;
import java.util.UUID;

/** Seek/authentication memory fixtures only; no decoder, Media3 runtime or device playback. */
public final class AuthenticatedRangeChecks {
    private static int passed;
    private interface Action { void run() throws Exception; }
    private static void require(boolean value) { if (!value) throw new AssertionError("Check failed"); }
    private static void check(String label, Action action) throws Exception {
        action.run(); passed++; System.out.println("PASS " + label);
    }
    private static void rejects(Action action) throws Exception {
        try { action.run(); }
        catch (GeneralSecurityException | IOException | IllegalArgumentException | IllegalStateException expected) { return; }
        throw new AssertionError("Invalid encrypted range was accepted");
    }
    public static void main(String[] args) throws Exception {
        AeadEnvelope cipher = new AeadEnvelope(); byte[] key = cipher.generateKey(); UUID id = UUID.randomUUID();
        byte[][] plain = {{0, 1, 2, 3}, {4, 5, 6, 7}, {8, 9}};
        byte[][] envelopes = new byte[plain.length][];
        for (int i = 0; i < plain.length; i++) envelopes[i] = cipher.seal(key, plain[i],
                new ChunkContext(id, 1, i, plain[i].length, ChunkContext.Scope.FILE).encode());
        int[] fetches = {0};
        AuthenticatedRangeReader.CiphertextSource source = (index, maximum) -> {
            fetches[0]++; require(maximum == envelopes[index].length); return envelopes[index].clone();
        };
        check("Seeked reads authenticate the entire target chunk and preserve adjacent output", () -> {
            try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4, 4, 2}, key, source)) {
                byte[] out = new byte[6]; Arrays.fill(out, (byte) 99);
                require(reader.readAt(5, out, 1, 4) == 3);
                require(Arrays.equals(out, new byte[] {99, 5, 6, 7, 99, 99}));
                require(reader.readAt(8, out, 1, 4) == 2);
                require(out[1] == 8 && out[2] == 9);
            }
        });
        check("Same-chunk reads reuse one buffer; seeking backward reauthenticates replaced chunk", () -> {
            fetches[0] = 0;
            try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4, 4, 2}, key, source)) {
                byte[] out = new byte[4]; reader.readAt(1, out, 0, 1); reader.readAt(2, out, 0, 1);
                require(fetches[0] == 1); reader.readAt(4, out, 0, 1); reader.readAt(0, out, 0, 1);
                require(fetches[0] == 3 && out[0] == 0);
            }
        });
        check("Sequential cross-chunk reads reconstruct the complete authenticated file", () -> {
            try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4, 4, 2}, key, source)) {
                byte[] out = new byte[10]; int position = 0;
                while (position < out.length) position += reader.readAt(position, out, position, out.length - position);
                require(Arrays.equals(out, new byte[] {0,1,2,3,4,5,6,7,8,9}));
                require(reader.readAt(10, out, 0, 1) == -1); require(reader.readAt(10, out, 0, 0) == 0);
            }
        });
        check("Wrong key, ID, version, reordered and damaged chunk never modify destination", () -> {
            for (int attack = 0; attack < 5; attack++) {
                final int mode = attack;
                byte[] candidateKey = mode == 0 ? cipher.generateKey() : key;
                UUID candidateId = mode == 1 ? UUID.randomUUID() : id;
                long candidateVersion = mode == 2 ? 2 : 1;
                try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(candidateId, candidateVersion,
                        new int[] {4,4,2}, candidateKey, (index, maximum) -> {
                            byte[] bytes = envelopes[mode == 3 ? 1 : index].clone();
                            if (mode == 4) bytes[bytes.length - 1] ^= 1;
                            return bytes;
                        })) {
                    byte[] out = new byte[] {99,99,99,99}; rejects(() -> reader.readAt(0, out, 0, 4));
                    require(Arrays.equals(out, new byte[] {99,99,99,99}));
                }
            }
        });
        check("Oversized/truncated remote envelopes are rejected before decoding", () -> {
            for (int size : new int[] {0, envelopes[0].length - 1, envelopes[0].length + 1}) {
                try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4}, key,
                        (index, maximum) -> Arrays.copyOf(envelopes[0], size))) {
                    rejects(() -> reader.readAt(0, new byte[4], 0, 4));
                }
            }
        });
        check("Read ranges and authenticated descriptor bounds reject invalid requests", () -> {
            rejects(() -> new AuthenticatedRangeReader(id, 1, new int[] {0}, key, source));
            rejects(() -> new AuthenticatedRangeReader(id, 1, new int[] {AeadEnvelope.MAX_PLAINTEXT_BYTES + 1}, key, source));
            try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4}, key, source)) {
                rejects(() -> reader.readAt(-1, new byte[4], 0, 1));
                rejects(() -> reader.readAt(5, new byte[4], 0, 1));
                rejects(() -> reader.readAt(0, new byte[4], Integer.MAX_VALUE, 1));
            }
        });
        check("Closed readers reject cached bytes and cancellation stops before fetch", () -> {
            AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4}, key, source);
            reader.readAt(0, new byte[1], 0, 1); reader.close(); reader.close();
            rejects(() -> reader.readAt(0, new byte[1], 0, 1));
            try (AuthenticatedRangeReader cancelled = new AuthenticatedRangeReader(id, 1, new int[] {4}, key, source)) {
                int before = fetches[0]; Thread.currentThread().interrupt();
                try { cancelled.readAt(0, new byte[1], 0, 1); throw new AssertionError("Cancelled read succeeded"); }
                catch (InterruptedIOException expected) { require(fetches[0] == before); }
                finally { Thread.interrupted(); }
            }
        });
        check("Empty files terminate without fetching phantom chunks", () -> {
            try (AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[0], key,
                    (index, maximum) -> { throw new AssertionError("Empty file fetched a chunk"); })) {
                require(reader.totalBytes == 0); require(reader.readAt(0, new byte[1], 0, 1) == -1);
            }
        });
        check("Vault/account closure does not wait for a fetch and prevents late plaintext publication", () -> {
            var entered = new java.util.concurrent.CountDownLatch(1);
            var release = new java.util.concurrent.CountDownLatch(1);
            byte[] destination = new byte[] {99,99,99,99};
            AuthenticatedRangeReader reader = new AuthenticatedRangeReader(id, 1, new int[] {4}, key, (index, maximum) -> {
                entered.countDown();
                try { if (!release.await(2, java.util.concurrent.TimeUnit.SECONDS)) throw new IOException("Fixture fetch stalled"); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new InterruptedIOException("Fixture cancelled"); }
                return envelopes[0].clone();
            });
            var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
            try {
                var pending = executor.submit(() -> { rejects(() -> reader.readAt(0, destination, 0, 4)); return true; });
                require(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                var close = executor.submit(() -> { reader.close(); return true; });
                require(close.get(500, java.util.concurrent.TimeUnit.MILLISECONDS));
                release.countDown(); require(pending.get(2, java.util.concurrent.TimeUnit.SECONDS));
                require(Arrays.equals(destination, new byte[] {99,99,99,99}));
            } finally { release.countDown(); reader.close(); executor.shutdownNow(); }
        });
        Arrays.fill(key, (byte) 0);
        System.out.println("RESULT " + passed + " checks passed. JCA range fixtures; real decoder/device streaming pending.");
    }
}
