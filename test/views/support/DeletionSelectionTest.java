package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code DeletionSelection}. Owner: Member D.
 *
 * <p>Cases D-10..D-13 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DeletionSelectionTest {

    private DeletionSelection selection;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        selection = new DeletionSelection();
    }

    /**
     * D-10: ticking [unit].
     *
     * <p>Arrange: toggle('A') twice; setSelected('B', true).</p>
     * <p>Act: setSelected / toggle / isSelected.</p>
     * <p>Assert: A false after two toggles; B true.</p>
     */
    @Test
    @DisplayName("D-10 ticking")
    void toggle_title_flipsState() {
        // Act
        boolean afterFirstToggle = selection.toggle("A");
        boolean afterSecondToggle = selection.toggle("A");
        selection.setSelected("B", true);

        // Assert
        assertTrue(afterFirstToggle);
        assertFalse(afterSecondToggle);
        assertFalse(selection.isSelected("A"));
        assertTrue(selection.isSelected("B"));
    }

    /**
     * D-11: refresh after deletion [unit].
     *
     * <p>Arrange: select A, B, C; retainOnly([A, C, D]).</p>
     * <p>Act: retainOnly.</p>
     * <p>Assert: selected [A, C].</p>
     */
    @Test
    @DisplayName("D-11 refresh after deletion")
    void retainOnly_existingTitles_dropsOthers() {
        // Arrange
        selection.setSelected("A", true);
        selection.setSelected("B", true);
        selection.setSelected("C", true);

        // Act
        selection.retainOnly(List.of("A", "C", "D"));

        // Assert
        assertEquals(List.of("A", "C"), selection.selectedTitles());
    }

    /**
     * D-12: stable order (D6) [unit].
     *
     * <p>Arrange: select 'b', 'A', 'a'.</p>
     * <p>Act: selectedTitles.</p>
     * <p>Assert: [A, a, b]; add() throws UnsupportedOperationException.</p>
     */
    @Test
    @DisplayName("D-12 stable order (D6)")
    void selectedTitles_mixedCase_sortedAndImmutable() {
        // Arrange
        selection.setSelected("b", true);
        selection.setSelected("A", true);
        selection.setSelected("a", true);

        // Act
        List<String> titles = selection.selectedTitles();

        // Assert
        assertEquals(List.of("A", "a", "b"), titles);
        assertThrows(UnsupportedOperationException.class, () -> titles.add("c"));
    }

    /**
     * D-13: Delete button disabled when empty (D7) [unit].
     *
     * <p>Arrange: select A; clear().</p>
     * <p>Act: isEmpty / clear.</p>
     * <p>Assert: isEmpty true.</p>
     */
    @Test
    @DisplayName("D-13 Delete button disabled when empty (D7)")
    void clear_afterSelections_isEmpty() {
        // Arrange
        selection.setSelected("A", true);

        // Act
        selection.clear();

        // Assert
        assertTrue(selection.isEmpty());
    }
}
