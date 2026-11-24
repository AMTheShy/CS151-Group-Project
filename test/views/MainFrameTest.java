package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
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
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(), "MainFrame needs a display");
        lifecycle = new FakeLifecycleController();
        frame = onEdtGet(() -> new MainFrame(
                new FakeDocumentLibraryController(),
                new FakeSearchController(),
                lifecycle,
                new ScriptedDialogService()));
    }

    /** Disposes the frame (no window left behind). */
    @AfterEach
    void tearDown() {
        if (frame != null) {
            onEdt(frame::dispose);
        }
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
    @DisplayName("D-48 single window layout (U1)")
    void constructor_buildsSingleFrameWithMenusAndSplitPane() {
        // Act
        List<String> menus = onEdtGet(() -> {
            JMenuBar menuBar = frame.getJMenuBar();
            List<String> texts = new ArrayList<>();
            for (int menuIndex = 0; menuIndex < menuBar.getMenuCount(); menuIndex++) {
                JMenu menu = menuBar.getMenu(menuIndex);
                StringBuilder entry = new StringBuilder(menu.getText()).append(':');
                for (int itemIndex = 0; itemIndex < menu.getItemCount(); itemIndex++) {
                    JMenuItem item = menu.getItem(itemIndex);
                    entry.append(item == null ? "|" : "[" + item.getText() + "]");
                }
                texts.add(entry.toString());
            }
            return texts;
        });

        // Assert
        assertEquals(List.of("File:[Import...]|[Exit]", "Search:[Search By...]"), menus);
        JSplitPane splitPane = assertInstanceOf(JSplitPane.class, onEdtGet(frame::getContentPane));
        assertSame(frame.libraryPanel(), onEdtGet(splitPane::getLeftComponent));
        assertSame(frame.searchResultsPane(), onEdtGet(splitPane::getRightComponent));
    }

    /**
     * D-49: Exit (U1, R8) [unit].
     *
     * <p>Arrange: onExitRequested().</p>
     * <p>Act: onExitRequested.</p>
     * <p>Assert: lifecycle.shutdownCalls 1; frame not displayable.</p>
     */
    @Test
    @DisplayName("D-49 Exit (U1, R8)")
    void onExitRequested_shutsDownAndDisposes() {
        // Act
        onEdt(frame::onExitRequested);

        // Assert
        assertEquals(1, lifecycle.shutdownCalls());
        assertFalse(onEdtGet(frame::isDisplayable));
    }
}
