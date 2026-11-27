package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;
import static views.SwingTestSupport.result;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.SearchQuery;
import testsupport.FakeDocumentLibraryController;
import testsupport.FakeSearchController;
import testsupport.FakeSearchController.StartedSearch;
import testsupport.TestDocuments;
import views.support.TabTitleFormatter;

/**
 * Tests for {@code SearchResultsPane}. Owner: Member D.
 *
 * <p>Cases D-40..D-42 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchResultsPaneTest {

    private FakeSearchController searchController;

    /** A document long enough to need several chunk steps (D-50). */
    private static final int LARGE_DOCUMENT_LENGTH = 2 * 1024 * 1024;

    private SearchResultsPane pane;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        searchController = new FakeSearchController();
        pane = onEdtGet(() -> new SearchResultsPane(
                searchController,
                new FakeDocumentLibraryController(),
                new ScriptedDialogService()));
    }

    /**
     * D-40: one tab per search (U3) [unit].
     *
     * <p>Arrange: openSearch(SearchQuery.of('computer algorithm')).</p>
     * <p>Act: openSearch.</p>
     * <p>Assert: tabCount 1; tab title via TabTitleFormatter; searchController.startedSearches size 1 with
     * the tab as listener.</p>
     */
    @Test
    @DisplayName("D-40 one tab per search (U3)")
    void openSearch_query_addsSelectedTabAndStartsSearch() {
        // Arrange
        SearchQuery query = SearchQuery.of("computer algorithm");

        // Act
        SearchTabPanel tab = onEdtGet(() -> pane.openSearch(query));

        // Assert
        assertEquals(1, onEdtGet(pane::tabCount));
        assertSame(tab, onEdtGet(() -> pane.tabbedPane().getSelectedComponent()));
        ClosableTabHeader header = assertInstanceOf(ClosableTabHeader.class,
                onEdtGet(() -> pane.tabbedPane().getTabComponentAt(0)));
        assertEquals(TabTitleFormatter.tabTitle(query), onEdtGet(() -> header.titleLabel().getText()));
        assertEquals(1, searchController.startedSearches().size());
        assertEquals(query, searchController.lastSearch().query());
        assertSame(tab, searchController.lastSearch().listener());
    }

    /**
     * D-41: closing a tab cancels its search (U3) [unit].
     *
     * <p>Arrange: openSearch; closeTab(tab).</p>
     * <p>Act: closeTab.</p>
     * <p>Assert: handle.cancelCalls 1; tabCount 0; later complete(...) not delivered.</p>
     */
    @Test
    @DisplayName("D-41 closing a tab cancels its search (U3)")
    void closeTab_runningSearch_cancelsHandleAndRemovesTab() {
        // Arrange
        SearchTabPanel tab = onEdtGet(() -> pane.openSearch(SearchQuery.of("computer")));
        StartedSearch search = searchController.lastSearch();

        // Act
        onEdt(() -> pane.closeTab(tab));

        // Assert
        assertEquals(1, search.handle().cancelCalls());
        assertEquals(0, onEdtGet(pane::tabCount));
        assertFalse(onEdtGet(() -> searchController.complete(search, List.of())));
    }

    /**
     * D-42: next query immediately (U3) [unit].
     *
     * <p>Arrange: openSearch twice before completing.</p>
     * <p>Act: openSearch.</p>
     * <p>Assert: tabCount 2; completing the first fills only the first tab.</p>
     */
    @Test
    @DisplayName("D-42 next query immediately (U3)")
    void openSearch_twice_twoIndependentTabs() {
        // Arrange
        SearchTabPanel firstTab = onEdtGet(() -> pane.openSearch(SearchQuery.of("computer")));
        SearchTabPanel secondTab = onEdtGet(() -> pane.openSearch(SearchQuery.of("algorithm")));
        StartedSearch firstSearch = searchController.startedSearches().get(0);

        // Act
        onEdt(() -> searchController.complete(firstSearch, List.of(result("A", "computer", "computer"))));

        // Assert
        assertEquals(2, onEdtGet(pane::tabCount));
        assertEquals("1 matching document", onEdtGet(firstTab::statusText));
        assertEquals("Searching...", onEdtGet(secondTab::statusText));
    }

    /**
     * D-50: closing a tab whose reader is still inserting a large document stops the remaining EDT steps.
     */
    @Test
    @DisplayName("D-50 closing a tab stops its reader's render (U3, U6)")
    void closeTab_renderInProgress_stopsReaderRendering() {
        // Arrange
        SearchTabPanel tab = onEdtGet(() -> pane.openSearch(SearchQuery.of("needle")));
        String content = "needle " + TestDocuments.asciiText(LARGE_DOCUMENT_LENGTH, 3);
        onEdt(() -> searchController.complete(searchController.lastSearch(),
                List.of(result("Big", content, "needle"))));
        DocumentReaderPanel reader = tab.readerPanel();

        // Act
        onEdt(() -> {
            tab.resultComboBox().setSelectedIndex(0);
            pane.closeTab(tab);
        });
        onEdt(() -> {
            // runs after any chunk step that was already queued
        });

        // Assert
        assertTrue(onEdtGet(reader::isRenderComplete));
        assertEquals(0, onEdtGet(() -> reader.textArea().getDocument().getLength()));
    }
}
