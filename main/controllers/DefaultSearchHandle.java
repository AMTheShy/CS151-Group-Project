package controllers;
//member c 
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import models.CancellationToken;
import models.SearchQuery;
import models.SimpleCancellationToken;

/** Tracks the state and worker tasks of one search. */
public final class DefaultSearchHandle implements SearchHandle {

    enum State {
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private final SearchQuery query;
    private final SimpleCancellationToken token = new SimpleCancellationToken();
    private final List<Future<?>> futures = new CopyOnWriteArrayList<>();
    private final AtomicReference<State> state =
            new AtomicReference<>(State.RUNNING);

    public DefaultSearchHandle(SearchQuery query) {
        this.query = Objects.requireNonNull(query, "query");
    }

    @Override
    public SearchQuery query() {
        return query;
    }

    @Override
    public void cancel() {
        if (state.compareAndSet(State.RUNNING, State.CANCELLED)) {
            token.cancel();
            for (Future<?> future : futures) {
                future.cancel(true);
            }
        }
    }

    @Override
    public boolean isCancelled() {
        return state.get() == State.CANCELLED;
    }

    @Override
    public boolean isDone() {
        return state.get() != State.RUNNING;
    }

    CancellationToken token() {
        return token;
    }

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

    /** Called on the Swing event thread immediately before delivering results. */
    boolean tryComplete() {
        return state.compareAndSet(State.RUNNING, State.COMPLETED);
    }

    /** Called on the Swing event thread immediately before reporting failure. */
    boolean tryFail() {
        return state.compareAndSet(State.RUNNING, State.FAILED);
    }
}