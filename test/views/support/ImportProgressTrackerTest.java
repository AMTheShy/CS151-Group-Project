package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.ImportProgress;
import models.ImportState;

/**
 * Tests for {@code ImportProgressTracker}. Owner: Member B.
 *
 * <p>Cases B-57..B-64 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class ImportProgressTrackerTest {

    private ImportProgressTracker tracker;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        tracker = new ImportProgressTracker();
    }

    /**
     * B-57: new import row (U2) [unit].
     *
     * <p>Arrange: apply(queued(1,'A')).</p>
     * <p>Act: apply.</p>
     * <p>Assert: returns true; rows() has one QUEUED row.</p>
     */
    @Test
    @DisplayName("B-57 new import row (U2)")
    void apply_queuedForNewId_addsRow() {
        ImportProgress progress = ImportProgress.queued(1, "A");
        assertTrue(tracker.apply(progress));
        assertEquals(List.of(progress), tracker.rows());
        assertEquals(ImportState.QUEUED, tracker.row(1).orElseThrow().state());
    }

    /**
     * B-58: progress update [unit].
     *
     * <p>Arrange: queued then reading(1,'A',50,100).</p>
     * <p>Act: apply.</p>
     * <p>Assert: single row with 50%.</p>
     */
    @Test
    @DisplayName("B-58 progress update")
    void apply_readingForKnownId_replacesRow() {
        tracker.apply(ImportProgress.queued(1, "A"));
        assertTrue(tracker.apply(ImportProgress.reading(1, "A", 50, 100)));
        assertEquals(1, tracker.rows().size());
        assertEquals(50, tracker.row(1).orElseThrow().percentComplete());
    }

    /**
     * B-59: completed row removed after linger [unit].
     *
     * <p>Arrange: completed(1,'A',10); remove(1).</p>
     * <p>Act: apply / remove.</p>
     * <p>Assert: remove returns true; rows empty.</p>
     */
    @Test
    @DisplayName("B-59 completed row removed after linger")
    void remove_completedRow_rowDisappears() {
        tracker.apply(ImportProgress.completed(1, "A", 10));
        assertTrue(tracker.remove(1));
        assertEquals(List.of(), tracker.rows());
        assertFalse(tracker.remove(1));
    }

    /**
     * B-60: failed row stays (U2) [unit].
     *
     * <p>Arrange: failed(1,'A',0,0,'boom').</p>
     * <p>Act: apply.</p>
     * <p>Assert: row FAILED with message 'boom'.</p>
     */
    @Test
    @DisplayName("B-60 failed row stays (U2)")
    void apply_failed_rowKeptWithMessage() {
        ImportProgress failed = ImportProgress.failed(1, "A", 0, 0, "boom");
        tracker.apply(failed);
        assertEquals(ImportState.FAILED, tracker.row(1).orElseThrow().state());
        assertEquals("boom", tracker.row(1).orElseThrow().errorMessage().orElseThrow());
    }

    /**
     * B-61: late update must not resurrect [unit].
     *
     * <p>Arrange: completed, remove(1), then reading(1,...).</p>
     * <p>Act: apply.</p>
     * <p>Assert: apply returns false; rows empty.</p>
     */
    @Test
    @DisplayName("B-61 late update must not resurrect")
    void apply_updateAfterRemove_isIgnored() {
        tracker.apply(ImportProgress.completed(1, "A", 10));
        tracker.remove(1);
        assertFalse(tracker.apply(ImportProgress.reading(1, "A", 5, 10)));
        assertEquals(List.of(), tracker.rows());
    }

    /**
     * B-62: out-of-order protection [unit].
     *
     * <p>Arrange: completed(1) then reading(1).</p>
     * <p>Act: apply.</p>
     * <p>Assert: row stays COMPLETED.</p>
     */
    @Test
    @DisplayName("B-62 out-of-order protection")
    void apply_nonTerminalAfterTerminal_isIgnored() {
        ImportProgress completed = ImportProgress.completed(1, "A", 10);
        tracker.apply(completed);
        assertFalse(tracker.apply(ImportProgress.reading(1, "A", 5, 10)));
        assertEquals(completed, tracker.row(1).orElseThrow());
    }

    /**
     * B-63: ordering [unit].
     *
     * <p>Arrange: apply ids 3, 1, 2.</p>
     * <p>Act: rows.</p>
     * <p>Assert: rows ids [1, 2, 3].</p>
     */
    @Test
    @DisplayName("B-63 ordering")
    void rows_multipleImports_orderedById() {
        tracker.apply(ImportProgress.queued(3, "C"));
        tracker.apply(ImportProgress.queued(1, "A"));
        tracker.apply(ImportProgress.queued(2, "B"));
        assertEquals(List.of(1L, 2L, 3L), tracker.rows().stream().map(ImportProgress::importId).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> tracker.rows().add(ImportProgress.queued(4, "D")));
    }

    /**
     * B-64: status column text [unit].
     *
     * <p>Arrange: one progress per state.</p>
     * <p>Act: statusText.</p>
     * <p>Assert: 'Queued', 'Reading 45%', 'Saving', 'Done', 'Cancelled', 'Failed: boom'.</p>
     */
    @Test
    @DisplayName("B-64 status column text")
    void statusText_eachState_formatsAsSpecified() {
        assertEquals("Queued", ImportProgressTracker.statusText(ImportProgress.queued(1, "A")));
        assertEquals("Reading 45%",
                ImportProgressTracker.statusText(ImportProgress.reading(1, "A", 45, 100)));
        assertEquals("Saving", ImportProgressTracker.statusText(ImportProgress.saving(1, "A", 100)));
        assertEquals("Done", ImportProgressTracker.statusText(ImportProgress.completed(1, "A", 100)));
        assertEquals("Cancelled", ImportProgressTracker.statusText(ImportProgress.cancelled(1, "A", 0, 0)));
        assertEquals("Failed: boom",
                ImportProgressTracker.statusText(ImportProgress.failed(1, "A", 0, 0, "boom")));
    }
}
