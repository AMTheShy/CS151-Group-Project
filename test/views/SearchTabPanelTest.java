package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.awaitRender;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;
import static views.SwingTestSupport.result;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.DocumentSearchResult;
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

    private DocumentReaderPanel reader;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        DownloadWorkflow download = new DownloadWorkflow(
                new FakeDocumentLibraryController(),
                new ScriptedDialogService());
        reader = onEdtGet(() -> new DocumentReaderPanel(download));
        tab = onEdtGet(() -> new SearchTabPanel(SearchQuery.of("computer"), reader));
    }

    /**
     * D-35: immediate feedback (U3) [unit].
     *
     * <p>Arrange: new tab.</p>
     * <p>Act: constructor.</p>
     * <p>Assert: status 'Searching...'; progress bar indeterminate and visible; combo disabled.</p>
     */
    @Test
    @DisplayName("D-35 immediate feedback (U3)")
    void constructor_initialState_showsSearching() {
        // Act + Assert
        assertEquals("Searching...", onEdtGet(tab::statusText));
        assertTrue(onEdtGet(() -> tab.progressBar().isIndeterminate()));
        assertTrue(onEdtGet(() -> tab.progressBar().isVisible()));
        assertFalse(onEdtGet(() -> tab.resultComboBox().isEnabled()));
    }

    /**
     * D-36: results listed (U4) [unit].
     *
     * <p>Arrange: 2 results.</p>
     * <p>Act: onSearchCompleted.</p>
     * <p>Assert: combo size 2 in order; status '2 matching documents'; progress hidden.</p>
     */
    @Test
    @DisplayName("D-36 results listed (U4)")
    void onSearchCompleted_results_fillComboAndCount() {
        // Arrange
        DocumentSearchResult first = result("A", "a computer", "computer");
        DocumentSearchResult second = result("b", "computer b", "computer");

        // Act
        onEdt(() -> tab.onSearchCompleted(List.of(first, second)));

        // Assert
        assertEquals(List.of(first, second), onEdtGet(() -> tab.comboModel().results()));
        assertEquals("2 matching documents", onEdtGet(tab::statusText));
        assertFalse(onEdtGet(() -> tab.progressBar().isVisible()));
        assertTrue(onEdtGet(() -> tab.resultComboBox().isEnabled()));
    }

    /**
     * D-37: zero results (U4) [unit].
     *
     * <p>Arrange: empty list.</p>
     * <p>Act: onSearchCompleted.</p>
     * <p>Assert: status 'No matches'; combo disabled.</p>
     */
    @Test
    @DisplayName("D-37 zero results (U4)")
    void onSearchCompleted_empty_showsNoMatches() {
        // Act
        onEdt(() -> tab.onSearchCompleted(List.of()));

        // Assert
        assertEquals("No matches", onEdtGet(tab::statusText));
        assertFalse(onEdtGet(() -> tab.resultComboBox().isEnabled()));
    }

    /**
     * D-38: search failure [unit].
     *
     * <p>Arrange: onSearchFailed('boom').</p>
     * <p>Act: onSearchFailed.</p>
     * <p>Assert: status 'Search failed: boom'.</p>
     */
    @Test
    @DisplayName("D-38 search failure")
    void onSearchFailed_message_shownInStatus() {
        // Act
        onEdt(() -> tab.onSearchFailed("boom"));

        // Assert
        assertEquals("Search failed: boom", onEdtGet(tab::statusText));
    }

    /**
     * D-39: reading a document (U5) [unit].
     *
     * <p>Arrange: results; select element 0; flush EDT steps.</p>
     * <p>Act: selection.</p>
     * <p>Assert: reader text equals the document's display text.</p>
     */
    @Test
    @DisplayName("D-39 reading a document (U5)")
    void selectResult_comboSelection_showsDocumentInReader() {
        // Arrange
        String content = "first line\r\nhas computer\rand more computer";
        DocumentSearchResult first = result("A", content, "computer");
        onEdt(() -> tab.onSearchCompleted(List.of(first, result("B", "computer", "computer"))));

        // Act
        onEdt(() -> tab.resultComboBox().setSelectedIndex(0));
        awaitRender(reader);

        // Assert
        assertEquals(DocumentReaderPanel.toDisplayText(content), onEdtGet(() -> reader.textArea().getText()));
    }
}
