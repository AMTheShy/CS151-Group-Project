package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static views.SwingTestSupport.result;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.DocumentSearchResult;

/**
 * Tests for {@code SearchResultComboBoxModel}. Owner: Member D.
 *
 * <p>Cases D-07..D-09 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchResultComboBoxModelTest {

    private SearchResultComboBoxModel model;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        model = new SearchResultComboBoxModel();
    }

    /**
     * D-07: only matching docs listed (U4) [unit].
     *
     * <p>Arrange: 2 DocumentSearchResult.</p>
     * <p>Act: setResults / getElementAt.</p>
     * <p>Assert: getSize 2; elements in order; getSelectedItem null.</p>
     */
    @Test
    @DisplayName("D-07 only matching docs listed (U4)")
    void setResults_twoResults_exposedInOrderWithoutSelection() {
        // Arrange
        DocumentSearchResult first = result("A", "x needle y", "needle");
        DocumentSearchResult second = result("b", "needle", "needle");

        // Act
        model.setResults(List.of(first, second));

        // Assert
        assertEquals(2, model.getSize());
        assertSame(first, model.getElementAt(0));
        assertSame(second, model.getElementAt(1));
        assertNull(model.getSelectedItem());
    }

    /**
     * D-08: selection events [unit].
     *
     * <p>Arrange: ListDataListener; select element 1; select foreign object.</p>
     * <p>Act: setSelectedItem.</p>
     * <p>Assert: selected element 1; contentsChanged fired; foreign object ignored.</p>
     */
    @Test
    @DisplayName("D-08 selection events")
    void setSelectedItem_element_selectsAndNotifies() {
        // Arrange
        DocumentSearchResult first = result("A", "needle", "needle");
        DocumentSearchResult second = result("B", "needle", "needle");
        model.setResults(List.of(first, second));
        List<ListDataEvent> events = new ArrayList<>();
        model.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent event) {
                events.add(event);
            }

            @Override
            public void intervalRemoved(ListDataEvent event) {
                events.add(event);
            }

            @Override
            public void contentsChanged(ListDataEvent event) {
                events.add(event);
            }
        });

        // Act
        model.setSelectedItem(second);
        model.setSelectedItem("foreign object");

        // Assert
        assertSame(second, model.getSelectedItem());
        assertEquals(1, events.size());
        assertEquals(ListDataEvent.CONTENTS_CHANGED, events.get(0).getType());
    }

    /**
     * D-09: zero results [unit].
     *
     * <p>Arrange: results then empty list.</p>
     * <p>Act: setResults.</p>
     * <p>Assert: getSize 0; getSelectedItem null.</p>
     */
    @Test
    @DisplayName("D-09 zero results")
    void setResults_emptyAfterSelection_clearsSelection() {
        // Arrange
        DocumentSearchResult first = result("A", "needle", "needle");
        model.setResults(List.of(first));
        model.setSelectedItem(first);

        // Act
        model.setResults(List.of());

        // Assert
        assertEquals(0, model.getSize());
        assertNull(model.getSelectedItem());
    }
}
