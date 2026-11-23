package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.FakeDocumentLibraryController.ImportRequest;
import testsupport.FakeDocumentLibraryController;
import views.ScriptedDialogService.Question;

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
    @DisplayName("D-16 cancel at file chooser")
    void run_fileChooserCancelled_startsNothing() {
        // Arrange
        dialogs.answerFileToImport(Optional.empty());

        // Act
        OptionalLong importId = workflow.run();

        // Assert
        assertTrue(importId.isEmpty());
        assertEquals(List.of(), controller.importRequests());
        assertEquals(List.of(), dialogs.questionsOf("askTitle"));
    }

    /**
     * D-17: default title (D1) [unit].
     *
     * <p>Arrange: file 'notes.txt'; user accepts suggestion.</p>
     * <p>Act: run.</p>
     * <p>Assert: askTitle got 'notes'; import request ('notes', replace=false).</p>
     */
    @Test
    @DisplayName("D-17 default title (D1)")
    void run_acceptDefaultTitle_importsWithSuggestedTitle() {
        // Arrange
        Path file = Path.of("dir", "notes.txt");
        dialogs.answerFileToImport(Optional.of(file)).answerTitle(Optional.of("notes"));

        // Act
        OptionalLong importId = workflow.run();

        // Assert
        assertTrue(importId.isPresent());
        assertEquals(List.of("notes", ""), dialogs.questionsOf("askTitle").get(0).arguments());
        assertEquals(List.of(new ImportRequest(importId.getAsLong(), file, "notes", false)),
                controller.importRequests());
    }

    /**
     * D-18: title validation loop (D1) [unit].
     *
     * <p>Arrange: answers '   ' then 'Good'.</p>
     * <p>Act: run.</p>
     * <p>Assert: second askTitle carries the 'empty' error; import title 'Good'.</p>
     */
    @Test
    @DisplayName("D-18 title validation loop (D1)")
    void run_invalidTitle_reAsksWithErrorMessage() {
        // Arrange
        dialogs.answerFileToImport(Optional.of(Path.of("notes.txt")))
                .answerTitle(Optional.of("   "))
                .answerTitle(Optional.of("Good"));

        // Act
        workflow.run();

        // Assert
        List<Question> asked = dialogs.questionsOf("askTitle");
        assertEquals(2, asked.size());
        assertEquals("", asked.get(0).arguments().get(1));
        assertEquals("   ", asked.get(1).arguments().get(0));
        assertTrue(asked.get(1).arguments().get(1).contains("empty"), asked.get(1).arguments().get(1));
        assertEquals("Good", controller.importRequests().get(0).title());
    }

    /**
     * D-19: overwrite confirmation (D2) [unit].
     *
     * <p>Arrange: controller.markTitleTaken('notes'); confirm = true.</p>
     * <p>Act: run.</p>
     * <p>Assert: import request replace=true.</p>
     */
    @Test
    @DisplayName("D-19 overwrite confirmation (D2)")
    void run_takenTitleConfirmed_importsWithReplace() {
        // Arrange
        controller.markTitleTaken("notes");
        dialogs.answerFileToImport(Optional.of(Path.of("notes.txt")))
                .answerTitle(Optional.of("notes"))
                .answerConfirmOverwriteDocument(true);

        // Act
        workflow.run();

        // Assert
        assertEquals(List.of("notes"), dialogs.questionsOf("confirmOverwriteDocument").get(0).arguments());
        assertEquals(1, controller.importRequests().size());
        assertTrue(controller.importRequests().get(0).replaceExisting());
    }

    /**
     * D-20: overwrite declined (D2) [unit].
     *
     * <p>Arrange: title taken; confirm = false.</p>
     * <p>Act: run.</p>
     * <p>Assert: returns empty; no import request.</p>
     */
    @Test
    @DisplayName("D-20 overwrite declined (D2)")
    void run_takenTitleDeclined_startsNothing() {
        // Arrange
        controller.markTitleTaken("notes");
        dialogs.answerFileToImport(Optional.of(Path.of("notes.txt")))
                .answerTitle(Optional.of("notes"))
                .answerConfirmOverwriteDocument(false);

        // Act
        OptionalLong importId = workflow.run();

        // Assert
        assertTrue(importId.isEmpty());
        assertEquals(List.of(), controller.importRequests());
    }
}
