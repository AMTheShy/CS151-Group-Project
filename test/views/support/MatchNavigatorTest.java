package views.support;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code MatchNavigator}. Owner: Member B.
 *
 * <p>Cases B-30..B-35 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class MatchNavigatorTest {

    /**
     * B-30: initial state (U5) [unit].
     *
     * <p>Arrange: new MatchNavigator(5).</p>
     * <p>Act: constructor / positionLabel.</p>
     * <p>Assert: currentIndex 0; positionLabel '1 / 5'.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-30 initial state (U5)")
    void constructor_someMatches_startsAtFirstMatch() {
        fail("TODO: Member B - B-30");
    }

    /**
     * B-31: cyclic next (UC4) [unit].
     *
     * <p>Arrange: n = 3; next() three times.</p>
     * <p>Act: next.</p>
     * <p>Assert: indices 1, 2, 0.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-31 cyclic next (UC4)")
    void next_atLastMatch_wrapsToFirst() {
        fail("TODO: Member B - B-31");
    }

    /**
     * B-32: cyclic previous (UC4) [unit].
     *
     * <p>Arrange: n = 3; previous().</p>
     * <p>Act: previous.</p>
     * <p>Assert: index 2; label '3 / 3'.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-32 cyclic previous (UC4)")
    void previous_atFirstMatch_wrapsToLast() {
        fail("TODO: Member B - B-32");
    }

    /**
     * B-33: no matches [unit].
     *
     * <p>Arrange: new MatchNavigator(0).</p>
     * <p>Act: all.</p>
     * <p>Assert: hasMatches false; currentIndex empty; label '0 / 0'; next/previous throw
     * IllegalStateException.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-33 no matches")
    void navigator_zeroMatches_reportsEmptyAndRejectsMoves() {
        fail("TODO: Member B - B-33");
    }

    /**
     * B-34: single match [unit].
     *
     * <p>Arrange: n = 1.</p>
     * <p>Act: next / previous.</p>
     * <p>Assert: next() and previous() return 0.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-34 single match")
    void nextAndPrevious_singleMatch_stayOnIt() {
        fail("TODO: Member B - B-34");
    }

    /**
     * B-35: jump to match [unit].
     *
     * <p>Arrange: n = 4; moveTo(2); moveTo(4); moveTo(-1).</p>
     * <p>Act: moveTo.</p>
     * <p>Assert: after moveTo(2) label '3 / 4'; others throw IndexOutOfBoundsException.</p>
     */
    @Test
    @Disabled("TODO: Member B")
    @DisplayName("B-35 jump to match")
    void moveTo_outOfRange_throwsIndexOutOfBoundsException() {
        fail("TODO: Member B - B-35");
    }
}
