package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
    @Disabled("TODO: Member D")
    @DisplayName("D-25 query entry (UC2, S2)")
    void promptForQuery_validInput_returnsNormalizedQuery() {
        fail("TODO: Member D - D-25");
    }

    /**
     * D-26: validation loop (S2) [unit].
     *
     * <p>Arrange: answers '   ' then 'x'.</p>
     * <p>Act: promptForQuery.</p>
     * <p>Assert: second ask carries error and initial text '   '; returns 'x'.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-26 validation loop (S2)")
    void promptForQuery_blankThenValid_reAsksWithError() {
        fail("TODO: Member D - D-26");
    }

    /**
     * D-27: cancel [unit].
     *
     * <p>Arrange: no answer.</p>
     * <p>Act: promptForQuery.</p>
     * <p>Assert: Optional.empty().</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-27 cancel")
    void promptForQuery_cancelled_returnsEmpty() {
        fail("TODO: Member D - D-27");
    }
}
