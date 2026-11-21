package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.MatchRange;

/**
 * Tests for {@code TextChunkPlanner}. Owner: Member B.
 *
 * <p>Cases B-41..B-43 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class TextChunkPlannerTest {

    private TextChunkPlanner planner;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        planner = new TextChunkPlanner(4);
    }

    /**
     * B-41: chunked text rendering (U6) [unit].
     *
     * <p>Arrange: length 10; chunk size 4.</p>
     * <p>Act: chunks.</p>
     * <p>Assert: [0,4),[4,8),[8,10).</p>
     */
    @Test
    @DisplayName("B-41 chunked text rendering (U6)")
    void chunks_textLengthTen_returnsContiguousCoveringChunks() {
        assertEquals(4, planner.chunkSize());
        assertEquals(List.of(new MatchRange(0, 4), new MatchRange(4, 8), new MatchRange(8, 10)),
                planner.chunks(10));
    }

    /**
     * B-42: empty document [unit].
     *
     * <p>Arrange: length 0.</p>
     * <p>Act: chunks.</p>
     * <p>Assert: empty list.</p>
     */
    @Test
    @DisplayName("B-42 empty document")
    void chunks_zeroLength_returnsNoChunks() {
        assertEquals(List.of(), planner.chunks(0));
    }

    /**
     * B-43: argument validation [unit].
     *
     * <p>Arrange: chunk size 0; chunks(-1).</p>
     * <p>Act: constructor / chunks.</p>
     * <p>Assert: IllegalArgumentException.</p>
     */
    @Test
    @DisplayName("B-43 argument validation")
    void constructor_invalidArguments_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunkPlanner(0));
        assertThrows(IllegalArgumentException.class, () -> planner.chunks(-1));
    }
}
