package views.support;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code HighlightBatchPlanner}. Owner: Member B.
 *
 * <p>Cases B-36..B-40 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
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
    @Disabled("TODO: Member B")
    @DisplayName("B-36 batching (U6)")
    void planBatches_sevenRanges_returnsBatchesOfThreeInOrder() {
        fail("TODO: Member B - B-36");
    }

    /**
     * B-37: empty input [unit].
     *
     * <p>Arrange: List.of().</p>
     * <p>Act: planBatches.</p>
     * <p>Assert: empty list.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-37 empty input")
    void planBatches_emptyList_returnsNoBatches() {
        fail("TODO: Member B - B-37");
    }

    /**
     * B-38: argument validation [unit].
     *
     * <p>Arrange: batch size 0.</p>
     * <p>Act: constructor.</p>
     * <p>Assert: IllegalArgumentException.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-38 argument validation")
    void constructor_nonPositiveBatchSize_throwsIllegalArgumentException() {
        fail("TODO: Member B - B-38");
    }

    /**
     * B-39: windowed highlighting (N8) [unit].
     *
     * <p>Arrange: ranges [0,2),[5,7),[10,12); windows [1,6), [7,10), [0,100).</p>
     * <p>Act: matchesIntersecting / indexOfFirstEndingAfter.</p>
     * <p>Assert: [[0,2),[5,7)], [], all three; indexOfFirstEndingAfter(7) = 2.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-39 windowed highlighting (N8)")
    void matchesIntersecting_window_returnsOnlyIntersectingRanges() {
        fail("TODO: Member B - B-39");
    }

    /**
     * B-40: window lookup speed + mode switch [unit].
     *
     * <p>Arrange: 1,000,000 ranges (MatchRanges); 10,000 random windows.</p>
     * <p>Act: matchesIntersecting / shouldHighlightEagerly.</p>
     * <p>Assert: total &lt;= 200 ms; shouldHighlightEagerly(10000) true, (10001) false.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-40 window lookup speed + mode switch")
    void matchesIntersecting_millionRanges_isLogarithmic() {
        fail("TODO: Member B - B-40");
    }
}
