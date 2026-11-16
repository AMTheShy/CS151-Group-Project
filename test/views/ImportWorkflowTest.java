package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.FakeDocumentLibraryController;

/**
 * Tests for {@code ImportWorkflow}. Owner: Member D.
 *
 * <p>Cases D-16..D-20 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class ImportWorkflowTest {

    private FakeDocumentLibraryController controller;

    private ScriptedDialogService dialogs;

    private ImportWorkflow workflow;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        controller = new FakeDocumentLibraryController();
        dialogs = new ScriptedDialogService();
        workflow = new ImportWorkflow(controller, dialogs);
    }

    /**
     * D-16: cancel at file chooser [unit].
     *
     * <p>Arrange: dialogs script: no file.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns empty; no import request; title never asked.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-16 cancel at file chooser")
    void run_fileChooserCancelled_startsNothing() {
        fail("TODO: Member D - D-16");
    }

    /**
     * D-17: default title (D1) [unit].
     *
     * <p>Arrange: file 'notes.txt'; user accepts suggestion.</p>
     * <p>Act: run.</p>
     * <p>Assert: askTitle got 'notes'; import request ('notes', replace=false).</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-17 default title (D1)")
    void run_acceptDefaultTitle_importsWithSuggestedTitle() {
        fail("TODO: Member D - D-17");
    }

    /**
     * D-18: title validation loop (D1) [unit].
     *
     * <p>Arrange: answers '   ' then 'Good'.</p>
     * <p>Act: run.</p>
     * <p>Assert: second askTitle carries the 'empty' error; import title 'Good'.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-18 title validation loop (D1)")
    void run_invalidTitle_reAsksWithErrorMessage() {
        fail("TODO: Member D - D-18");
    }

    /**
     * D-19: overwrite confirmation (D2) [unit].
     *
     * <p>Arrange: controller.markTitleTaken('notes'); confirm = true.</p>
     * <p>Act: run.</p>
     * <p>Assert: import request replace=true.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-19 overwrite confirmation (D2)")
    void run_takenTitleConfirmed_importsWithReplace() {
        fail("TODO: Member D - D-19");
    }

    /**
     * D-20: overwrite declined (D2) [unit].
     *
     * <p>Arrange: title taken; confirm = false.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns empty; no import request.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-20 overwrite declined (D2)")
    void run_takenTitleDeclined_startsNothing() {
        fail("TODO: Member D - D-20");
    }
}
