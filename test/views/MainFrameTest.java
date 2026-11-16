package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.FakeDocumentLibraryController;
import testsupport.FakeLifecycleController;
import testsupport.FakeSearchController;

/**
 * Tests for {@code MainFrame}. Owner: Member D.
 *
 * <p>Cases D-48..D-49 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 *
 * <p>Note: Requires a display: start each test with
 * Assumptions.assumeFalse(GraphicsEnvironment.isHeadless()).</p>
 */
class MainFrameTest {

    private FakeLifecycleController lifecycle;

    private MainFrame frame;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        lifecycle = new FakeLifecycleController();
        frame = new MainFrame(
                new FakeDocumentLibraryController(),
                new FakeSearchController(),
                lifecycle,
                new ScriptedDialogService());
    }

    /**
     * D-48: single window layout (U1) [unit].
     *
     * <p>Arrange: new MainFrame on the EDT.</p>
     * <p>Act: constructor.</p>
     * <p>Assert: menus File(Import..., Exit) and Search(Search By...); content is a JSplitPane with library
     * panel left and results pane right.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-48 single window layout (U1)")
    void constructor_buildsSingleFrameWithMenusAndSplitPane() {
        fail("TODO: Member D - D-48");
    }

    /**
     * D-49: Exit (U1, R8) [unit].
     *
     * <p>Arrange: onExitRequested().</p>
     * <p>Act: onExitRequested.</p>
     * <p>Assert: lifecycle.shutdownCalls 1; frame not displayable.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-49 Exit (U1, R8)")
    void onExitRequested_shutsDownAndDisposes() {
        fail("TODO: Member D - D-49");
    }
}
