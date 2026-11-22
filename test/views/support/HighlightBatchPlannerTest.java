package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.MatchRange;
import models.MatchRanges;

/**
 * Tests for {@code HighlightBatchPlanner}. Owner: Member B.
 *
 * <p>Cases B-36..B-40 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class HighlightBatchPlannerTest {

    private HighlightBatchPlanner planner;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        planner = new HighlightBatchPlanner(3);
    }

    /**
     * B-36: batching (U6) [unit].
     *
     * <p>Arrange: 7 ranges; batch size 3.</p>
     * <p>Act: planBatches.</p>
     * <p>Assert: 3 batches of sizes 3,3,1; concatenation equals input.</p>
     */
    @Test
    @DisplayName("B-36 batching (U6)")
    void planBatches_sevenRanges_returnsBatchesOfThreeInOrder() {
        List<MatchRange> matches = MatchRanges.of(1, 0, 2, 4, 6, 8, 10, 12);
        List<List<MatchRange>> batches = planner.planBatches(matches);
        assertEquals(3, batches.size());
        assertEquals(List.of(3, 3, 1), batches.stream().map(List::size).toList());
        assertEquals(matches, batches.stream().flatMap(List::stream).toList());
        assertThrows(UnsupportedOperationException.class, () -> batches.add(matches));
    }

    /**
     * B-37: empty input [unit].
     *
     * <p>Arrange: List.of().</p>
     * <p>Act: planBatches.</p>
     * <p>Assert: empty list.</p>
     */
    @Test
    @DisplayName("B-37 empty input")
    void planBatches_emptyList_returnsNoBatches() {
        assertEquals(List.of(), planner.planBatches(List.of()));
    }

    /**
     * B-38: argument validation [unit].
     *
     * <p>Arrange: batch size 0.</p>
     * <p>Act: constructor.</p>
     * <p>Assert: IllegalArgumentException.</p>
     */
    @Test
    @DisplayName("B-38 argument validation")
    void constructor_nonPositiveBatchSize_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new HighlightBatchPlanner(0));
        assertThrows(NullPointerException.class, () -> planner.planBatches(null));
    }

    /**
     * B-39: windowed highlighting (N8) [unit].
     *
     * <p>Arrange: ranges [0,2),[5,7),[10,12); windows [1,6), [7,10), [0,100).</p>
     * <p>Act: matchesIntersecting / indexOfFirstEndingAfter.</p>
     * <p>Assert: [[0,2),[5,7)], [], all three; indexOfFirstEndingAfter(7) = 2.</p>
     */
    @Test
    @DisplayName("B-39 windowed highlighting (N8)")
    void matchesIntersecting_window_returnsOnlyIntersectingRanges() {
        List<MatchRange> matches = MatchRanges.of(2, 0, 5, 10);
        assertEquals(matches.subList(0, 2), planner.matchesIntersecting(matches, 1, 6));
        assertEquals(List.of(), planner.matchesIntersecting(matches, 7, 10));
        assertEquals(matches, planner.matchesIntersecting(matches, 0, 100));
        assertEquals(2, planner.indexOfFirstEndingAfter(matches, 7));
        assertThrows(IllegalArgumentException.class,
                () -> planner.matchesIntersecting(matches, 10, 9));
    }

    /**
     * B-40: window lookup speed + mode switch [unit].
     *
     * <p>Arrange: 1,000,000 ranges (MatchRanges); 10,000 random windows.</p>
     * <p>Act: matchesIntersecting / shouldHighlightEagerly.</p>
     * <p>Assert: total &lt;= 200 ms; shouldHighlightEagerly(10000) true, (10001) false.</p>
     */
    @Test
    @DisplayName("B-40 window lookup speed + mode switch")
    void matchesIntersecting_millionRanges_isLogarithmic() {
        MatchRanges.Builder builder = MatchRanges.builder(1);
        for (int index = 0; index < 1_000_000; index++) {
            builder.add(index * 2);
        }
        List<MatchRange> matches = builder.build();
        Random random = new Random(2026);
        for (int index = 0; index < 500; index++) {
            int offset = random.nextInt(2_000_000);
            planner.matchesIntersecting(matches, offset, offset + 100);
        }
        long start = System.nanoTime();
        for (int index = 0; index < 10_000; index++) {
            int offset = random.nextInt(2_000_000);
            List<MatchRange> window = planner.matchesIntersecting(matches, offset, offset + 100);
            assertTrue(window.size() <= 51);
        }
        double millis = (System.nanoTime() - start) / 1_000_000.0;
        assertTrue(millis <= 200, "Window lookups took " + millis + " ms");
        assertTrue(planner.shouldHighlightEagerly(10_000));
        assertFalse(planner.shouldHighlightEagerly(10_001));
    }
}
