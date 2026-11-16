package views.support;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code TextChunkPlanner}. Owner: Member B.
 *
 * <p>Cases B-41..B-43 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
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
    @Disabled("TODO: Member B")
    @DisplayName("B-41 chunked text rendering (U6)")
    void chunks_textLengthTen_returnsContiguousCoveringChunks() {
        fail("TODO: Member B - B-41");
    }

    /**
     * B-42: empty document [unit].
     *
     * <p>Arrange: length 0.</p>
     * <p>Act: chunks.</p>
     * <p>Assert: empty list.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-42 empty document")
    void chunks_zeroLength_returnsNoChunks() {
        fail("TODO: Member B - B-42");
    }

    /**
     * B-43: argument validation [unit].
     *
     * <p>Arrange: chunk size 0; chunks(-1).</p>
     * <p>Act: constructor / chunks.</p>
     * <p>Assert: IllegalArgumentException.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-43 argument validation")
    void constructor_invalidArguments_throwIllegalArgumentException() {
        fail("TODO: Member B - B-43");
    }
}
