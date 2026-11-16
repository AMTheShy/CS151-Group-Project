package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.SearchQuery;
import testsupport.FakeDocumentLibraryController;
import testsupport.FakeSearchController;

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

    private SearchResultsPane pane;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        searchController = new FakeSearchController();
        pane = new SearchResultsPane(
                searchController,
                new FakeDocumentLibraryController(),
                new ScriptedDialogService());
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
    @Disabled("TODO: Member D")
    @DisplayName("D-40 one tab per search (U3)")
    void openSearch_query_addsSelectedTabAndStartsSearch() {
        fail("TODO: Member D - D-40");
    }

    /**
     * D-41: closing a tab cancels its search (U3) [unit].
     *
     * <p>Arrange: openSearch; closeTab(tab).</p>
     * <p>Act: closeTab.</p>
     * <p>Assert: handle.cancelCalls 1; tabCount 0; later complete(...) not delivered.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-41 closing a tab cancels its search (U3)")
    void closeTab_runningSearch_cancelsHandleAndRemovesTab() {
        fail("TODO: Member D - D-41");
    }

    /**
     * D-42: next query immediately (U3) [unit].
     *
     * <p>Arrange: openSearch twice before completing.</p>
     * <p>Act: openSearch.</p>
     * <p>Assert: tabCount 2; completing the first fills only the first tab.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-42 next query immediately (U3)")
    void openSearch_twice_twoIndependentTabs() {
        fail("TODO: Member D - D-42");
    }
}
