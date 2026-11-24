package views;

import java.awt.Dimension;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Objects;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

import controllers.DocumentLibraryController;
import controllers.LibraryListener;
import controllers.LifecycleController;
import controllers.SearchController;
import models.Document;
import models.ImportProgress;
import views.support.ImportProgressTracker;

/**
 * The single application window (U1).
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Structure:</b></p>
 * <ul>
 *   <li>Title "Local Search Engine"; default size 1200 x 800, minimum 800 x 500, centered.</li>
 *   <li>Menu bar: <b>File</b> &rarr; "Import..." (Ctrl+O), separator, "Exit"; <b>Search</b> &rarr;
 *       "Search By..." (Ctrl+F).</li>
 *   <li>Content: horizontal {@link JSplitPane} - left {@link DocumentLibraryPanel} (initial divider
 *       ~40%), right {@link SearchResultsPane}; {@code setResizeWeight(0.4)}.</li>
 *   <li>Default close operation {@code DO_NOTHING_ON_CLOSE}; a {@code WindowListener} routes the
 *       close button to {@link #onExitRequested()}.</li>
 * </ul>
 *
 * <p><b>Wiring done in the constructor:</b> build {@code ImportProgressPanel(new ImportProgressTracker())},
 * {@code DocumentLibraryPanel}, {@code SearchResultsPane}; register the library panel, the progress
 * panel and this frame (for {@link #onImportFailed}, {@link #onExportCompleted},
 * {@link #onExportFailed}) as {@link LibraryListener}s; fill the table once from
 * {@code libraryController.currentDocuments()}. If {@code dialogs} is a {@link SwingDialogService},
 * call {@code setParent(this)}.</p>
 *
 * <p><b>Menu actions:</b> Import &rarr; {@link #onImportRequested()} = {@code importWorkflow.run()};
 * Search By &rarr; {@link #onSearchRequested()} = {@code searchQueryWorkflow.promptForQuery()
 * .ifPresent(searchResultsPane::openSearch)}; Exit &rarr; {@link #onExitRequested()} =
 * {@code lifecycleController.shutdown(); dispose();} (the JVM exits when the worker threads end,
 * within {@code AppConstants.SHUTDOWN_GRACE_MILLIS}).</p>
 *
 * <p><b>Dialogs owned here:</b> {@code onImportFailed} &rarr; {@code showError("Import failed",
 * "Could not import \"<title>\": <message>")}; {@code onExportFailed} &rarr;
 * {@code showError("Download failed", message)}; {@code onExportCompleted} &rarr;
 * {@code showInfo("Download complete", "Saved \"<title>\" to <path>")}.</p>
 *
 * <p><b>Threading:</b> EDT only. Must be constructed on the EDT. This is the only {@code JFrame}
 * of the application.</p>
 */
public final class MainFrame extends JFrame implements LibraryListener {

    private static final long serialVersionUID = 1L;

    /** Initial window size. */
    private static final Dimension DEFAULT_SIZE = new Dimension(1200, 800);

    /** Minimum window size. */
    private static final Dimension MINIMUM_SIZE = new Dimension(800, 500);

    /** Share of extra width given to the left side; also the initial divider position. */
    private static final double SPLIT_RESIZE_WEIGHT = 0.4;

    /** Library use cases. */
    private final transient DocumentLibraryController libraryController;

    /** Search use cases. */
    private final transient SearchController searchController;

    /** Shutdown. */
    private final transient LifecycleController lifecycleController;

    /** All dialogs. */
    private final transient DialogService dialogs;

    /** File &rarr; Import... */
    private final JMenuItem importMenuItem = new JMenuItem("Import...");

    /** File &rarr; Exit */
    private final JMenuItem exitMenuItem = new JMenuItem("Exit");

    /** Search &rarr; Search By... */
    private final JMenuItem searchMenuItem = new JMenuItem("Search By...");

    /** Left/right split. */
    private final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

    /** Left side. */
    private final DocumentLibraryPanel libraryPanel;

    /** Right side. */
    private final SearchResultsPane searchResultsPane;

    /** File &rarr; Import... */
    private final transient ImportWorkflow importWorkflow;

    /** Search &rarr; Search By... */
    private final transient SearchQueryWorkflow searchQueryWorkflow;

    /**
     * Builds the window (not yet visible).
     *
     * @param libraryController   the library controller, not {@code null}
     * @param searchController    the search controller, not {@code null}
     * @param lifecycleController the lifecycle controller, not {@code null}
     * @param dialogs             the dialog service, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public MainFrame(DocumentLibraryController libraryController, SearchController searchController,
                     LifecycleController lifecycleController, DialogService dialogs) {
        super("Local Search Engine");
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.searchController = Objects.requireNonNull(searchController, "searchController");
        this.lifecycleController = Objects.requireNonNull(lifecycleController, "lifecycleController");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        if (dialogs instanceof SwingDialogService) {
            ((SwingDialogService) dialogs).setParent(this);
        }
        ImportProgressPanel importProgressPanel = new ImportProgressPanel(new ImportProgressTracker());
        libraryPanel = new DocumentLibraryPanel(libraryController, dialogs, importProgressPanel);
        searchResultsPane = new SearchResultsPane(searchController, libraryController, dialogs);
        importWorkflow = new ImportWorkflow(libraryController, dialogs);
        searchQueryWorkflow = new SearchQueryWorkflow(dialogs);

        buildMenus();
        splitPane.setLeftComponent(libraryPanel);
        splitPane.setRightComponent(searchResultsPane);
        splitPane.setResizeWeight(SPLIT_RESIZE_WEIGHT);
        splitPane.setDividerLocation((int) (DEFAULT_SIZE.width * SPLIT_RESIZE_WEIGHT));
        setContentPane(splitPane);

        libraryController.addLibraryListener(libraryPanel);
        libraryController.addLibraryListener(importProgressPanel);
        libraryController.addLibraryListener(this);
        libraryPanel.onDocumentsChanged(libraryController.currentDocuments());

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                onExitRequested();
            }
        });
        setSize(DEFAULT_SIZE);
        setMinimumSize(MINIMUM_SIZE);
        setLocationRelativeTo(null);
    }

    /** File &rarr; Import... (UC1). */
    void onImportRequested() {
        importWorkflow.run();
    }

    /** Search &rarr; Search By... (UC2). */
    void onSearchRequested() {
        searchQueryWorkflow.promptForQuery().ifPresent(searchResultsPane::openSearch);
    }

    /** File &rarr; Exit and window close (D-49). */
    void onExitRequested() {
        lifecycleController.shutdown();
        dispose();
    }

    /** {@inheritDoc} */
    @Override
    public void onImportFailed(ImportProgress failed) {
        Objects.requireNonNull(failed, "failed");
        dialogs.showError("Import failed", "Could not import \"" + failed.title() + "\": "
                + failed.errorMessage().orElse("unknown error"));
    }

    /** {@inheritDoc} */
    @Override
    public void onExportCompleted(Document document, Path target) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        dialogs.showInfo("Download complete", "Saved \"" + document.title() + "\" to " + target);
    }

    /** {@inheritDoc} */
    @Override
    public void onExportFailed(Document document, Path target, String message) {
        Objects.requireNonNull(message, "message");
        dialogs.showError("Download failed", message);
    }

    /**
     * Test accessor (D-48).
     *
     * @return the split pane
     */
    JSplitPane splitPane() {
        return splitPane;
    }

    /**
     * Test accessor (D-48).
     *
     * @return the library panel
     */
    DocumentLibraryPanel libraryPanel() {
        return libraryPanel;
    }

    /**
     * Test accessor (D-48).
     *
     * @return the search results pane
     */
    SearchResultsPane searchResultsPane() {
        return searchResultsPane;
    }

    /** Builds File and Search menus with accelerators. */
    private void buildMenus() {
        importMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        importMenuItem.addActionListener(event -> onImportRequested());
        exitMenuItem.addActionListener(event -> onExitRequested());
        searchMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));
        searchMenuItem.addActionListener(event -> onSearchRequested());

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(importMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);
        JMenu searchMenu = new JMenu("Search");
        searchMenu.setMnemonic(KeyEvent.VK_S);
        searchMenu.add(searchMenuItem);
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(fileMenu);
        menuBar.add(searchMenu);
        setJMenuBar(menuBar);
    }
}
