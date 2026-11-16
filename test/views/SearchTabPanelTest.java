package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.SearchQuery;
import testsupport.FakeDocumentLibraryController;

/**
 * Tests for {@code SearchTabPanel}. Owner: Member D.
 *
 * <p>Cases D-35..D-39 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchTabPanelTest {

    private SearchTabPanel tab;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        DownloadWorkflow download = new DownloadWorkflow(
                new FakeDocumentLibraryController(),
                new ScriptedDialogService());
        tab = new SearchTabPanel(SearchQuery.of("computer"), new DocumentReaderPanel(download));
    }

    /**
     * D-35: immediate feedback (U3) [unit].
     *
     * <p>Arrange: new tab.</p>
     * <p>Act: constructor.</p>
     * <p>Assert: status 'Searching...'; progress bar indeterminate and visible; combo disabled.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-35 immediate feedback (U3)")
    void constructor_initialState_showsSearching() {
        fail("TODO: Member D - D-35");
    }

    /**
     * D-36: results listed (U4) [unit].
     *
     * <p>Arrange: 2 results.</p>
     * <p>Act: onSearchCompleted.</p>
     * <p>Assert: combo size 2 in order; status '2 matching documents'; progress hidden.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-36 results listed (U4)")
    void onSearchCompleted_results_fillComboAndCount() {
        fail("TODO: Member D - D-36");
    }

    /**
     * D-37: zero results (U4) [unit].
     *
     * <p>Arrange: empty list.</p>
     * <p>Act: onSearchCompleted.</p>
     * <p>Assert: status 'No matches'; combo disabled.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-37 zero results (U4)")
    void onSearchCompleted_empty_showsNoMatches() {
        fail("TODO: Member D - D-37");
    }

    /**
     * D-38: search failure [unit].
     *
     * <p>Arrange: onSearchFailed('boom').</p>
     * <p>Act: onSearchFailed.</p>
     * <p>Assert: status 'Search failed: boom'.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-38 search failure")
    void onSearchFailed_message_shownInStatus() {
        fail("TODO: Member D - D-38");
    }

    /**
     * D-39: reading a document (U5) [unit].
     *
     * <p>Arrange: results; select element 0; flush EDT steps.</p>
     * <p>Act: selection.</p>
     * <p>Assert: reader text equals the document's display text.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-39 reading a document (U5)")
    void selectResult_comboSelection_showsDocumentInReader() {
        fail("TODO: Member D - D-39");
    }
}
