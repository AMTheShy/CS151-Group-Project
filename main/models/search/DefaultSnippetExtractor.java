package models.search;

import java.util.Objects;

import models.AppConstants;
import models.MatchRange;
import models.Snippet;

/**
 * Standard {@link SnippetExtractor} (S4, D7).
 *
 * <p><b>Owner:</b> Member B (Search Engine).</p>
 *
 * <p><b>extract - exact rules:</b></p>
 * <ul>
 *   <li>{@code from = max(0, match.start() - context)}, {@code to = min(text.length(), match.end() + context)}.</li>
 *   <li>{@code truncatedStart = from > 0}, {@code truncatedEnd = to < text.length()}.</li>
 *   <li>{@code body = replaceLineBreaks(text.substring(from, to))}.</li>
 *   <li>{@code display = (truncatedStart ? "..." : "") + body + (truncatedEnd ? "..." : "")}.</li>
 *   <li>{@code highlightStart = (truncatedStart ? 3 : 0) + (match.start() - from)};
 *       highlight length = {@code match.length()}.</li>
 *   <li>Boundary: exactly {@code context} characters before the match &rarr; no leading ellipsis
 *       (B-23); {@code context + 1} &rarr; leading ellipsis (B-24).</li>
 * </ul>
 * <p>Worked example ({@code context = 40}, resource {@code snippet-boundaries.txt} = 100 x,
 * {@code NEEDLE}, 100 y; match [100,106)): {@code "..." + 40 x + "NEEDLE" + 40 y + "..."},
 * highlight [43,49).</p>
 *
 * <p><b>preview - exact rules:</b> {@code replaceLineBreaks(text.substring(0, min(maxChars, text.length())))};
 * no ellipsis; {@code maxChars = 0} gives {@code ""}.</p>
 *
 * <p><b>Line breaks:</b> each {@code '\r'} and each {@code '\n'} becomes one {@code ' '}
 * (one-to-one; offsets do not shift). Other characters, including tabs, are kept.</p>
 *
 * <p><b>Performance:</b> O(context) / O(maxChars) - never copy the whole document (B-45).</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe (stateless).</p>
 */
public final class DefaultSnippetExtractor implements SnippetExtractor {

    /** Creates the extractor. */
    public DefaultSnippetExtractor() {
        // stateless
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests: B-19..B-27.</p>
     */
    @Override
    public Snippet extract(String text, MatchRange match, int context) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(match, "match");
        if (context < 0) {
            throw new IllegalArgumentException("context must be >= 0 but was " + context);
        }
        if (match.end() > text.length()) {
            throw new IllegalArgumentException("match end " + match.end()
                    + " exceeds text length " + text.length());
        }
        int from = Math.max(0, match.start() - context);
        int to = match.end() + Math.min(context, text.length() - match.end());
        boolean truncatedStart = from > 0;
        boolean truncatedEnd = to < text.length();
        String body = replaceLineBreaks(text.substring(from, to));
        String display = (truncatedStart ? AppConstants.ELLIPSIS : "")
                + body + (truncatedEnd ? AppConstants.ELLIPSIS : "");
        int highlightStart = (truncatedStart ? AppConstants.ELLIPSIS.length() : 0)
                + (match.start() - from);
        MatchRange highlight = new MatchRange(highlightStart, highlightStart + match.length());
        return new Snippet(display, highlight, truncatedStart, truncatedEnd);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Tests: B-28, B-29.</p>
     */
    @Override
    public String preview(String text, int maxChars) {
        Objects.requireNonNull(text, "text");
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars must be >= 0 but was " + maxChars);
        }
        return replaceLineBreaks(text.substring(0, Math.min(maxChars, text.length())));
    }

    /**
     * Replaces every {@code '\r'} and {@code '\n'} with {@code ' '}, one-to-one.
     *
     * @param text the text
     * @return the text without line breaks, same length
     */
    static String replaceLineBreaks(String text) {
        Objects.requireNonNull(text, "text");
        return text.replace('\r', ' ').replace('\n', ' ');
    }
}
