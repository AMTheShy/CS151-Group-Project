package views;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import controllers.DocumentLibraryController;
import controllers.LibraryListener;
import models.DocumentSummary;
import models.LoadSummary;

/**
 * Left side of the main window (UC3, D5, D7, U1, U2): the document table, the Delete button under
 * it, the startup-load status line, and the {@link ImportProgressPanel}.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Layout:</b> {@code BorderLayout}: NORTH = {@link #startupStatusLabel}; CENTER =
 * {@code JScrollPane(documentTable)}; SOUTH = a panel with the Delete button (right-aligned) above
 * the import progress panel.</p>
 *
 * <p><b>Table:</b> {@code JTable(tableModel)}; Title column ~200 px, Preview takes the rest, Delete
 * ~60 px; row sorting disabled (the order comes from the controller, D6); the Preview cell renderer
 * sets the tooltip to the preview text.</p>
 *
 * <p><b>Delete (D7):</b> {@link #deleteButton} is enabled iff {@code tableModel.hasSelection()}
 * (update it from a {@code TableModelListener}). On click: {@code titles = tableModel.selectedTitles()};
 * if {@code dialogs.confirmDelete(titles)} then {@code libraryController.deleteDocuments(titles)} and
 * {@code tableModel.clearSelection()}. Deletion happens in the background; the table updates when
 * {@code onDocumentsChanged} arrives.</p>
 *
 * <p><b>Listener callbacks</b> (registered by {@link MainFrame}): {@code onDocumentsChanged} &rarr;
 * {@code tableModel.setDocuments}; {@code onStartupLoadProgress(k, n)} &rarr; label
 * {@code "Loading saved documents... k / n"}; {@code onStartupLoadCompleted} &rarr; label
 * {@code "n documents"} (and if {@code summary.hasProblems()}, append
 * {@code " (m could not be loaded)"} with the problems as tooltip); after the startup load has
 * ended, every {@code onDocumentsChanged} refreshes the count from the table; {@code onStartupLoadFailed}
 * &rarr; {@code dialogs.showError("Loading saved documents failed", message)};
 * {@code onDeleteFailed} &rarr; {@code dialogs.showError("Delete failed", message)}.</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class DocumentLibraryPanel extends JPanel implements LibraryListener {

    private static final long serialVersionUID = 1L;

    /** Preferred width of the Title column in pixels. */
    private static final int TITLE_COLUMN_WIDTH = 200;

    /** Width of the Delete column in pixels. */
    private static final int DELETE_COLUMN_WIDTH = 60;

    /** Vertical gap between the parts of the panel in pixels. */
    private static final int LAYOUT_GAP = 4;

    /** Starts deletions; queried for nothing else. */
    private final transient DocumentLibraryController libraryController;

    /** Confirmation and error dialogs. */
    private final transient DialogService dialogs;

    /** Rows of the table. */
    private final DocumentTableModel tableModel = new DocumentTableModel();

    /** The document table. */
    private final JTable documentTable = new JTable(tableModel);

    /** Deletes ticked documents; disabled when nothing is ticked. */
    private final JButton deleteButton = new JButton("Delete");

    /** Startup load status / document count. */
    private final JLabel startupStatusLabel = new JLabel(" ");

    /** Import progress rows. */
    private final ImportProgressPanel importProgressPanel;

    /** Problems reported by the startup load; shown as suffix and tooltip of the status line. */
    private transient List<String> loadProblems = List.of();

    /** Whether the startup load has ended; from then on the status line shows the document count. */
    private boolean startupLoadFinished;

    /**
     * Creates the panel.
     *
     * @param libraryController   the library controller, not {@code null}
     * @param dialogs             the dialog service, not {@code null}
     * @param importProgressPanel the progress area, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public DocumentLibraryPanel(DocumentLibraryController libraryController, DialogService dialogs,
                                ImportProgressPanel importProgressPanel) {
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        this.importProgressPanel = Objects.requireNonNull(importProgressPanel, "importProgressPanel");
        configureTable();
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(event -> onDeleteClicked());
        tableModel.addTableModelListener(event -> deleteButton.setEnabled(tableModel.hasSelection()));

        setLayout(new BorderLayout(0, LAYOUT_GAP));
        add(startupStatusLabel, BorderLayout.NORTH);
        add(new JScrollPane(documentTable), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.add(deleteButton);
        JPanel southPanel = new JPanel(new BorderLayout(0, LAYOUT_GAP));
        southPanel.add(buttonPanel, BorderLayout.NORTH);
        southPanel.add(importProgressPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    /** {@inheritDoc} Test: D-30. */
    @Override
    public void onDocumentsChanged(List<DocumentSummary> sortedDocuments) {
        Objects.requireNonNull(sortedDocuments, "sortedDocuments");
        tableModel.setDocuments(sortedDocuments);
        if (startupLoadFinished) {
            showDocumentCount(tableModel.getRowCount());
        }
    }

    /** {@inheritDoc} Test: D-31. */
    @Override
    public void onStartupLoadProgress(int processedCount, int totalCount) {
        startupStatusLabel.setText("Loading saved documents... " + processedCount + " / " + totalCount);
    }

    /** {@inheritDoc} Test: D-31. */
    @Override
    public void onStartupLoadCompleted(LoadSummary summary) {
        Objects.requireNonNull(summary, "summary");
        startupLoadFinished = true;
        loadProblems = summary.problems();
        showDocumentCount(summary.loadedCount());
    }

    /** {@inheritDoc} */
    @Override
    public void onStartupLoadFailed(String message) {
        Objects.requireNonNull(message, "message");
        startupLoadFinished = true;
        showDocumentCount(tableModel.getRowCount());
        dialogs.showError("Loading saved documents failed", message);
    }

    /** {@inheritDoc} */
    @Override
    public void onDeleteFailed(String message) {
        Objects.requireNonNull(message, "message");
        dialogs.showError("Delete failed", message);
    }

    /** Handles a click on the Delete button (D-29). */
    void onDeleteClicked() {
        List<String> titles = tableModel.selectedTitles();
        if (titles.isEmpty()) {
            return;
        }
        if (dialogs.confirmDelete(titles)) {
            libraryController.deleteDocuments(titles);
            tableModel.clearSelection();
        }
    }

    /**
     * Shows {@code "n documents"} plus the startup problems suffix, if any.
     *
     * @param count the number of documents
     */
    private void showDocumentCount(int count) {
        String text = count == 1 ? "1 document" : count + " documents";
        if (loadProblems.isEmpty()) {
            startupStatusLabel.setText(text);
            startupStatusLabel.setToolTipText(null);
            return;
        }
        startupStatusLabel.setText(text + " (" + loadProblems.size() + " could not be loaded)");
        StringBuilder tooltip = new StringBuilder("<html>");
        for (String problem : loadProblems) {
            tooltip.append(SearchResultCellRenderer.escapeHtml(problem)).append("<br>");
        }
        startupStatusLabel.setToolTipText(tooltip.append("</html>").toString());
    }

    /** Configures columns, sorting and renderers of the document table. */
    private void configureTable() {
        documentTable.setAutoCreateRowSorter(false);
        documentTable.setRowSorter(null);
        documentTable.setFillsViewportHeight(true);
        documentTable.getTableHeader().setReorderingAllowed(false);
        documentTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        TableColumn titleColumn = documentTable.getColumnModel().getColumn(DocumentTableModel.TITLE_COLUMN);
        titleColumn.setPreferredWidth(TITLE_COLUMN_WIDTH);
        TableColumn deleteColumn = documentTable.getColumnModel().getColumn(DocumentTableModel.DELETE_COLUMN);
        deleteColumn.setPreferredWidth(DELETE_COLUMN_WIDTH);
        deleteColumn.setMaxWidth(DELETE_COLUMN_WIDTH);
        documentTable.getColumnModel().getColumn(DocumentTableModel.PREVIEW_COLUMN)
                .setCellRenderer(new DefaultTableCellRenderer() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value,
                                                                   boolean isSelected, boolean hasFocus,
                                                                   int row, int column) {
                        Component cell = super.getTableCellRendererComponent(table, value, isSelected,
                                hasFocus, row, column);
                        setToolTipText(value == null ? null : value.toString());
                        return cell;
                    }
                });
    }

    /**
     * Test accessor.
     *
     * @return the table model
     */
    DocumentTableModel tableModel() {
        return tableModel;
    }

    /**
     * Test accessor.
     *
     * @return the Delete button
     */
    JButton deleteButton() {
        return deleteButton;
    }

    /**
     * Test accessor.
     *
     * @return the status label
     */
    JLabel startupStatusLabel() {
        return startupStatusLabel;
    }
}
