package views;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.table.AbstractTableModel;

import models.DocumentSummary;
import views.support.DeletionSelection;

/**
 * Table model of the document library (UC3, D7): columns Title, Preview, Delete.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Columns: {@value #TITLE_COLUMN} "Title" ({@code String}), {@value #PREVIEW_COLUMN}
 *       "Preview" ({@code String}, the precomputed {@link DocumentSummary#preview()}),
 *       {@value #DELETE_COLUMN} "Delete" ({@code Boolean}, rendered as a checkbox by {@code JTable}
 *       because {@link #getColumnClass(int)} returns {@code Boolean.class}).</li>
 *   <li>Rows are in the order given to {@link #setDocuments(List)} - the controller already sorted
 *       them by {@code TITLE_ORDER} (D6). The model never sorts.</li>
 *   <li>Only the Delete column is editable; {@link #setValueAt} on it updates the
 *       {@link DeletionSelection} and fires {@code fireTableCellUpdated}.</li>
 *   <li>{@link #setDocuments(List)} keeps ticks for titles that still exist
 *       ({@code selection.retainOnly}) and fires {@code fireTableDataChanged()}.</li>
 *   <li>The Delete button listens to this model ({@code TableModelListener}) and enables itself
 *       iff {@link #hasSelection()} (D7).</li>
 * </ul>
 *
 * <p><b>Threading:</b> EDT only (Swing model).</p>
 */
public final class DocumentTableModel extends AbstractTableModel {

    /** Index of the Title column. */
    public static final int TITLE_COLUMN = 0;

    /** Index of the Preview column. */
    public static final int PREVIEW_COLUMN = 1;

    /** Index of the Delete (checkbox) column. */
    public static final int DELETE_COLUMN = 2;

    private static final long serialVersionUID = 1L;

    /** Column headers, indexed by column. */
    private static final List<String> COLUMN_NAMES = List.of("Title", "Preview", "Delete");

    /** Current rows, immutable, in display order. */
    private transient List<DocumentSummary> rows = List.of();

    /** Ticked titles; survives refreshes. */
    private final transient DeletionSelection selection = new DeletionSelection();

    /** Creates an empty model. */
    public DocumentTableModel() {
        // empty
    }

    /**
     * Replaces all rows (D-02, D-04, D-05).
     *
     * @param sortedDocuments the new rows, already sorted, not {@code null}
     */
    public void setDocuments(List<DocumentSummary> sortedDocuments) {
        Objects.requireNonNull(sortedDocuments, "sortedDocuments");
        rows = List.copyOf(sortedDocuments);
        selection.retainOnly(rows.stream().map(DocumentSummary::title).collect(Collectors.toSet()));
        fireTableDataChanged();
    }

    /**
     * Returns the summary shown in a row.
     *
     * @param row the row index
     * @return the summary
     * @throws IndexOutOfBoundsException if the row does not exist
     */
    public DocumentSummary summaryAt(int row) {
        return rows.get(row);
    }

    /**
     * Returns the ticked titles in table order (D-06).
     *
     * @return immutable list
     */
    public List<String> selectedTitles() {
        return rows.stream()
                .map(DocumentSummary::title)
                .filter(selection::isSelected)
                .toList();
    }

    /**
     * Tells whether at least one row is ticked (D-06).
     *
     * @return {@code true} if any is ticked
     */
    public boolean hasSelection() {
        return !selection.isEmpty();
    }

    /** Unticks all rows and fires an update. */
    public void clearSelection() {
        if (selection.isEmpty()) {
            return;
        }
        selection.clear();
        if (!rows.isEmpty()) {
            fireTableRowsUpdated(0, rows.size() - 1);
        }
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.size();
    }

    @Override
    public String getColumnName(int column) {
        return COLUMN_NAMES.get(column);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == DELETE_COLUMN ? Boolean.class : String.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return column == DELETE_COLUMN;
    }

    @Override
    public Object getValueAt(int row, int column) {
        DocumentSummary summary = rows.get(row);
        switch (column) {
            case TITLE_COLUMN:
                return summary.title();
            case PREVIEW_COLUMN:
                return summary.preview();
            case DELETE_COLUMN:
                return selection.isSelected(summary.title());
            default:
                throw new IndexOutOfBoundsException("column " + column);
        }
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (column != DELETE_COLUMN) {
            return;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("value must be a Boolean: " + value);
        }
        selection.setSelected(rows.get(row).title(), (Boolean) value);
        fireTableCellUpdated(row, column);
    }
}
