package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.onEdtGet;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JScrollPane;

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
    @DisplayName("D-14 confirmation lists all titles (D7)")
    void buildDeletionList_titles_listedInScrollPane() {
        // Arrange
        List<String> titles = List.of("A", "B", "C", "D", "E");

        // Act
        JComponent component = onEdtGet(() -> SwingDialogService.buildDeletionList(titles));

        // Assert
        JScrollPane scrollPane = assertInstanceOf(JScrollPane.class, component);
        JList<?> titleList = assertInstanceOf(JList.class, scrollPane.getViewport().getView());
        List<Object> listed = new ArrayList<>();
        for (int index = 0; index < titleList.getModel().getSize(); index++) {
            listed.add(titleList.getModel().getElementAt(index));
        }
        assertEquals(titles, listed);
    }

    /**
     * D-15: dialog size controlled (D7) [unit].
     *
     * <p>Arrange: 1000 titles.</p>
     * <p>Act: buildDeletionList.</p>
     * <p>Assert: preferred size &lt;= DELETION_LIST_MAX_SIZE in both dimensions.</p>
     */
    @Test
    @DisplayName("D-15 dialog size controlled (D7)")
    void buildDeletionList_thousandTitles_sizeBounded() {
        // Arrange
        List<String> titles = IntStream.range(0, 1000)
                .mapToObj(index -> "A rather long document title number " + index)
                .toList();

        // Act
        Dimension size = onEdtGet(() -> SwingDialogService.buildDeletionList(titles).getPreferredSize());

        // Assert
        assertTrue(size.width <= SwingDialogService.DELETION_LIST_MAX_SIZE.width, "width " + size.width);
        assertTrue(size.height <= SwingDialogService.DELETION_LIST_MAX_SIZE.height, "height " + size.height);
    }
}
