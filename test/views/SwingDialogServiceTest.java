package views;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code SwingDialogService.buildDeletionList}. Owner: Member D.
 *
 * <p>Cases D-14..D-15 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SwingDialogServiceTest {

    /**
     * D-14: confirmation lists all titles (D7) [unit].
     *
     * <p>Arrange: 5 titles.</p>
     * <p>Act: buildDeletionList.</p>
     * <p>Assert: JScrollPane whose JList model contains the 5 titles in order.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-14 confirmation lists all titles (D7)")
    void buildDeletionList_titles_listedInScrollPane() {
        fail("TODO: Member D - D-14");
    }

    /**
     * D-15: dialog size controlled (D7) [unit].
     *
     * <p>Arrange: 1000 titles.</p>
     * <p>Act: buildDeletionList.</p>
     * <p>Assert: preferred size &lt;= DELETION_LIST_MAX_SIZE in both dimensions.</p>
     */
    @Test
    @Disabled("TODO: Member D")
    @DisplayName("D-15 dialog size controlled (D7)")
    void buildDeletionList_thousandTitles_sizeBounded() {
        fail("TODO: Member D - D-15");
    }
}
