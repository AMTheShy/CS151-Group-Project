package views.support;

import java.util.OptionalInt;

/**
 * Cyclic "current match" cursor behind the reader's Previous / Next buttons and its
 * {@code k / n} label (U5, original UC4).
 *
 * <p><b>Owner:</b> Member B (placed in {@code views.support} because the reader uses it, but it is
 * pure logic over match indices and is owned by the search module - see developer guide,
 * "Workload balancing").</p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>With {@code n > 0} matches the cursor starts at index 0; {@link #positionLabel()} is
 *       {@code "1 / n"} (1-based for humans).</li>
 *   <li>{@link #next()} from the last index wraps to 0; {@link #previous()} from 0 wraps to
 *       {@code n - 1}. With one match both stay at 0.</li>
 *   <li>With {@code n == 0}: {@link #hasMatches()} is false, {@link #currentIndex()} is empty,
 *       {@link #positionLabel()} is {@code "0 / 0"}, and {@link #next()} / {@link #previous()}
 *       throw {@link IllegalStateException} (the view disables the buttons).</li>
 * </ul>
 *
 * <p><b>What it does NOT do:</b> no Swing, no scrolling, no knowledge of offsets.</p>
 *
 * <p><b>Threading:</b> Not thread-safe; confined to the EDT.</p>
 */
public final class MatchNavigator {

    /** Total number of matches, {@code >= 0}. */
    private final int matchCount;

    /** Current 0-based index; meaningless when {@code matchCount == 0}. */
    private int currentIndex;

    /**
     * Creates a navigator positioned at the first match.
     *
     * @param matchCount number of matches, {@code >= 0}
     * @throws IllegalArgumentException if {@code matchCount < 0}
     */
    public MatchNavigator(int matchCount) {
        if (matchCount < 0) {
            throw new IllegalArgumentException("matchCount must be >= 0");
        }
        this.matchCount = matchCount;
    }

    /**
     * Returns the number of matches.
     *
     * @return {@code n}
     */
    public int matchCount() {
        return matchCount;
    }

    /**
     * Tells whether there is at least one match.
     *
     * @return {@code n > 0}
     */
    public boolean hasMatches() {
        return matchCount > 0;
    }

    /**
     * Returns the current 0-based index.
     *
     * @return the index, or empty when there are no matches
     */
    public OptionalInt currentIndex() {
        return hasMatches() ? OptionalInt.of(currentIndex) : OptionalInt.empty();
    }

    /**
     * Moves to the next match, wrapping around (B-31).
     *
     * @return the new 0-based index
     * @throws IllegalStateException if there are no matches
     */
    public int next() {
        if (!hasMatches()) {
            throw new IllegalStateException("No matches to navigate");
        }
        currentIndex = currentIndex + 1 == matchCount ? 0 : currentIndex + 1;
        return currentIndex;
    }

    /**
     * Moves to the previous match, wrapping around (B-32).
     *
     * @return the new 0-based index
     * @throws IllegalStateException if there are no matches
     */
    public int previous() {
        if (!hasMatches()) {
            throw new IllegalStateException("No matches to navigate");
        }
        currentIndex = currentIndex == 0 ? matchCount - 1 : currentIndex - 1;
        return currentIndex;
    }

    /**
     * Jumps to a given match (B-35).
     *
     * @param index 0-based index, {@code 0 <= index < n}
     * @throws IndexOutOfBoundsException if out of range (always when {@code n == 0})
     */
    public void moveTo(int index) {
        if (index < 0 || index >= matchCount) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for " + matchCount);
        }
        currentIndex = index;
    }

    /**
     * Returns the label shown between the buttons.
     *
     * @return {@code "k / n"} with 1-based {@code k}, or {@code "0 / 0"} without matches
     */
    public String positionLabel() {
        return hasMatches() ? (currentIndex + 1) + " / " + matchCount : "0 / 0";
    }
}
