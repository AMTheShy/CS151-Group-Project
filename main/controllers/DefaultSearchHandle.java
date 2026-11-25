package controllers;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import models.CancellationToken;
import models.SearchQuery;
import models.SimpleCancellationToken;

/**
 * {@link SearchHandle} of {@link DefaultSearchController}: owns the search's cancellation token,
 * its worker futures and its single terminal state.
 *
 * <p><b>Owner:</b> Member C.</p>
 *
 * <p><b>State machine:</b> {@code RUNNING -> COMPLETED | FAILED | CANCELLED}, each transition
 * done with one {@code compareAndSet} on {@link #state}, so exactly one of "deliver the result" and
 * "cancel" wins, whichever threads they run on.</p>
 *
 * <p><b>How the "never call back after cancel" guarantee is implemented (race R5):</b> the search
 * controller dispatches the delivery to the EDT as
 * {@code () -> { if (handle.tryComplete()) listener.onSearchCompleted(results); }}. The view calls
 * {@link #cancel()} on the EDT when the tab closes. Both run on the EDT, and both go through the
 * CAS, so if the tab was closed before the delivery task runs, {@link #tryComplete()} returns
 * {@code false} and the listener is never called.</p>
 *
 * <p><b>Threading:</b> Thread-safe.</p>
 */
public final class DefaultSearchHandle implements SearchHandle {

    /** Terminal-state machine of a search. */
    enum State {
        /** Workers may still be running. */
        RUNNING,
        /** The listener received {@code onSearchCompleted}. */
        COMPLETED,
        /** The listener received {@code onSearchFailed}. */
        FAILED,
        /** Cancelled before delivery; the listener is never called. */
        CANCELLED
    }

    /** The query of this search. */
    private final SearchQuery query;
    /** Observed by the search engine on every worker; set by {@link #cancel()}. */
    private final SimpleCancellationToken token = new SimpleCancellationToken();
    /** Per-document worker futures, cancelled with {@code cancel(true)} by {@link #cancel()}. */
    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();
    /** Current state; transitions only via CAS. */
    private final AtomicReference<State> state =
            new AtomicReference<>(State.RUNNING);

    /**
     * Creates a running handle.
     *
     * @param query the query, not {@code null}
     * @throws NullPointerException if {@code query} is {@code null}
     */
    public DefaultSearchHandle(SearchQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    /** {@inheritDoc} */
    @Override
    public SearchQuery query() {
        return query;
    }

    /**
     * {@inheritDoc}
     *
     * <p>CAS RUNNING &rarr; CANCELLED; if it succeeded: {@code token.cancel()} and
     * {@code future.cancel(true)} for every attached future. Test: C-39.</p>
     */
    @Override
    public void cancel() {
        if (state.compareAndSet(State.RUNNING, State.CANCELLED)) {
            token.cancel();
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isCancelled() {
        return state.get() == State.CANCELLED;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDone() {
        return state.get() != State.RUNNING;
    }

    /**
     * Returns the token to pass to the search engine.
     *
     * @return the cancellation token
     */
    CancellationToken token() {
        return token;
    }

    /**
     * Registers worker futures so {@link #cancel()} can cancel them. If the handle is already
     * cancelled, the futures are cancelled immediately.
     *
     * @param workerFutures the futures, not {@code null}
     */
    void attachFutures(List<? extends Future<?>> workerFutures) {
        List<Future<?>> pending = List.copyOf(workerFutures);
        futures.addAll(pending);

        // Cancellation may have started before these futures were added.
        if (isCancelled()) {
            for (Future<?> future : pending) {
                future.cancel(true);
            }
        }
    }

    /**
     * Attempts the transition RUNNING &rarr; COMPLETED. Call on the EDT right before invoking
     * {@code onSearchCompleted}.
     *
     * @return {@code true} if the caller may deliver the result
     */
    boolean tryComplete() {
        return state.compareAndSet(State.RUNNING, State.COMPLETED);
    }

    /**
     * Attempts the transition RUNNING &rarr; FAILED. Call on the EDT right before invoking
     * {@code onSearchFailed}.
     *
     * @return {@code true} if the caller may deliver the failure
     */
    boolean tryFail() {
        return state.compareAndSet(State.RUNNING, State.FAILED);
    }
}