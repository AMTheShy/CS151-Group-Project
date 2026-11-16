package controllers;

import models.SearchQuery;

/**
 * Handle of one running or finished search (U3), in the spirit of a {@code Future}.
 *
 * <p><b>Cancellation guarantee:</b> if {@link #cancel()} is called on the EDT before the
 * listener has been called, the listener will never be called. Implementations achieve this by
 * re-checking the cancelled flag inside the task that runs on the EDT, immediately before
 * invoking the listener (see design doc, "Concurrency and Race Conditions", R5).</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C
 * ({@code controllers.DefaultSearchHandle}).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public interface SearchHandle {

    /**
     * Returns the query of this search.
     *
     * @return the query
     */
    SearchQuery query();

    /**
     * Requests cancellation: sets the cancellation token observed by the search engine and
     * cancels the worker futures. Idempotent; has no effect after the search finished.
     */
    void cancel();

    /**
     * Tells whether {@link #cancel()} was called before the search finished.
     *
     * @return {@code true} if cancelled
     */
    boolean isCancelled();

    /**
     * Tells whether the search reached an end: completed, failed or cancelled.
     *
     * @return {@code true} once no more work will be done for this search
     */
    boolean isDone();
}
