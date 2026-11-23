package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.SearchQuery;
import views.ScriptedDialogService.Question;

/**
 * Tests for {@code SearchQueryWorkflow}. Owner: Member D.
 *
 * <p>Cases D-25..D-27 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchQueryWorkflowTest {

    private ScriptedDialogService dialogs;

    private SearchQueryWorkflow workflow;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        dialogs = new ScriptedDialogService();
        workflow = new SearchQueryWorkflow(dialogs);
    }

    /**
     * D-25: query entry (UC2, S2) [unit].
     *
     * <p>Arrange: answer '  Computer  Algorithm '.</p>
     * <p>Act: promptForQuery.</p>
     * <p>Assert: SearchQuery text 'Computer  Algorithm'.</p>
     */
    @Test
    @DisplayName("D-25 query entry (UC2, S2)")
    void promptForQuery_validInput_returnsNormalizedQuery() {
        // Arrange
        dialogs.answerSearchQuery(Optional.of("  Computer  Algorithm "));

        // Act
        Optional<SearchQuery> query = workflow.promptForQuery();

        // Assert
        assertEquals("Computer  Algorithm", query.orElseThrow().text());
    }

    /**
     * D-26: validation loop (S2) [unit].
     *
     * <p>Arrange: answers '   ' then 'x'.</p>
     * <p>Act: promptForQuery.</p>
     * <p>Assert: second ask carries error and initial text '   '; returns 'x'.</p>
     */
    @Test
    @DisplayName("D-26 validation loop (S2)")
    void promptForQuery_blankThenValid_reAsksWithError() {
        // Arrange
        dialogs.answerSearchQuery(Optional.of("   ")).answerSearchQuery(Optional.of("x"));

        // Act
        Optional<SearchQuery> query = workflow.promptForQuery();

        // Assert
        List<Question> asked = dialogs.questionsOf("askSearchQuery");
        assertEquals(List.of("", ""), asked.get(0).arguments());
        assertEquals("   ", asked.get(1).arguments().get(0));
        assertFalse(asked.get(1).arguments().get(1).isEmpty());
        assertEquals("x", query.orElseThrow().text());
    }

    /**
     * D-27: cancel [unit].
     *
     * <p>Arrange: no answer.</p>
     * <p>Act: promptForQuery.</p>
     * <p>Assert: Optional.empty().</p>
     */
    @Test
    @DisplayName("D-27 cancel")
    void promptForQuery_cancelled_returnsEmpty() {
        // Arrange
        dialogs.answerSearchQuery(Optional.empty());

        // Act + Assert
        assertEquals(Optional.empty(), workflow.promptForQuery());
    }
}
