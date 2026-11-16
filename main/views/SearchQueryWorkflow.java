package views;

import java.util.Objects;
import java.util.Optional;

import models.SearchQuery;

/**
 * The interactive part of Search &rarr; Search By... (UC2, S2): asks for a query until it is valid
 * or the user cancels.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Sequence (required):</b> {@code input = ""}, {@code error = empty}; loop:
 * {@code raw = dialogs.askSearchQuery(input, error)}; empty &rarr; return empty;
 * {@code error = SearchQuery.validate(raw)}; if valid return {@code SearchQuery.of(raw)};
 * else {@code input = raw} and repeat. The caller ({@link MainFrame}) then opens a new tab with
 * {@link SearchResultsPane#openSearch(SearchQuery)}.</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class SearchQueryWorkflow {

    /** Shows the dialogs. */
    private final DialogService dialogs;

    /**
     * Creates the workflow.
     *
     * @param dialogs the dialog service, not {@code null}
     * @throws NullPointerException if {@code dialogs} is {@code null}
     */
    public SearchQueryWorkflow(DialogService dialogs) {
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
    }

    /**
     * Runs the sequence above.
     *
     * @return the valid query, or empty if the user cancelled
     */
    public Optional<SearchQuery> promptForQuery() {
        throw new UnsupportedOperationException("TODO: Member D");
    }
}
