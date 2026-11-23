package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;

import controllers.LibraryListener;
import models.AppConstants;
import models.ImportProgress;
import models.ImportState;
import views.support.ImportProgressTracker;

/**
 * The "uploads" area at the bottom of the left side (U2): one row per running or recently
 * finished import, each with the title, a {@code JProgressBar} (0-100, from
 * {@link ImportProgress#percentComplete()}) and the status text.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Implements {@link LibraryListener#onImportProgress}: forwards to the
 *       {@link ImportProgressTracker}; if rows changed, creates/updates/removes row components
 *       (only the affected row - do not rebuild all rows).</li>
 *   <li>QUEUED: progress bar indeterminate. READING: determinate percentage. SAVING: indeterminate.</li>
 *   <li>COMPLETED/CANCELLED: start a one-shot {@code javax.swing.Timer}
 *       ({@code AppConstants.COMPLETED_IMPORT_ROW_LINGER_MILLIS}) that removes the row.</li>
 *   <li>FAILED: row foreground red, status {@code "Failed: <message>"} (tooltip = full message),
 *       plus a small "Dismiss" button that removes the row. The error <em>dialog</em> is shown by
 *       {@link MainFrame} (it listens to {@code onImportFailed}), not here.</li>
 *   <li>The panel is hidden ({@code setVisible(false)}) when there are no rows.</li>
 * </ul>
 *
 * <p><b>Threading:</b> EDT only (callbacks are guaranteed on the EDT).</p>
 */
public final class ImportProgressPanel extends JPanel implements LibraryListener {

    private static final long serialVersionUID = 1L;

    /** Maximum value of the progress bars. */
    private static final int PERCENT_MAX = 100;

    /** Preferred width of the status text in pixels; longer failure texts end with "..." (tooltip = full). */
    private static final int STATUS_WIDTH = 160;

    /** Horizontal gap inside a row in pixels. */
    private static final int ROW_GAP = 6;

    /** Row state (pure logic, unit-tested separately). */
    private final transient ImportProgressTracker tracker;

    /** Row widgets per import id, ascending by id. */
    private final transient TreeMap<Long, RowWidgets> rowWidgets = new TreeMap<>();

    /**
     * Creates the panel.
     *
     * @param tracker the row model, not {@code null}
     * @throws NullPointerException if {@code tracker} is {@code null}
     */
    public ImportProgressPanel(ImportProgressTracker tracker) {
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createTitledBorder("Imports"));
        setVisible(false);
    }

    /** {@inheritDoc} Test: D-32. */
    @Override
    public void onImportProgress(ImportProgress progress) {
        Objects.requireNonNull(progress, "progress");
        long importId = progress.importId();
        if (!tracker.apply(progress)) {
            return;
        }
        ImportProgress row = tracker.row(importId).orElseThrow();
        RowWidgets widgets = rowWidgets.get(importId);
        if (widgets == null) {
            widgets = createRow(row);
            rowWidgets.put(importId, widgets);
            add(widgets.rowPanel(), indexOfNewRow(importId));
        }
        updateRow(widgets, row);
        if (row.state() == ImportState.COMPLETED || row.state() == ImportState.CANCELLED) {
            Timer lingerTimer = new Timer(AppConstants.COMPLETED_IMPORT_ROW_LINGER_MILLIS,
                    event -> removeRow(importId));
            lingerTimer.setRepeats(false);
            lingerTimer.start();
        }
        refreshVisibility();
    }

    /**
     * Returns the number of visible rows (test accessor, D-32).
     *
     * @return row count
     */
    int visibleRowCount() {
        return rowWidgets.size();
    }

    /**
     * Returns the status text of a row (test accessor, D-32).
     *
     * @param importId the import id
     * @return the status text, or empty if there is no such row
     */
    Optional<String> statusTextOf(long importId) {
        return Optional.ofNullable(rowWidgets.get(importId)).map(widgets -> widgets.statusLabel().getText());
    }

    /**
     * Creates the widgets of a new row.
     *
     * @param progress the first progress of the import
     * @return the row widgets
     */
    private RowWidgets createRow(ImportProgress progress) {
        JLabel titleLabel = new JLabel(progress.title());
        titleLabel.setToolTipText(progress.title());
        titleLabel.setMinimumSize(new Dimension(0, titleLabel.getPreferredSize().height));
        JProgressBar progressBar = new JProgressBar(0, PERCENT_MAX);
        JLabel statusLabel = new JLabel(" ");
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        statusLabel.setPreferredSize(new Dimension(STATUS_WIDTH, statusLabel.getPreferredSize().height));
        JButton dismissButton = new JButton("Dismiss");
        dismissButton.setVisible(false);
        dismissButton.setFocusable(false);
        long importId = progress.importId();
        dismissButton.addActionListener(event -> removeRow(importId));
        JPanel textPanel = new JPanel(new BorderLayout(ROW_GAP, 0));
        textPanel.add(titleLabel, BorderLayout.CENTER);
        textPanel.add(statusLabel, BorderLayout.EAST);
        JPanel barPanel = new JPanel(new BorderLayout(ROW_GAP, 0));
        barPanel.add(progressBar, BorderLayout.CENTER);
        barPanel.add(dismissButton, BorderLayout.EAST);
        JPanel rowPanel = new JPanel(new BorderLayout(0, ROW_GAP / 2));
        rowPanel.setBorder(BorderFactory.createEmptyBorder(ROW_GAP / 2, 0, ROW_GAP / 2, 0));
        rowPanel.add(textPanel, BorderLayout.NORTH);
        rowPanel.add(barPanel, BorderLayout.CENTER);
        rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return new RowWidgets(rowPanel, statusLabel, progressBar, dismissButton);
    }

    /**
     * Updates an existing row.
     *
     * @param widgets  the row widgets
     * @param progress the latest progress
     */
    private void updateRow(RowWidgets widgets, ImportProgress progress) {
        ImportState state = progress.state();
        String statusText = ImportProgressTracker.statusText(progress);
        widgets.statusLabel().setText(statusText);
        widgets.statusLabel().setToolTipText(progress.errorMessage().orElse(statusText));
        JProgressBar progressBar = widgets.progressBar();
        progressBar.setIndeterminate(state == ImportState.QUEUED || state == ImportState.SAVING);
        if (state == ImportState.READING || state == ImportState.COMPLETED) {
            progressBar.setValue(progress.percentComplete());
        }
        boolean failed = state == ImportState.FAILED;
        widgets.statusLabel().setForeground(failed ? Color.RED : UIManager.getColor("Label.foreground"));
        widgets.dismissButton().setVisible(failed);
    }

    /**
     * Removes a row from the tracker and the panel (linger timer or Dismiss button).
     *
     * @param importId the import id
     */
    private void removeRow(long importId) {
        tracker.remove(importId);
        RowWidgets widgets = rowWidgets.remove(importId);
        if (widgets != null) {
            remove(widgets.rowPanel());
            refreshVisibility();
        }
    }

    /**
     * Returns the child index for a new row so that rows stay ordered by import id.
     *
     * @param importId the new id
     * @return the index
     */
    private int indexOfNewRow(long importId) {
        return rowWidgets.headMap(importId).size();
    }

    /** Hides the panel when empty and relayouts it. */
    private void refreshVisibility() {
        setVisible(!rowWidgets.isEmpty());
        revalidate();
        repaint();
    }

    /**
     * The widgets of one progress row.
     *
     * @param rowPanel      the row container
     * @param statusLabel   the status text
     * @param progressBar   the progress bar
     * @param dismissButton removes a FAILED row
     */
    private record RowWidgets(JPanel rowPanel, JLabel statusLabel, JProgressBar progressBar,
                              JButton dismissButton) {
    }
}
