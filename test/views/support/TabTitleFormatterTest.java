package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.SearchQuery;

/**
 * Tests for {@code TabTitleFormatter}. Owner: Member B.
 *
 * <p>Cases B-47..B-51 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class TabTitleFormatterTest {

    /**
     * B-47: tab name = query (U3) [unit].
     *
     * <p>Arrange: query 'computer'.</p>
     * <p>Act: tabTitle.</p>
     * <p>Assert: 'computer'.</p>
     */
    @Test
    @DisplayName("B-47 tab name = query (U3)")
    void tabTitle_shortQuery_returnedUnchanged() {
        assertEquals("computer", TabTitleFormatter.tabTitle(SearchQuery.of("computer")));
    }

    /**
     * B-48: boundary 30 [unit].
     *
     * <p>Arrange: 30-char query.</p>
     * <p>Act: tabTitle.</p>
     * <p>Assert: unchanged.</p>
     */
    @Test
    @DisplayName("B-48 boundary 30")
    void tabTitle_thirtyCharacters_notTruncated() {
        String text = "a".repeat(30);
        assertEquals(text, TabTitleFormatter.tabTitle(SearchQuery.of(text)));
    }

    /**
     * B-49: boundary 31 [unit].
     *
     * <p>Arrange: 31-char query.</p>
     * <p>Act: tabTitle.</p>
     * <p>Assert: first 30 chars + '...' (33 chars).</p>
     */
    @Test
    @DisplayName("B-49 boundary 31")
    void tabTitle_thirtyOneCharacters_truncatedWithEllipsis() {
        String text = "a".repeat(31);
        assertEquals("a".repeat(30) + "...", TabTitleFormatter.tabTitle(SearchQuery.of(text)));
        assertEquals(33, TabTitleFormatter.tabTitle(SearchQuery.of(text)).length());
    }

    /**
     * B-50: single-line tab title [unit].
     *
     * <p>Arrange: query 'a\tb\nc'.</p>
     * <p>Act: tabTitle.</p>
     * <p>Assert: 'a b c'.</p>
     */
    @Test
    @DisplayName("B-50 single-line tab title")
    void tabTitle_queryWithTabOrLineBreak_shownAsSpaces() {
        assertEquals("a b c", TabTitleFormatter.tabTitle(SearchQuery.of("a\tb\nc")));
    }

    /**
     * B-51: tooltip shows full query (U3) [unit].
     *
     * <p>Arrange: 50-char query.</p>
     * <p>Act: tooltip.</p>
     * <p>Assert: tooltip equals query.text().</p>
     */
    @Test
    @DisplayName("B-51 tooltip shows full query (U3)")
    void tooltip_longQuery_returnsFullText() {
        SearchQuery query = SearchQuery.of("a".repeat(50));
        assertEquals(query.text(), TabTitleFormatter.tooltip(query));
        assertThrows(NullPointerException.class, () -> TabTitleFormatter.tooltip(null));
    }
}
