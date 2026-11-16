package models;

import java.util.Objects;

/**
 * The display text around the first match of a document (S4, UC2 second column).
 *
 * <p>{@code text} is ready to display: it already contains the leading and/or trailing
 * {@link AppConstants#ELLIPSIS} when the context was cut, and every {@code '\r'} and
 * {@code '\n'} has been replaced by one space (one-to-one, so offsets are not shifted).
 * {@code highlight} is the position of the matched characters <em>inside {@code text}</em>
 * (including the offset caused by a leading ellipsis), so a renderer can bold or color it.</p>
 *
 * <p>Invariants:</p>
 * <ul>
 *   <li>{@code text} and {@code highlight} are not {@code null}; {@code highlight.end() <= text.length()}.</li>
 *   <li>{@code text} contains neither {@code '\r'} nor {@code '\n'}.</li>
 *   <li>If {@code truncatedStart}, {@code text} starts with the ellipsis and the highlight starts
 *       after it.</li>
 *   <li>If {@code truncatedEnd}, {@code text} ends with the ellipsis and the highlight ends
 *       before it.</li>
 * </ul>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). Created by {@code models.search.SnippetExtractor}.</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param text           display text, without line breaks
 * @param highlight      position of the match inside {@code text}
 * @param truncatedStart whether text before the context was cut off (leading ellipsis present)
 * @param truncatedEnd   whether text after the context was cut off (trailing ellipsis present)
 */
public record Snippet(String text, MatchRange highlight, boolean truncatedStart, boolean truncatedEnd) {

    /**
     * Validates the invariants listed in the class description.
     *
     * @throws NullPointerException     if {@code text} or {@code highlight} is {@code null}
     * @throws IllegalArgumentException if any other invariant is violated
     */
    public Snippet {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(highlight, "highlight");
        if (highlight.end() > text.length()) {
            throw new IllegalArgumentException("highlight " + highlight + " exceeds text length " + text.length());
        }
        if (text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("snippet text must not contain line breaks");
        }
        int ellipsisLength = AppConstants.ELLIPSIS.length();
        if (truncatedStart && (!text.startsWith(AppConstants.ELLIPSIS) || highlight.start() < ellipsisLength)) {
            throw new IllegalArgumentException("truncatedStart requires a leading ellipsis before the highlight");
        }
        if (truncatedEnd && (!text.endsWith(AppConstants.ELLIPSIS)
                || highlight.end() > text.length() - ellipsisLength)) {
            throw new IllegalArgumentException("truncatedEnd requires a trailing ellipsis after the highlight");
        }
    }

    /**
     * Returns the matched characters as they appear in the snippet.
     *
     * @return {@code text.substring(highlight.start(), highlight.end())}
     */
    public String highlightedText() {
        return text.substring(highlight.start(), highlight.end());
    }
}
