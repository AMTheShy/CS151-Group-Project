package testsupport;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import controllers.SearchController;
import controllers.SearchHandle;
import controllers.SearchListener;
import models.DocumentSearchResult;
import models.SearchQuery;

/**
 * Recording {@link SearchController} whose outcomes are delivered manually by the test. Used by
 * Member D to test search tabs.
 *
 * <p>{@link #complete} and {@link #fail} honor the real contract: nothing is delivered if the
 * handle was cancelled, and at most one outcome is delivered per search. They invoke the listener
 * synchronously on the calling thread (call them on the EDT in Swing tests).</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeSearchController implements SearchController {

    /** One recorded search. */
    public record StartedSearch(SearchQuery query, SearchListener listener, FakeSearchHandle handle) {
    }

    private final List<StartedSearch> startedSearches = new CopyOnWriteArrayList<>();

    /** Creates the controller. */
    public FakeSearchController() {
        // empty
    }

    /**
     * Returns all started searches in call order.
     *
     * @return immutable copy
     */
    public List<StartedSearch> startedSearches() {
        return List.copyOf(startedSearches);
    }

    /**
     * Returns the most recently started search.
     *
     * @return the last search
     * @throws IllegalStateException if no search was started
     */
    public StartedSearch lastSearch() {
        if (startedSearches.isEmpty()) {
            throw new IllegalStateException("no search was started");
        }
        return startedSearches.get(startedSearches.size() - 1);
    }

    /**
     * Delivers {@code onSearchCompleted} unless the search was cancelled or already finished.
     *
     * @param search  the search
     * @param results the sorted results
     * @return {@code true} if the listener was called
     */
    public boolean complete(StartedSearch search, List<DocumentSearchResult> results) {
        if (!search.handle().markDelivered()) {
            return false;
        }
        search.listener().onSearchCompleted(List.copyOf(results));
        return true;
    }

    /**
     * Delivers {@code onSearchFailed} unless the search was cancelled or already finished.
     *
     * @param search  the search
     * @param message the message
     * @return {@code true} if the listener was called
     */
    public boolean fail(StartedSearch search, String message) {
        if (!search.handle().markDelivered()) {
            return false;
        }
        search.listener().onSearchFailed(message);
        return true;
    }

    @Override
    public SearchHandle startSearch(SearchQuery query, SearchListener listener) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(listener, "listener");
        FakeSearchHandle handle = new FakeSearchHandle(query);
        startedSearches.add(new StartedSearch(query, listener, handle));
        return handle;
    }
}
