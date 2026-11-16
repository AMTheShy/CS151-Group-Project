package controllers;

import models.SearchQuery;

/**
 * Use-case facade for searching (UC2, S5, U3).
 *
 * <p>A search runs in the background over a snapshot of the repository taken at the moment
 * {@link #startSearch} is called (S5): documents imported later are not searched, documents
 * deleted later stay in the result. The documents are searched in parallel, and the listener
 * receives exactly one final callback on the EDT - unless the search is cancelled first, in which
 * case it receives nothing at all.</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C
 * ({@code controllers.DefaultSearchController}). Used by Member D's search tab.</p>
 *
 * <p>Threading: Thread-safe; intended to be called on the EDT. Returns within microseconds.</p>
 */
public interface SearchController {

    /**
     * Starts a search and returns immediately.
     *
     * @param query    the validated query, not {@code null}
     * @param listener receives the single outcome on the EDT, not {@code null}
     * @return a handle to cancel the search (e.g. when its tab is closed)
     * @throws NullPointerException  if an argument is {@code null}
     * @throws IllegalStateException if the controller has been shut down
     */
    SearchHandle startSearch(SearchQuery query, SearchListener listener);
}
