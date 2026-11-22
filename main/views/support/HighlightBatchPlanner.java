package views.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import models.AppConstants;
import models.MatchRange;

/**
 * Plans how the reader applies match highlights without blocking the EDT (U6, decision N8).
 *
 * <p><b>Owner:</b> Member B (pure algorithmic helper used by Member D's reader).</p>
 *
 * <p><b>Two modes, chosen by {@link #shouldHighlightEagerly(int)}:</b></p>
 * <ul>
 *   <li><b>Eager</b> ({@code matchCount <= AppConstants.MAX_EAGER_HIGHLIGHTS}): the reader adds
 *       highlights for all matches, {@link #planBatches} batches at a time, one batch per
 *       {@code SwingUtilities.invokeLater} step.</li>
 *   <li><b>Windowed</b> (more matches): the reader highlights only the matches intersecting the
 *       visible character range plus a margin; it gets them with {@link #matchesIntersecting},
 *       which must be O(log n + k) using binary search, because it runs on every scroll.</li>
 * </ul>
 *
 * <p>All returned lists are views ({@code subList}) of the input or new immutable lists; the input
 * is never modified. The input list must be sorted ascending and non-overlapping (as produced by
 * the search engine); this is a precondition, not re-checked.</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe.</p>
 */
public final class HighlightBatchPlanner {

    /** Number of highlights per batch, {@code >= 1}. */
    private final int batchSize;

    /** Creates a planner with {@link AppConstants#HIGHLIGHT_BATCH_SIZE}. */
    public HighlightBatchPlanner() {
        this(AppConstants.HIGHLIGHT_BATCH_SIZE);
    }

    /**
     * Creates a planner with a custom batch size.
     *
     * @param batchSize highlights per batch, {@code >= 1}
     * @throws IllegalArgumentException if {@code batchSize < 1} (B-38)
     */
    public HighlightBatchPlanner(int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be >= 1");
        }
        this.batchSize = batchSize;
    }

    /**
     * Returns the batch size.
     *
     * @return the batch size
     */
    public int batchSize() {
        return batchSize;
    }

    /**
     * Tells whether all matches should be highlighted (eager mode).
     *
     * @param matchCount number of matches
     * @return {@code matchCount <= AppConstants.MAX_EAGER_HIGHLIGHTS}
     */
    public boolean shouldHighlightEagerly(int matchCount) {
        return matchCount <= AppConstants.MAX_EAGER_HIGHLIGHTS;
    }

    /**
     * Splits the matches into consecutive batches of {@link #batchSize()} (the last may be
     * smaller), preserving order; the concatenation of the batches equals the input (B-36).
     *
     * @param matches sorted matches, not {@code null}
     * @return immutable list of batches; empty for empty input (B-37)
     * @throws NullPointerException if {@code matches} is {@code null}
     */
    public List<List<MatchRange>> planBatches(List<MatchRange> matches) {
        Objects.requireNonNull(matches, "matches");
        List<List<MatchRange>> batches = new ArrayList<>();
        for (int start = 0; start < matches.size();) {
            int end = start + Math.min(batchSize, matches.size() - start);
            batches.add(matches.subList(start, end));
            start = end;
        }
        return List.copyOf(batches);
    }

    /**
     * Returns the index of the first match whose {@code end > offset}, by binary search.
     *
     * @param matches sorted matches, not {@code null}
     * @param offset  a character offset, {@code >= 0}
     * @return an index in {@code 0..matches.size()} ({@code size()} if none)
     */
    public int indexOfFirstEndingAfter(List<MatchRange> matches, int offset) {
        Objects.requireNonNull(matches, "matches");
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be >= 0 but was " + offset);
        }
        int low = 0;
        int high = matches.size();
        while (low < high) {
            int middle = low + (high - low) / 2;
            if (matches.get(middle).end() <= offset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /**
     * Returns the contiguous sub-list of matches that intersect {@code [fromOffset, toOffset)}
     * (B-39, B-40).
     *
     * @param matches    sorted matches, not {@code null}
     * @param fromOffset inclusive start of the window, {@code >= 0}
     * @param toOffset   exclusive end of the window, {@code >= fromOffset}
     * @return a {@code subList} view, possibly empty
     * @throws IllegalArgumentException if the window is invalid
     */
    public List<MatchRange> matchesIntersecting(List<MatchRange> matches, int fromOffset, int toOffset) {
        Objects.requireNonNull(matches, "matches");
        if (fromOffset < 0 || toOffset < fromOffset) {
            throw new IllegalArgumentException("invalid window: fromOffset " + fromOffset
                    + ", toOffset " + toOffset);
        }
        if (fromOffset == toOffset) {
            return matches.subList(0, 0);
        }
        int first = indexOfFirstEndingAfter(matches, fromOffset);
        int low = first;
        int high = matches.size();
        while (low < high) {
            int middle = low + (high - low) / 2;
            if (matches.get(middle).start() < toOffset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return matches.subList(first, low);
    }
}
