package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.FakeDocumentLibraryController;
import views.support.ImportProgressTracker;

/**
 * Tests for {@code DocumentLibraryPanel}. Owner: Member D.
 *
 * <p>Cases D-28..D-31 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 *
 * <p>Note: Run Swing interactions inside SwingUtilities.invokeAndWait.</p>
 */
class DocumentLibraryPanelTest {

    private FakeDocumentLibraryController controller;

    private ScriptedDialogService dialogs;

    private DocumentLibraryPanel panel;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        controller = new FakeDocumentLibraryController();
        dialogs = new ScriptedDialogService();
        panel = new DocumentLibraryPanel(controller, dialogs, new ImportProgressPanel(new ImportProgressTracker()));
    }

    /**
     * D-28: Delete disabled without selection (D7) [unit].
     *
     * <p>Arrange: onDocumentsChanged(2 rows); tick row 0.</p>
     * <p>Act: deleteButton.</p>
     * <p>Assert: initially disabled; enabled after tick; disabled after untick.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-28 Delete disabled without selection (D7)")
    void deleteButton_noSelection_disabledThenEnabledAfterTick() {
        fail("TODO: Member D - D-28");
    }

    /**
     * D-29: delete confirmation (D7) [unit].
     *
     * <p>Arrange: tick A, B; confirmDelete false, then true.</p>
     * <p>Act: onDeleteClicked.</p>
     * <p>Assert: no request when declined; deleteDocuments([A, B]) when confirmed; ticks cleared.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-29 delete confirmation (D7)")
    void onDeleteClicked_confirmedOrDeclined_deletesOnlyWhenConfirmed() {
        fail("TODO: Member D - D-29");
    }

    /**
     * D-30: table refresh [unit].
     *
     * <p>Arrange: fire 3 summaries then 1.</p>
     * <p>Act: onDocumentsChanged.</p>
     * <p>Assert: table rowCount 3 then 1.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-30 table refresh")
    void onDocumentsChanged_newList_refreshesTable() {
        fail("TODO: Member D - D-30");
    }

    /**
     * D-31: startup load progress on the left (D5) [unit].
     *
     * <p>Arrange: progress (2, 5); completed(LoadSummary(5,0,[])); completed with 1 problem.</p>
     * <p>Act: onStartupLoadProgress / Completed.</p>
     * <p>Assert: 'Loading saved documents... 2 / 5'; '5 documents'; '(1 could not be loaded)' suffix.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-31 startup load progress on the left (D5)")
    void startupLoadCallbacks_updateStatusLabel() {
        fail("TODO: Member D - D-31");
    }
}
