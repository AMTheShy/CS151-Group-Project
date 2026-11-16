package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import testsupport.FakeDocumentLibraryController;

/**
 * Tests for {@code DocumentReaderPanel}. Owner: Member D.
 *
 * <p>Cases D-43..D-47 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DocumentReaderPanelTest {

    private FakeDocumentLibraryController controller;

    private ScriptedDialogService dialogs;

    private DocumentReaderPanel reader;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        controller = new FakeDocumentLibraryController();
        dialogs = new ScriptedDialogService();
        reader = new DocumentReaderPanel(new DownloadWorkflow(controller, dialogs));
    }

    /**
     * D-43: offsets stay aligned (N9) [unit].
     *
     * <p>Arrange: 'a\r\nb\rc'.</p>
     * <p>Act: toDisplayText.</p>
     * <p>Assert: 'a \nb c'; same length.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-43 offsets stay aligned (N9)")
    void toDisplayText_carriageReturns_replacedOneToOne() {
        fail("TODO: Member D - D-43");
    }

    /**
     * D-44: reader content (U5) [unit].
     *
     * <p>Arrange: result with 3 matches; flush chained EDT steps (invokeAndWait loop).</p>
     * <p>Act: showResult.</p>
     * <p>Assert: text equals display text; not editable; query label contains query and '3 matches';
     * position '1 / 3'.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-44 reader content (U5)")
    void showResult_document_rendersReadOnlyTextAndLabels() {
        fail("TODO: Member D - D-44");
    }

    /**
     * D-45: cyclic Previous/Next (U5) [unit].
     *
     * <p>Arrange: 3 matches; next x3; previous.</p>
     * <p>Act: showNextMatch / showPreviousMatch.</p>
     * <p>Assert: caret at starts of match 1, 2, 0, then 2; position label follows.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-45 cyclic Previous/Next (U5)")
    void navigation_nextAndPrevious_moveCaretCyclically() {
        fail("TODO: Member D - D-45");
    }

    /**
     * D-46: Download button (D8) [unit].
     *
     * <p>Arrange: dialogs script target; click download.</p>
     * <p>Act: onDownloadClicked.</p>
     * <p>Assert: controller.exportRequests has the displayed document.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-46 Download button (D8)")
    void onDownloadClicked_targetChosen_exportsDisplayedSnapshot() {
        fail("TODO: Member D - D-46");
    }

    /**
     * D-47: render 5 MB &lt;= 1 s, no EDT step &gt; 100 ms (3.4, U6) [benchmark].
     *
     * <p>Arrange: 5 MB document with 20,000 matches; time each EDT step.</p>
     * <p>Act: showResult.</p>
     * <p>Assert: total &lt;= 1000 ms; max single step &lt;= 100 ms.</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member D")
    @DisplayName("D-47 render 5 MB <= 1 s, no EDT step > 100 ms (3.4, U6)")
    void showResult_fiveMegabyteDocument_rendersWithinOneSecondWithShortEdtSteps() {
        fail("TODO: Member D - D-47");
    }
}
