package views;

import java.awt.CardLayout;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import controllers.DocumentLibraryController;
import controllers.SearchController;
import models.SearchQuery;
import views.support.TabTitleFormatter;

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

    /** Hint shown while no tab is open. */
    static final String HINT_TEXT = "Use Search \u2192 Search By... to search";

    /** Card name of the hint. */
    private static final String HINT_CARD = "hint";

    /** Card name of the tabbed pane. */
    private static final String TABS_CARD = "tabs";

    /** Starts searches. */
    private final transient SearchController searchController;

    /** Needed by each tab's download workflow. */
    private final transient DocumentLibraryController libraryController;

    /** Dialogs for the download workflow. */
    private final transient DialogService dialogs;

    /** One tab per search. */
    private final JTabbedPane tabbedPane = new JTabbedPane();

    /** Switches between the hint and the tabbed pane. */
    private final CardLayout cardLayout = new CardLayout();

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
        setLayout(cardLayout);
        add(new JLabel(HINT_TEXT, SwingConstants.CENTER), HINT_CARD);
        add(tabbedPane, TABS_CARD);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        showCard();
    }

    /**
     * Opens a new tab and starts its search (D-40, D-42).
     *
     * @param query the validated query, not {@code null}
     * @return the new tab
     */
    public SearchTabPanel openSearch(SearchQuery query) {
        Objects.requireNonNull(query, "query");
        DocumentReaderPanel reader = new DocumentReaderPanel(new DownloadWorkflow(libraryController, dialogs));
        SearchTabPanel tab = new SearchTabPanel(query, reader);
        tabbedPane.addTab(TabTitleFormatter.tabTitle(query), tab);
        int index = tabbedPane.indexOfComponent(tab);
        tabbedPane.setTabComponentAt(index, new ClosableTabHeader(TabTitleFormatter.tabTitle(query),
                TabTitleFormatter.tooltip(query), () -> closeTab(tab)));
        tabbedPane.setToolTipTextAt(index, TabTitleFormatter.tooltip(query));
        tabbedPane.setSelectedIndex(index);
        showCard();
        tab.attachHandle(searchController.startSearch(query, tab));
        return tab;
    }

    /**
     * Cancels the tab's search and removes the tab (D-41).
     *
     * @param tab a tab of this pane, not {@code null}
     */
    public void closeTab(SearchTabPanel tab) {
        Objects.requireNonNull(tab, "tab");
        tab.cancelSearch();
        tabbedPane.remove(tab);
        showCard();
    }

    /**
     * Returns the number of open search tabs.
     *
     * @return tab count
     */
    public int tabCount() {
        return tabbedPane.getTabCount();
    }

    /** Shows the tabbed pane if any tab is open, otherwise the hint. */
    private void showCard() {
        cardLayout.show(this, tabbedPane.getTabCount() == 0 ? HINT_CARD : TABS_CARD);
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
