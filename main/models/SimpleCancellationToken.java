package models;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The standard {@link CancellationToken}: a flag that can be set once.
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe. {@link #cancel()} happens-before every subsequent
 * {@link #isCancelled()} that returns {@code true} (backed by an {@link AtomicBoolean}).</p>
 */
public final class SimpleCancellationToken implements CancellationToken {

    /** Set to {@code true} exactly once by {@link #cancel()}. */
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    /** Creates a token that is not cancelled. */
    public SimpleCancellationToken() {
        // nothing to initialize beyond the field
    }

    /**
     * Requests cancellation. Idempotent.
     *
     * @return {@code true} if this call changed the state, {@code false} if it was already cancelled
     */
    public boolean cancel() {
        return cancelled.compareAndSet(false, true);
    }

    /**
     * Tells whether {@link #cancel()} was called.
     *
     * @return {@code true} once cancelled
     */
    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }
}
