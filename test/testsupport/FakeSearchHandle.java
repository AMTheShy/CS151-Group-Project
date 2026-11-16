package testsupport;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import controllers.SearchHandle;
import models.SearchQuery;

/**
 * {@link SearchHandle} returned by {@link FakeSearchController}; records cancellations.
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeSearchHandle implements SearchHandle {

    private final SearchQuery query;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicInteger cancelCalls = new AtomicInteger();

    /**
     * Creates a running handle.
     *
     * @param query the query
     */
    public FakeSearchHandle(SearchQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    /**
     * Marks the search as finished (called by {@link FakeSearchController} when it delivers).
     *
     * @return {@code true} if the handle was still running and not cancelled
     */
    boolean markDelivered() {
        return !cancelled.get() && done.compareAndSet(false, true);
    }

    /**
     * Returns how often {@link #cancel()} was called.
     *
     * @return the count
     */
    public int cancelCalls() {
        return cancelCalls.get();
    }

    @Override
    public SearchQuery query() {
        return query;
    }

    @Override
    public void cancel() {
        cancelCalls.incrementAndGet();
        if (!done.get()) {
            cancelled.set(true);
            done.set(true);
        }
    }

    @Override
    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public boolean isDone() {
        return done.get();
    }
}
