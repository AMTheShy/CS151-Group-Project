package views.support;

import models.AppConstants;
import models.SearchQuery;

/**
 * Formats the title and tooltip of a search tab (U3).
 *
 * <p><b>Owner:</b> Member B (text formatting helper, like snippets; used by Member D).</p>
 *
 * <p><b>Rules:</b> the tab title is the query text if it has at most
 * {@link AppConstants#TAB_TITLE_MAX_LENGTH} (30) characters; otherwise the first 30 characters
 * followed by {@link AppConstants#ELLIPSIS} (33 characters in total). Line breaks and tabs inside
 * the query are shown as single spaces (one-to-one) so the tab stays one line. The tooltip is
 * always the full query text, unmodified.</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe (static methods).</p>
 */
public final class TabTitleFormatter {

    private TabTitleFormatter() {
        throw new AssertionError("TabTitleFormatter is a utility class and must not be instantiated");
    }

    /**
     * Returns the tab title for a query (B-47..B-50).
     *
     * @param query the query, not {@code null}
     * @return the possibly truncated title
     * @throws NullPointerException if {@code query} is {@code null}
     */
    public static String tabTitle(SearchQuery query) {
        throw new UnsupportedOperationException("TODO: Member B");
    }

    /**
     * Returns the tab tooltip (B-51).
     *
     * @param query the query, not {@code null}
     * @return {@code query.text()}
     * @throws NullPointerException if {@code query} is {@code null}
     */
    public static String tooltip(SearchQuery query) {
        throw new UnsupportedOperationException("TODO: Member B");
    }
}
