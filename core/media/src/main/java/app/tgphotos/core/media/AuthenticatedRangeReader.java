package app.tgphotos.core.media;

import app.tgphotos.core.crypto.AeadEnvelope;
import app.tgphotos.core.crypto.ChunkContext;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.UUID;

/** Draft seekable, one-chunk plaintext buffer. Manifest/key must already be authenticated/unlocked. */
public final class AuthenticatedRangeReader implements AutoCloseable {
    public interface CiphertextSource {
        /** Blocking playback-loader operation. Transport must enforce bounds, deadlines and interruption. */
        byte[] fetch(int chunkIndex, int maximumResponseBytes) throws IOException;
    }
    public final UUID fileId;
    public final long version;
    public final long totalBytes;
    private final int[] lengths;
    private final long[] offsets;
    private final byte[] key;
    private final CiphertextSource source;
    private final AeadEnvelope cipher = new AeadEnvelope();
    private byte[] buffer;
    private int bufferedIndex = -1;
    private boolean closed;

    public AuthenticatedRangeReader(UUID id, long version, int[] authenticatedChunkLengths,
            byte[] fileKey, CiphertextSource source) {
        if (id == null || version <= 0 || authenticatedChunkLengths == null ||
                authenticatedChunkLengths.length > 100000 || fileKey == null || fileKey.length != 32 || source == null) {
            throw new IllegalArgumentException("Invalid authenticated media descriptor");
        }
        lengths = authenticatedChunkLengths.clone();
        offsets = new long[lengths.length + 1];
        for (int i = 0; i < lengths.length; i++) {
            if (lengths[i] < 1 || lengths[i] > AeadEnvelope.MAX_PLAINTEXT_BYTES) throw new IllegalArgumentException("Invalid media chunk bound");
            offsets[i + 1] = Math.addExact(offsets[i], lengths[i]);
        }
        fileId = id; this.version = version; totalBytes = offsets[lengths.length];
        key = fileKey.clone(); this.source = source;
    }

    /** Returns at most the remainder of one fully authenticated chunk, 0 for zero length, -1 at EOF. */
    public synchronized int readAt(long position, byte[] destination, int offset, int count)
            throws IOException, GeneralSecurityException {
        if (closed) throw new IllegalStateException("Authenticated media source is closed");
        if (destination == null || offset < 0 || count < 0 || offset > destination.length - count ||
                position < 0 || position > totalBytes) throw new IllegalArgumentException("Invalid media read range");
        if (count == 0) return 0;
        if (position == totalBytes) return -1;
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Media read cancelled");
        int index = Arrays.binarySearch(offsets, position);
        if (index < 0) index = -index - 2;
        if (index != bufferedIndex) {
            wipeBuffer();
            int maximum = lengths[index] + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES;
            byte[] envelope = source.fetch(index, maximum);
            if (envelope == null || envelope.length != maximum) throw new IOException("Invalid encrypted media chunk length");
            byte[] authenticated = cipher.open(key, envelope,
                    new ChunkContext(fileId, version, index, lengths[index], ChunkContext.Scope.FILE).encode());
            if (Thread.currentThread().isInterrupted()) {
                Arrays.fill(authenticated, (byte) 0); throw new InterruptedIOException("Media read cancelled");
            }
            buffer = authenticated; bufferedIndex = index;
        }
        int within = (int) (position - offsets[index]);
        int available = Math.min(count, buffer.length - within);
        System.arraycopy(buffer, within, destination, offset, available);
        return available;
    }

    private void wipeBuffer() {
        if (buffer != null) Arrays.fill(buffer, (byte) 0);
        buffer = null; bufferedIndex = -1;
    }

    /** Caller must close all readers on vault lock/account lock/background policy before releasing previews. */
    @Override public synchronized void close() {
        wipeBuffer(); Arrays.fill(key, (byte) 0); closed = true;
    }
    @Override public String toString() { return "AuthenticatedRangeReader(redacted)"; }
}
