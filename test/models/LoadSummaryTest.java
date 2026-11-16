package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link LoadSummary} (D5). Owner: Eric. */
class LoadSummaryTest {

    /** E-46: problems are copied; negative counts rejected. */
    @Test
    @DisplayName("E-46 constructor copies problems and rejects negative counts")
    void constructor_mutableProblems_isDefensivelyCopied() {
        List<String> problems = new ArrayList<>(List.of("corrupt entry"));

        LoadSummary summary = new LoadSummary(2, 1, problems);
        problems.add("later");

        assertEquals(List.of("corrupt entry"), summary.problems());
        assertTrue(summary.hasProblems());
        assertFalse(LoadSummary.empty().hasProblems());
        assertThrows(IllegalArgumentException.class, () -> new LoadSummary(-1, 0, List.of()));
    }
}
