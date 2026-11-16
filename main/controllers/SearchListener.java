package controllers;

import java.util.List;

import models.DocumentSearchResult;

/**
 * Receives the outcome of one search (UC2, U3, U4).
 *
 * <p><b>Guarantees given by the caller:</b> called on the EDT; at most one of the two methods is
 * called, at most once; neither is called if the search was cancelled before the callback ran.</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member D's search tab; invoked by
 * Member C's search controller.</p>
 *
 * <p>Threading: EDT only (guaranteed by the caller).</p>
 */
public interface SearchListener {

    /**
     * The search finished.
     *
     * @param sortedResults immutable list containing only matching documents, sorted by title
     *                      (D6); empty if nothing matched
     */
    void onSearchCompleted(List<DocumentSearchResult> sortedResults);

    /**
     * The search failed because of an unexpected error.
     *
     * @param message user-presentable message
     */
    void onSearchFailed(String message);
}
