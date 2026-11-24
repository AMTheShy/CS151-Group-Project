package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.ImportProgress;
import views.support.ImportProgressTracker;

/**
 * Tests for {@code ImportProgressPanel}. Owner: Member D.
 *
 * <p>Cases D-32..D-32 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class ImportProgressPanelTest {

    private ImportProgressPanel panel;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        panel = onEdtGet(() -> new ImportProgressPanel(new ImportProgressTracker()));
    }

    /**
     * D-32: progress rows (U2) [unit].
     *
     * <p>Arrange: queued(1), queued(2), reading(1, 50%), failed(2).</p>
     * <p>Act: onImportProgress.</p>
     * <p>Assert: visibleRowCount 2; panel visible; row 2 shows failure.</p>
     */
    @Test
    @DisplayName("D-32 progress rows (U2)")
    void onImportProgress_events_createUpdateAndKeepFailedRows() {
        // Act
        onEdt(() -> {
            panel.onImportProgress(ImportProgress.queued(1, "A"));
            panel.onImportProgress(ImportProgress.queued(2, "B"));
            panel.onImportProgress(ImportProgress.reading(1, "A", 50, 100));
            panel.onImportProgress(ImportProgress.failed(2, "B", 0, 0, "File \"b.txt\" is not ASCII."));
        });

        // Assert
        assertEquals(2, onEdtGet(panel::visibleRowCount));
        assertTrue(onEdtGet(panel::isVisible));
        assertEquals("Reading 50%", onEdtGet(() -> panel.statusTextOf(1)).orElseThrow());
        assertEquals("Failed: File \"b.txt\" is not ASCII.", onEdtGet(() -> panel.statusTextOf(2)).orElseThrow());
    }
}
