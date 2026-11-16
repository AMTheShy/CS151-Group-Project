package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code ClosableTabHeader}. Owner: Member D.
 *
 * <p>Cases D-34..D-34 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class ClosableTabHeaderTest {

    /**
     * D-34: closable tab (U3) [unit].
     *
     * <p>Arrange: header('title', 'full', counter::increment); closeButton().doClick().</p>
     * <p>Act: closeButton.</p>
     * <p>Assert: counter 1; label text 'title'; tooltip 'full'.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-34 closable tab (U3)")
    void closeButton_click_runsCallbackOnce() {
        fail("TODO: Member D - D-34");
    }
}
