package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import controllers.SearchHandle;
import controllers.SearchListener;
import models.DocumentSearchResult;
import models.SearchQuery;

/**
 * Content of one search tab (U3, U4): drop-down of matching documents on top, reader below.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Layout:</b> {@code BorderLayout}: NORTH = panel with {@link #resultComboBox} (renderer
 * {@link SearchResultCellRenderer}, width follows the tab) and {@link #statusLabel} plus
 * {@link #progressBar}; CENTER = {@link #readerPanel}. Optionally a vertical {@code JSplitPane}.</p>
 *
 * <p><b>States:</b></p>
 * <ul>
 *   <li>Searching (initial): status {@code "Searching..."}, progress bar visible and
 *       indeterminate, combo box disabled (D-35).</li>
 *   <li>{@link #onSearchCompleted} with n &gt; 0 results: progress bar hidden, combo enabled with
 *       the results, status {@code "<n> matching documents"} ({@code "1 matching document"} for
 *       n = 1), reader shows the hint "Select a document above" (D-36).</li>
 *   <li>{@link #onSearchCompleted} with 0 results: status {@code "No matches"}, combo disabled (D-37).</li>
 *   <li>{@link #onSearchFailed}: status {@code "Search failed: <message>"} in red; no dialog (D-38).</li>
 * </ul>
 * <p>Selecting a combo item calls {@code readerPanel.showResult(result, query)} (D-39); selecting
 * nothing clears the reader.</p>
 *
 * <p><b>Cancellation:</b> {@link SearchResultsPane} attaches the handle returned by
 * {@code SearchController.startSearch} via {@link #attachHandle}; {@link #cancelSearch()} calls
 * {@code handle.cancel()} when the tab is closed (U3). Because the controller guarantees no callback
 * after {@code cancel()}, this panel needs no extra "am I closed?" checks.</p>
 *
 * <p><b>Threading:</b> EDT only (listener callbacks are guaranteed on the EDT).</p>
 */
public final class SearchTabPanel extends JPanel implements SearchListener {

    private static final long serialVersionUID = 1L;

    /** Rows shown by the open drop-down before it scrolls. */
    private static final int COMBO_VISIBLE_ROWS = 12;

    /** Gap around and between the parts of the top area in pixels. */
    private static final int GAP = 6;

    /** The query of this tab. */
    private final transient SearchQuery query;

    /** Elements of the drop-down. */
    private final SearchResultComboBoxModel comboModel = new SearchResultComboBoxModel();

    /** Drop-down of matching documents. */
    private final JComboBox<DocumentSearchResult> resultComboBox = new JComboBox<>(comboModel);

    /** "Searching...", result count, "No matches" or failure text. */
    private final JLabel statusLabel = new JLabel("Searching...");

    /** Indeterminate while searching. */
    private final JProgressBar progressBar = new JProgressBar();

    /** Renders the selected document. */
    private final DocumentReaderPanel readerPanel;

    /** Handle of the running search; {@code null} until {@link #attachHandle} (private state only). */
    private transient SearchHandle handle;

    /** The result currently given to the reader, or {@code null} (private state only). */
    private transient DocumentSearchResult shownResult;

    /**
     * Creates a tab in the "Searching..." state.
     *
     * @param query       the query, not {@code null}
     * @param readerPanel the reader for the lower half, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public SearchTabPanel(SearchQuery query, DocumentReaderPanel readerPanel) {
        this.query = Objects.requireNonNull(query, "query");
        this.readerPanel = Objects.requireNonNull(readerPanel, "readerPanel");
        resultComboBox.setRenderer(new SearchResultCellRenderer());
        resultComboBox.setMaximumRowCount(COMBO_VISIBLE_ROWS);
        resultComboBox.setEnabled(false);
        resultComboBox.addActionListener(event -> onSelectionChanged());
        progressBar.setIndeterminate(true);
        progressBar.setVisible(true);

        JPanel statusPanel = new JPanel(new BorderLayout(GAP, 0));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.add(progressBar, BorderLayout.CENTER);
        JPanel northPanel = new JPanel(new BorderLayout(0, GAP));
        northPanel.setBorder(BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP));
        northPanel.add(resultComboBox, BorderLayout.NORTH);
        northPanel.add(statusPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(northPanel, BorderLayout.NORTH);
        add(readerPanel, BorderLayout.CENTER);
    }

    /**
     * Stores the handle of the search shown in this tab.
     *
     * @param searchHandle the handle, not {@code null}
     */
    public void attachHandle(SearchHandle searchHandle) {
        this.handle = Objects.requireNonNull(searchHandle, "searchHandle");
    }

    /** Cancels the search of this tab (idempotent); called when the tab closes (D-41). */
    public void cancelSearch() {
        if (handle != null) {
            handle.cancel();
        }
    }

    /**
     * Returns the query of this tab.
     *
     * @return the query
     */
    public SearchQuery query() {
        return query;
    }

    /** {@inheritDoc} Tests: D-36, D-37. */
    @Override
    public void onSearchCompleted(List<DocumentSearchResult> sortedResults) {
        Objects.requireNonNull(sortedResults, "sortedResults");
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        comboModel.setResults(sortedResults);
        int count = sortedResults.size();
        if (count == 0) {
            statusLabel.setText("No matches");
            resultComboBox.setEnabled(false);
        } else {
            statusLabel.setText(count == 1 ? "1 matching document" : count + " matching documents");
            resultComboBox.setEnabled(true);
        }
        shownResult = null;
        readerPanel.clear();
    }

    /** {@inheritDoc} Test: D-38. */
    @Override
    public void onSearchFailed(String message) {
        Objects.requireNonNull(message, "message");
        progressBar.setIndeterminate(false);
        progressBar.setVisible(false);
        resultComboBox.setEnabled(false);
        statusLabel.setForeground(Color.RED);
        statusLabel.setText("Search failed: " + message);
    }

    /** Shows the selected result in the reader, or clears the reader when nothing is selected. */
    private void onSelectionChanged() {
        Object selected = comboModel.getSelectedItem();
        if (selected == shownResult) {
            return;
        }
        if (selected instanceof DocumentSearchResult) {
            shownResult = (DocumentSearchResult) selected;
            readerPanel.showResult(shownResult, query);
        } else {
            shownResult = null;
            readerPanel.clear();
        }
    }

    /**
     * Test accessor.
     *
     * @return the status label text
     */
    String statusText() {
        return statusLabel.getText();
    }

    /**
     * Test accessor.
     *
     * @return the combo box model
     */
    SearchResultComboBoxModel comboModel() {
        return comboModel;
    }

    /**
     * Test accessor.
     *
     * @return the combo box
     */
    JComboBox<DocumentSearchResult> resultComboBox() {
        return resultComboBox;
    }

    /**
     * Test accessor.
     *
     * @return the progress bar
     */
    JProgressBar progressBar() {
        return progressBar;
    }
}
