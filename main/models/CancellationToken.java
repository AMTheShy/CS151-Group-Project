package models;

/**
 * Cooperative cancellation signal passed into every long-running operation (4.4).
 *
 * <p>Long loops (reading a file, scanning a document, loading persisted documents) must call
 * {@link #throwIfCancelled()} at least every {@link AppConstants#CANCELLATION_CHECK_INTERVAL}
 * characters/bytes, and once per document when iterating documents. Code that receives a token
 * only <em>observes</em> it; only the creator (the controller) cancels it.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). Implementations: {@link SimpleCancellationToken},
 * {@link #NONE}.</p>
 *
 * <p>Threading: Implementations must be thread-safe: one thread cancels while another polls.</p>
 */
public interface CancellationToken {

    /** A token that is never cancelled; for callers that do not support cancellation. */
    CancellationToken NONE = () -> false;

    /**
     * Tells whether cancellation was requested.
     *
     * @return {@code true} once cancellation has been requested; never goes back to {@code false}
     */
    boolean isCancelled();

    /**
     * Throws if cancellation was requested.
     *
     * @throws OperationCancelledException if {@link #isCancelled()} returns {@code true}
     */
    default void throwIfCancelled() {
        if (isCancelled()) {
            throw new OperationCancelledException("Operation was cancelled");
        }
    }
}
