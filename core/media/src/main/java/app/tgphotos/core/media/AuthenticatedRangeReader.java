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
    private final Object reads = new Object();
    private final Object state = new Object();
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
    public int readAt(long position, byte[] destination, int offset, int count)
            throws IOException, GeneralSecurityException {
        synchronized (reads) {
            int index;
            byte[] operationKey;
            synchronized (state) {
                requireOpen();
                if (destination == null || offset < 0 || count < 0 || offset > destination.length - count ||
                        position < 0 || position > totalBytes) throw new IllegalArgumentException("Invalid media read range");
                if (count == 0) return 0;
                if (position == totalBytes) return -1;
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Media read cancelled");
                index = Arrays.binarySearch(offsets, position);
                if (index < 0) index = -index - 2;
                if (index == bufferedIndex) return copy(position, index, destination, offset, count);
                wipeBuffer();
                operationKey = key.clone();
            }
            byte[] authenticated = null;
            try {
                int maximum = lengths[index] + AeadEnvelope.ENVELOPE_OVERHEAD_BYTES;
                byte[] envelope = source.fetch(index, maximum);
                synchronized (state) { requireOpen(); }
                if (envelope == null || envelope.length != maximum) throw new IOException("Invalid encrypted media chunk length");
                authenticated = cipher.open(operationKey, envelope,
                        new ChunkContext(fileId, version, index, lengths[index], ChunkContext.Scope.FILE).encode());
                synchronized (state) {
                    requireOpen();
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Media read cancelled");
                    buffer = authenticated; authenticated = null; bufferedIndex = index;
                    return copy(position, index, destination, offset, count);
                }
            } finally {
                Arrays.fill(operationKey, (byte) 0);
                if (authenticated != null) Arrays.fill(authenticated, (byte) 0);
            }
        }
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Authenticated media source is closed");
    }

    private int copy(long position, int index, byte[] destination, int offset, int count) {
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
    @Override public void close() {
        // Never wait for a blocked remote fetch. An in-flight operation cannot publish bytes after closure.
        synchronized (state) { wipeBuffer(); Arrays.fill(key, (byte) 0); closed = true; }
    }
    @Override public String toString() { return "AuthenticatedRangeReader(redacted)"; }
}
