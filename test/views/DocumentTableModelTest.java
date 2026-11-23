package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.summaries;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TableModelEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.DocumentSummary;

/**
 * Tests for {@code DocumentTableModel}. Owner: Member D.
 *
 * <p>Cases D-01..D-06 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DocumentTableModelTest {

    private DocumentTableModel model;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        model = new DocumentTableModel();
    }

    /**
     * D-01: three columns (D7) [unit].
     *
     * <p>Arrange: new model.</p>
     * <p>Act: getColumnName / getColumnClass.</p>
     * <p>Assert: names Title, Preview, Delete; classes String, String, Boolean; count 3.</p>
     */
    @Test
    @DisplayName("D-01 three columns (D7)")
    void columns_threeColumns_namesAndTypes() {
        // Act + Assert
        assertEquals(3, model.getColumnCount());
        assertEquals(List.of("Title", "Preview", "Delete"),
                List.of(model.getColumnName(0), model.getColumnName(1), model.getColumnName(2)));
        assertEquals(String.class, model.getColumnClass(DocumentTableModel.TITLE_COLUMN));
        assertEquals(String.class, model.getColumnClass(DocumentTableModel.PREVIEW_COLUMN));
        assertEquals(Boolean.class, model.getColumnClass(DocumentTableModel.DELETE_COLUMN));
    }

    /**
     * D-02: rows mirror controller order (D6) [unit].
     *
     * <p>Arrange: 3 DocumentSummary in TITLE_ORDER.</p>
     * <p>Act: setDocuments / getValueAt.</p>
     * <p>Assert: rowCount 3; title/preview values per row; Delete false.</p>
     */
    @Test
    @DisplayName("D-02 rows mirror controller order (D6)")
    void setDocuments_summaries_rowsInGivenOrder() {
        // Arrange
        List<DocumentSummary> rows = summaries("A", "a", "b");

        // Act
        model.setDocuments(rows);

        // Assert
        assertEquals(3, model.getRowCount());
        for (int row = 0; row < rows.size(); row++) {
            assertEquals(rows.get(row).title(), model.getValueAt(row, DocumentTableModel.TITLE_COLUMN));
            assertEquals(rows.get(row).preview(), model.getValueAt(row, DocumentTableModel.PREVIEW_COLUMN));
            assertEquals(Boolean.FALSE, model.getValueAt(row, DocumentTableModel.DELETE_COLUMN));
            assertSame(rows.get(row), model.summaryAt(row));
        }
    }

    /**
     * D-03: only Delete editable [unit].
     *
     * <p>Arrange: setValueAt(true, 1, DELETE_COLUMN); try editing Title.</p>
     * <p>Act: isCellEditable / setValueAt.</p>
     * <p>Assert: Delete cell true; isCellEditable false for Title/Preview.</p>
     */
    @Test
    @DisplayName("D-03 only Delete editable")
    void setValueAt_deleteColumn_togglesSelection() {
        // Arrange
        model.setDocuments(summaries("A", "B", "C"));

        // Act
        model.setValueAt(true, 1, DocumentTableModel.DELETE_COLUMN);
        model.setValueAt("changed", 1, DocumentTableModel.TITLE_COLUMN);

        // Assert
        assertEquals(Boolean.TRUE, model.getValueAt(1, DocumentTableModel.DELETE_COLUMN));
        assertEquals("B", model.getValueAt(1, DocumentTableModel.TITLE_COLUMN));
        assertTrue(model.isCellEditable(1, DocumentTableModel.DELETE_COLUMN));
        assertFalse(model.isCellEditable(1, DocumentTableModel.TITLE_COLUMN));
        assertFalse(model.isCellEditable(1, DocumentTableModel.PREVIEW_COLUMN));
    }

    /**
     * D-04: selection survives refresh [unit].
     *
     * <p>Arrange: tick A and B; setDocuments without B.</p>
     * <p>Act: setDocuments.</p>
     * <p>Assert: A still ticked; B gone; selectedTitles [A].</p>
     */
    @Test
    @DisplayName("D-04 selection survives refresh")
    void setDocuments_refresh_keepsTicksOfRemainingTitles() {
        // Arrange
        model.setDocuments(summaries("A", "B", "C"));
        model.setValueAt(true, 0, DocumentTableModel.DELETE_COLUMN);
        model.setValueAt(true, 1, DocumentTableModel.DELETE_COLUMN);

        // Act
        model.setDocuments(summaries("A", "C"));

        // Assert
        assertEquals(Boolean.TRUE, model.getValueAt(0, DocumentTableModel.DELETE_COLUMN));
        assertEquals(Boolean.FALSE, model.getValueAt(1, DocumentTableModel.DELETE_COLUMN));
        assertEquals(List.of("A"), model.selectedTitles());
    }

    /**
     * D-05: Swing events for JTable [unit].
     *
     * <p>Arrange: TableModelListener attached.</p>
     * <p>Act: events.</p>
     * <p>Assert: setDocuments fires data-changed; setValueAt fires cell-updated for (row, 2).</p>
     */
    @Test
    @DisplayName("D-05 Swing events for JTable")
    void setDocumentsAndSetValueAt_fireTableModelEvents() {
        // Arrange
        List<TableModelEvent> events = new ArrayList<>();
        model.addTableModelListener(events::add);

        // Act
        model.setDocuments(summaries("A", "B"));
        model.setValueAt(true, 1, DocumentTableModel.DELETE_COLUMN);

        // Assert
        assertEquals(2, events.size());
        TableModelEvent dataChanged = events.get(0);
        assertEquals(TableModelEvent.UPDATE, dataChanged.getType());
        assertEquals(0, dataChanged.getFirstRow());
        assertEquals(Integer.MAX_VALUE, dataChanged.getLastRow());
        assertEquals(TableModelEvent.ALL_COLUMNS, dataChanged.getColumn());
        TableModelEvent cellUpdated = events.get(1);
        assertEquals(1, cellUpdated.getFirstRow());
        assertEquals(1, cellUpdated.getLastRow());
        assertEquals(DocumentTableModel.DELETE_COLUMN, cellUpdated.getColumn());
    }

    /**
     * D-06: Delete button state source [unit].
     *
     * <p>Arrange: tick rows 2 and 0; clearSelection.</p>
     * <p>Act: selectedTitles / hasSelection / clearSelection.</p>
     * <p>Assert: selectedTitles in table order; hasSelection true then false.</p>
     */
    @Test
    @DisplayName("D-06 Delete button state source")
    void selectedTitles_ticks_returnedInTableOrder() {
        // Arrange
        model.setDocuments(summaries("A", "b", "C"));

        // Act
        model.setValueAt(true, 2, DocumentTableModel.DELETE_COLUMN);
        model.setValueAt(true, 0, DocumentTableModel.DELETE_COLUMN);

        // Assert
        assertEquals(List.of("A", "C"), model.selectedTitles());
        assertTrue(model.hasSelection());
        model.clearSelection();
        assertFalse(model.hasSelection());
        assertEquals(List.of(), model.selectedTitles());
    }
}
