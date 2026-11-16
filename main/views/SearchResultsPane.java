package views;

import java.util.Objects;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import controllers.DocumentLibraryController;
import controllers.SearchController;
import models.SearchQuery;

/**
 * Right side of the main window (U1, U3): a {@code JTabbedPane} with one closable tab per search.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>{@link #openSearch(SearchQuery)} (required):</b></p>
 * <ol>
 *   <li>Create {@code reader = new DocumentReaderPanel(new DownloadWorkflow(libraryController, dialogs))}
 *       and {@code tab = new SearchTabPanel(query, reader)}.</li>
 *   <li>Add the tab, install {@code new ClosableTabHeader(TabTitleFormatter.tabTitle(query),
 *       TabTitleFormatter.tooltip(query), () -> closeTab(tab))} with {@code setTabComponentAt},
 *       select it.</li>
 *   <li>{@code tab.attachHandle(searchController.startSearch(query, tab))} - the search runs in the
 *       background; the user can immediately start another search (U3).</li>
 * </ol>
 * <p>When no tab is open, show a centered hint label "Use Search &rarr; Search By... to search"
 * instead of the empty tabbed pane.</p>
 *
 * <p><b>{@link #closeTab(SearchTabPanel)}:</b> {@code tab.cancelSearch()} first, then remove the
 * tab (U3: closing the tab cancels its search).</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class SearchResultsPane extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Starts searches. */
    private final transient SearchController searchController;

    /** Needed by each tab's download workflow. */
    private final transient DocumentLibraryController libraryController;

    /** Dialogs for the download workflow. */
    private final transient DialogService dialogs;

    /** One tab per search. */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /**
     * Creates the pane with no tabs.
     *
     * @param searchController  the search controller, not {@code null}
     * @param libraryController the library controller (downloads), not {@code null}
     * @param dialogs           the dialog service, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public SearchResultsPane(SearchController searchController, DocumentLibraryController libraryController,
                             DialogService dialogs) {
        this.searchController = Objects.requireNonNull(searchController, "searchController");
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
        // TODO: Member D - layout (CardLayout: hint label vs. tabbed pane)
    }

    /**
     * Opens a new tab and starts its search (D-40, D-42).
     *
     * @param query the validated query, not {@code null}
     * @return the new tab
     */
    public SearchTabPanel openSearch(SearchQuery query) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Cancels the tab's search and removes the tab (D-41).
     *
     * @param tab a tab of this pane, not {@code null}
     */
    public void closeTab(SearchTabPanel tab) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Returns the number of open search tabs.
     *
     * @return tab count
     */
    public int tabCount() {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Test accessor.
     *
     * @return the tabbed pane
     */
    JTabbedPane tabbedPane() {
        return tabbedPane;
    }
}
