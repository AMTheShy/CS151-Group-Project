package views.support;

import java.util.ArrayList;
import java.util.List;

import models.AppConstants;
import models.MatchRange;

/**
 * Splits a document into consecutive character chunks so that the reader can insert a large text
 * into its Swing text model in several short EDT steps instead of one long one (U6, 3.4:
 * render 5 MB in &le; 1 s with no single EDT step &gt; 100 ms).
 *
 * <p><b>Owner:</b> Member B (pure algorithmic helper used by Member D's reader).</p>
 *
 * <p>Chunks are represented as {@link MatchRange} intervals {@code [start, end)} purely for reuse
 * of the half-open interval type. Rules: chunks are contiguous, ascending, cover exactly
 * {@code [0, textLength)}, each has length {@link #chunkSize()} except the last, which may be
 * shorter. A text of length 0 has no chunks.</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe.</p>
 */
public final class TextChunkPlanner {

    /** Characters per chunk, {@code >= 1}. */
    private final int chunkSize;

    /** Creates a planner with {@link AppConstants#TEXT_RENDER_CHUNK_CHARS}. */
    public TextChunkPlanner() {
        this(AppConstants.TEXT_RENDER_CHUNK_CHARS);
    }

    /**
     * Creates a planner with a custom chunk size.
     *
     * @param chunkSize characters per chunk, {@code >= 1}
     * @throws IllegalArgumentException if {@code chunkSize < 1} (B-43)
     */
    public TextChunkPlanner(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize must be >= 1");
        }
        this.chunkSize = chunkSize;
    }

    /**
     * Returns the chunk size.
     *
     * @return the chunk size
     */
    public int chunkSize() {
        return chunkSize;
    }

    /**
     * Plans the chunks of a text (B-41, B-42).
     *
     * @param textLength length of the text, {@code >= 0}
     * @return immutable list of chunk intervals; empty for length 0
     * @throws IllegalArgumentException if {@code textLength < 0}
     */
    public List<MatchRange> chunks(int textLength) {
        if (textLength < 0) {
            throw new IllegalArgumentException("textLength must be >= 0 but was " + textLength);
        }
        List<MatchRange> chunks = new ArrayList<>();
        for (int start = 0; start < textLength;) {
            int end = start + Math.min(chunkSize, textLength - start);
            chunks.add(new MatchRange(start, end));
            start = end;
        }
        return List.copyOf(chunks);
    }
}
