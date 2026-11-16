package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MatchRange} (S3). Owner: Eric. */
class MatchRangeTest {

    /** E-17: a valid half-open range. */
    @Test
    @DisplayName("E-17 constructor with 0 <= start < end creates range with correct length")
    void constructor_validBounds_createsRange() {
        MatchRange range = new MatchRange(3, 7);

        assertEquals(3, range.start());
        assertEquals(7, range.end());
        assertEquals(4, range.length());
    }

    /** E-18: negative, empty and reversed ranges are rejected. */
    @Test
    @DisplayName("E-18 constructor with negative start, empty or reversed range throws IllegalArgumentException")
    void constructor_invalidBounds_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new MatchRange(-1, 2));
        assertThrows(IllegalArgumentException.class, () -> new MatchRange(2, 2));
        assertThrows(IllegalArgumentException.class, () -> new MatchRange(5, 2));
    }

    /** E-19: shifting and intersection helpers. */
    @Test
    @DisplayName("E-19 shiftedBy moves both ends and intersects detects overlap of half-open intervals")
    void shiftedBy_positiveDelta_movesBothEnds() {
        MatchRange range = new MatchRange(3, 7);

        assertEquals(new MatchRange(13, 17), range.shiftedBy(10));
        assertThrows(IllegalArgumentException.class, () -> range.shiftedBy(-4));
        assertTrue(range.intersects(6, 10));
        assertFalse(range.intersects(7, 10));
        assertFalse(range.intersects(0, 3));
    }

    /** E-20: natural order by start, then end. */
    @Test
    @DisplayName("E-20 compareTo orders by start then end")
    void compareTo_unsortedRanges_sortsByStartThenEnd() {
        List<MatchRange> ranges = new ArrayList<>(List.of(
                new MatchRange(5, 9), new MatchRange(1, 4), new MatchRange(1, 2)));

        Collections.sort(ranges);

        assertEquals(List.of(new MatchRange(1, 2), new MatchRange(1, 4), new MatchRange(5, 9)), ranges);
    }
}
