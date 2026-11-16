package models.search;

import models.MatchRange;
import models.Snippet;

/**
 * Produces the short display texts of the UI: the match snippet (S4) and the library preview (D7).
 *
 * <p><b>Line breaks:</b> in both outputs every {@code '\r'} and every {@code '\n'} is replaced by
 * exactly one space. The replacement is one-to-one, so offsets inside the extracted text are not
 * shifted ({@code "\r\n"} becomes two spaces).</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member B
 * ({@code models.search.DefaultSnippetExtractor}). Used by Member B's search engine and by
 * Member C's library controller (previews).</p>
 *
 * <p>Threading: Implementations must be stateless; safe for concurrent use.</p>
 */
public interface SnippetExtractor {

    /**
     * Extracts the snippet around a match (S4).
     *
     * <p>The snippet consists of: up to {@code context} characters before {@code match}, the
     * matched characters, and up to {@code context} characters after it, cut at the text
     * boundaries. If characters before the context window exist, the snippet starts with
     * {@code AppConstants.ELLIPSIS} and {@code truncatedStart} is true; symmetrically for the end.
     * The snippet's highlight is the position of the matched characters in the snippet text.</p>
     *
     * <p>Example ({@code context = 3}): text {@code "abcdefgXYZhijklmn"}, match [7,10) gives
     * {@code "...efgXYZhij..."} with highlight [6,9).</p>
     *
     * @param text    the full document text, not {@code null}
     * @param match   a range inside the text ({@code match.end() <= text.length()}), not {@code null}
     * @param context maximum number of characters on each side, {@code >= 0}
     * @return the snippet
     * @throws NullPointerException     if {@code text} or {@code match} is {@code null}
     * @throws IllegalArgumentException if {@code context < 0} or the match lies outside the text
     */
    Snippet extract(String text, MatchRange match, int context);

    /**
     * Returns the preview of a document for the library table (D7, UC3).
     *
     * @param text     the full text, not {@code null}
     * @param maxChars maximum preview length, {@code >= 0}
     * @return the first {@code min(maxChars, text.length())} characters, line breaks replaced by
     *         spaces; no ellipsis is added
     * @throws NullPointerException     if {@code text} is {@code null}
     * @throws IllegalArgumentException if {@code maxChars < 0}
     */
    String preview(String text, int maxChars);
}
