package controllers;

/**
 * Application life-cycle operations requested by the view (File &rarr; Exit, closing the window).
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C
 * ({@code controllers.DefaultLifecycleController}). Used by Member D's main frame.</p>
 *
 * <p>Threading: Thread-safe; intended to be called on the EDT.</p>
 */
public interface LifecycleController {

    /**
     * Stops all background work: cancels running searches and imports, stops accepting new
     * tasks and shuts the thread pools down. Returns within {@code 100 ms}; it does not wait for
     * worker threads to finish (they finish on their own within
     * {@code AppConstants.SHUTDOWN_GRACE_MILLIS}). Idempotent. After this call, controller methods
     * that start work throw {@link IllegalStateException}, and no further listener callbacks are
     * delivered.
     */
    void shutdown();
}
