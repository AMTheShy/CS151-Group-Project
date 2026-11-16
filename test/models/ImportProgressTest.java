package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ImportProgress} and {@link ImportState} (U2). Owner: Eric. */
class ImportProgressTest {

    /** E-33: factories produce the expected states. */
    @Test
    @DisplayName("E-33 factories create progress in the expected state; terminal states are terminal")
    void factories_eachState_createExpectedState() {
        assertEquals(ImportState.QUEUED, ImportProgress.queued(1, "A").state());
        assertEquals(ImportState.READING, ImportProgress.reading(1, "A", 5, 10).state());
        assertEquals(10, ImportProgress.saving(1, "A", 10).bytesRead());
        assertEquals(ImportState.COMPLETED, ImportProgress.completed(1, "A", 10).state());
        assertEquals(ImportState.CANCELLED, ImportProgress.cancelled(1, "A", 0, 0).state());
        assertTrue(ImportState.FAILED.isTerminal());
        assertFalse(ImportState.SAVING.isTerminal());
    }

    /** E-34: errorMessage present exactly for FAILED; other invariants. */
    @Test
    @DisplayName("E-34 constructor requires errorMessage exactly for FAILED and valid counts")
    void constructor_errorMessageMismatch_throwsIllegalArgumentException() {
        assertEquals(Optional.of("boom"), ImportProgress.failed(1, "A", 0, 0, "boom").errorMessage());
        assertThrows(IllegalArgumentException.class,
                () -> new ImportProgress(1, "A", 0, 0, ImportState.FAILED, Optional.empty()));
        assertThrows(IllegalArgumentException.class,
                () -> new ImportProgress(1, "A", 0, 0, ImportState.READING, Optional.of("x")));
        assertThrows(IllegalArgumentException.class, () -> ImportProgress.reading(1, "A", 11, 10));
        assertThrows(IllegalArgumentException.class, () -> ImportProgress.queued(0, "A"));
    }

    /** E-35: progress fraction and percentage. */
    @Test
    @DisplayName("E-35 fractionComplete and percentComplete handle unknown size and completion")
    void fractionComplete_variousStates_computesFraction() {
        assertEquals(0.25, ImportProgress.reading(1, "A", 25, 100).fractionComplete(), 1e-9);
        assertEquals(33, ImportProgress.reading(1, "A", 1, 3).percentComplete());
        assertEquals(0.0, ImportProgress.queued(1, "A").fractionComplete(), 1e-9);
        assertEquals(100, ImportProgress.completed(1, "A", 0).percentComplete());
    }
}
