package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
        panel = new ImportProgressPanel(new ImportProgressTracker());
    }

    /**
     * D-32: progress rows (U2) [unit].
     *
     * <p>Arrange: queued(1), queued(2), reading(1, 50%), failed(2).</p>
     * <p>Act: onImportProgress.</p>
     * <p>Assert: visibleRowCount 2; panel visible; row 2 shows failure.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-32 progress rows (U2)")
    void onImportProgress_events_createUpdateAndKeepFailedRows() {
        fail("TODO: Member D - D-32");
    }
}
