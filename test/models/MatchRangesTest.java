package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link MatchRanges} (S3, N6). Owner: Eric. */
class MatchRangesTest {

    /** E-21: builder collects ascending fixed-length ranges. */
    @Test
    @DisplayName("E-21 builder builds ascending fixed-length ranges, growing past initial capacity")
    void builder_manyAscendingStarts_buildsAllRanges() {
        MatchRanges.Builder builder = MatchRanges.builder(2);
        for (int index = 0; index < 100; index++) {
            builder.add(index * 3);
        }

        MatchRanges ranges = builder.build();

        assertEquals(100, ranges.size());
        assertEquals(new MatchRange(297, 299), ranges.get(99));
        assertEquals(297, ranges.startAt(99));
        assertEquals(2, ranges.matchLength());
    }

    /** E-22: overlapping, descending or negative starts are rejected. */
    @Test
    @DisplayName("E-22 builder rejects overlapping, descending and negative starts; build twice fails")
    void builder_overlappingStart_throwsIllegalArgumentException() {
        MatchRanges.Builder builder = MatchRanges.builder(2).add(0);

        assertThrows(IllegalArgumentException.class, () -> builder.add(1));
        assertThrows(IllegalArgumentException.class, () -> MatchRanges.of(2, 4, 0));
        assertThrows(IllegalArgumentException.class, () -> MatchRanges.of(2, -1));
        assertThrows(IllegalArgumentException.class, () -> MatchRanges.builder(0));
        builder.add(2).build();
        assertThrows(IllegalStateException.class, builder::build);
    }

    /** E-23: the list is immutable. */
    @Test
    @DisplayName("E-23 MatchRanges is immutable")
    void add_onBuiltList_throwsUnsupportedOperationException() {
        MatchRanges ranges = MatchRanges.of(2, 0, 2);

        assertThrows(UnsupportedOperationException.class, () -> ranges.add(new MatchRange(4, 6)));
        assertThrows(UnsupportedOperationException.class, () -> ranges.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> ranges.set(0, new MatchRange(8, 10)));
    }

    /** E-24: List contract - equals a regular list with the same elements. */
    @Test
    @DisplayName("E-24 equals and hashCode follow the List contract; empty list behaves")
    void equals_regularListWithSameRanges_isEqual() {
        MatchRanges ranges = MatchRanges.of(2, 0, 2);
        List<MatchRange> expected = List.of(new MatchRange(0, 2), new MatchRange(2, 4));

        assertEquals(expected, ranges);
        assertEquals(expected.hashCode(), ranges.hashCode());
        assertTrue(MatchRanges.empty(3).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> ranges.get(2));
    }
}
