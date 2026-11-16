package models;

/**
 * Receives progress of a long-running operation, in operation-specific units (bytes for file
 * reading, documents for the startup load).
 *
 * <p>Called on the worker thread that performs the operation, never on the EDT. Implementations
 * must be fast and non-blocking; the controller's implementation only creates an
 * {@link ImportProgress} and hands it to the {@code UiDispatcher}. Implementations may receive
 * many calls; the caller is free to throttle (e.g. once per buffer).</p>
 *
 * <p>Contract for callers: {@code completed} is non-decreasing across calls of one operation,
 * {@code 0 <= completed <= total}.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Called from worker threads; implementations must be thread-safe.</p>
 */
@FunctionalInterface
public interface ProgressListener {

    /** A listener that ignores all progress. */
    ProgressListener NONE = (completed, total) -> { };

    /**
     * Reports progress.
     *
     * @param completed units done so far
     * @param total     total units, {@code >= completed}
     */
    void onProgress(long completed, long total);
}
