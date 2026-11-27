package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;
import static views.SwingTestSupport.summaries;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.LoadSummary;
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
        panel = onEdtGet(() -> new DocumentLibraryPanel(controller, dialogs,
                new ImportProgressPanel(new ImportProgressTracker())));
    }

    /**
     * D-28: Delete disabled without selection (D7) [unit].
     *
     * <p>Arrange: onDocumentsChanged(2 rows); tick row 0.</p>
     * <p>Act: deleteButton.</p>
     * <p>Assert: initially disabled; enabled after tick; disabled after untick.</p>
     */
    @Test
    @DisplayName("D-28 Delete disabled without selection (D7)")
    void deleteButton_noSelection_disabledThenEnabledAfterTick() {
        // Arrange
        onEdt(() -> panel.onDocumentsChanged(summaries("A", "B")));
        boolean initiallyEnabled = onEdtGet(() -> panel.deleteButton().isEnabled());

        // Act
        boolean enabledAfterTick = onEdtGet(() -> {
            panel.tableModel().setValueAt(true, 0, DocumentTableModel.DELETE_COLUMN);
            return panel.deleteButton().isEnabled();
        });
        boolean enabledAfterUntick = onEdtGet(() -> {
            panel.tableModel().setValueAt(false, 0, DocumentTableModel.DELETE_COLUMN);
            return panel.deleteButton().isEnabled();
        });

        // Assert
        assertFalse(initiallyEnabled);
        assertTrue(enabledAfterTick);
        assertFalse(enabledAfterUntick);
    }

    /**
     * D-29: delete confirmation (D7) [unit].
     *
     * <p>Arrange: tick A, B; confirmDelete false, then true.</p>
     * <p>Act: onDeleteClicked.</p>
     * <p>Assert: no request when declined; deleteDocuments([A, B]) when confirmed; ticks cleared.</p>
     */
    @Test
    @DisplayName("D-29 delete confirmation (D7)")
    void onDeleteClicked_confirmedOrDeclined_deletesOnlyWhenConfirmed() {
        // Arrange
        onEdt(() -> {
            panel.onDocumentsChanged(summaries("A", "B", "C"));
            panel.tableModel().setValueAt(true, 0, DocumentTableModel.DELETE_COLUMN);
            panel.tableModel().setValueAt(true, 1, DocumentTableModel.DELETE_COLUMN);
        });
        dialogs.answerConfirmDelete(false).answerConfirmDelete(true);

        // Act
        onEdt(panel::onDeleteClicked);
        List<List<String>> afterDecline = controller.deleteRequests();
        onEdt(panel::onDeleteClicked);

        // Assert
        assertEquals(List.of(), afterDecline);
        assertEquals(List.of("A", "B"), dialogs.questionsOf("confirmDelete").get(0).arguments());
        assertEquals(List.of(List.of("A", "B")), controller.deleteRequests());
        assertFalse(onEdtGet(() -> panel.tableModel().hasSelection()));
        assertFalse(onEdtGet(() -> panel.deleteButton().isEnabled()));
    }

    /**
     * D-30: table refresh [unit].
     *
     * <p>Arrange: fire 3 summaries then 1.</p>
     * <p>Act: onDocumentsChanged.</p>
     * <p>Assert: table rowCount 3 then 1.</p>
     */
    @Test
    @DisplayName("D-30 table refresh")
    void onDocumentsChanged_newList_refreshesTable() {
        // Act
        int afterThree = onEdtGet(() -> {
            panel.onDocumentsChanged(summaries("A", "B", "C"));
            return panel.tableModel().getRowCount();
        });
        int afterOne = onEdtGet(() -> {
            panel.onDocumentsChanged(summaries("B"));
            return panel.tableModel().getRowCount();
        });

        // Assert
        assertEquals(3, afterThree);
        assertEquals(1, afterOne);
    }

    /**
     * D-31: startup load progress on the left (D5) [unit].
     *
     * <p>Arrange: progress (2, 5); completed(LoadSummary(5,0,[])); completed with 1 problem.</p>
     * <p>Act: onStartupLoadProgress / Completed.</p>
     * <p>Assert: 'Loading saved documents... 2 / 5'; '5 documents'; '(1 could not be loaded)' suffix.</p>
     */
    @Test
    @DisplayName("D-31 startup load progress on the left (D5)")
    void startupLoadCallbacks_updateStatusLabel() {
        // Act
        String duringLoad = onEdtGet(() -> {
            panel.onStartupLoadProgress(2, 5);
            return panel.startupStatusLabel().getText();
        });
        String afterLoad = onEdtGet(() -> {
            panel.onDocumentsChanged(summaries("A", "B", "C", "D", "E"));
            panel.onStartupLoadCompleted(new LoadSummary(5, 0, List.of()));
            return panel.startupStatusLabel().getText();
        });
        String withProblem = onEdtGet(() -> {
            panel.onStartupLoadCompleted(new LoadSummary(5, 0, List.of("Entry 'B' is corrupt.")));
            return panel.startupStatusLabel().getText();
        });

        // Assert
        assertEquals("Loading saved documents... 2 / 5", duringLoad);
        assertEquals("5 documents", afterLoad);
        assertEquals("5 documents (1 could not be loaded)", withProblem);
    }

    /**
     * D-52: documents imported while the startup load runs are included in the count.
     */
    @Test
    @DisplayName("D-52 startup count includes documents imported during the load (D5)")
    void onStartupLoadCompleted_documentsImportedDuringLoad_countsTableRows() {
        // Arrange
        onEdt(() -> panel.onDocumentsChanged(summaries("A", "B", "C", "D", "E", "imported")));

        // Act
        String status = onEdtGet(() -> {
            panel.onStartupLoadCompleted(new LoadSummary(5, 0, List.of()));
            return panel.startupStatusLabel().getText();
        });

        // Assert
        assertEquals("6 documents", status);
    }
}
