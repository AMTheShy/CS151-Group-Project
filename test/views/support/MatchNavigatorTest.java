package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code MatchNavigator}. Owner: Member B.
 *
 * <p>Cases B-30..B-35 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
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
    @DisplayName("B-30 initial state (U5)")
    void constructor_someMatches_startsAtFirstMatch() {
        MatchNavigator navigator = new MatchNavigator(5);
        assertTrue(navigator.hasMatches());
        assertEquals(5, navigator.matchCount());
        assertEquals(0, navigator.currentIndex().orElseThrow());
        assertEquals("1 / 5", navigator.positionLabel());
    }

    /**
     * B-31: cyclic next (UC4) [unit].
     *
     * <p>Arrange: n = 3; next() three times.</p>
     * <p>Act: next.</p>
     * <p>Assert: indices 1, 2, 0.</p>
     */
    @Test
    @DisplayName("B-31 cyclic next (UC4)")
    void next_atLastMatch_wrapsToFirst() {
        MatchNavigator navigator = new MatchNavigator(3);
        assertEquals(1, navigator.next());
        assertEquals(2, navigator.next());
        assertEquals(0, navigator.next());
    }

    /**
     * B-32: cyclic previous (UC4) [unit].
     *
     * <p>Arrange: n = 3; previous().</p>
     * <p>Act: previous.</p>
     * <p>Assert: index 2; label '3 / 3'.</p>
     */
    @Test
    @DisplayName("B-32 cyclic previous (UC4)")
    void previous_atFirstMatch_wrapsToLast() {
        MatchNavigator navigator = new MatchNavigator(3);
        assertEquals(2, navigator.previous());
        assertEquals("3 / 3", navigator.positionLabel());
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
    @DisplayName("B-33 no matches")
    void navigator_zeroMatches_reportsEmptyAndRejectsMoves() {
        MatchNavigator navigator = new MatchNavigator(0);
        assertFalse(navigator.hasMatches());
        assertTrue(navigator.currentIndex().isEmpty());
        assertEquals("0 / 0", navigator.positionLabel());
        assertThrows(IllegalStateException.class, navigator::next);
        assertThrows(IllegalStateException.class, navigator::previous);
    }

    /**
     * B-34: single match [unit].
     *
     * <p>Arrange: n = 1.</p>
     * <p>Act: next / previous.</p>
     * <p>Assert: next() and previous() return 0.</p>
     */
    @Test
    @DisplayName("B-34 single match")
    void nextAndPrevious_singleMatch_stayOnIt() {
        MatchNavigator navigator = new MatchNavigator(1);
        assertEquals(0, navigator.next());
        assertEquals(0, navigator.previous());
        assertEquals("1 / 1", navigator.positionLabel());
    }

    /**
     * B-35: jump to match [unit].
     *
     * <p>Arrange: n = 4; moveTo(2); moveTo(4); moveTo(-1).</p>
     * <p>Act: moveTo.</p>
     * <p>Assert: after moveTo(2) label '3 / 4'; others throw IndexOutOfBoundsException.</p>
     */
    @Test
    @DisplayName("B-35 jump to match")
    void moveTo_outOfRange_throwsIndexOutOfBoundsException() {
        MatchNavigator navigator = new MatchNavigator(4);
        navigator.moveTo(2);
        assertEquals("3 / 4", navigator.positionLabel());
        assertThrows(IndexOutOfBoundsException.class, () -> navigator.moveTo(4));
        assertThrows(IndexOutOfBoundsException.class, () -> navigator.moveTo(-1));
    }
}
