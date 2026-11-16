package controllers.concurrency;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Deterministic {@link TaskScheduler} for controller unit tests: tasks are queued and run only when
 * the test calls {@link #runNext()} / {@link #runAll()}, on the test thread. Lets tests interleave
 * "worker" steps, cancellations and EDT flushes in any order without real threads.
 *
 * <p><b>Owner:</b> Member C (test helper of the controllers module; not part of the frozen contract).</p>
 *
 * <p><b>Required behavior:</b></p>
 * <ul>
 *   <li>{@link #submitImport}/{@link #submitBackground} queue the runnable and return a
 *       {@code FutureTask}-like future that completes when the task is run; {@code cancel(true)}
 *       on it removes the task from the queue if it has not run.</li>
 *   <li>{@link #supplySearch} queues the supplier and returns a {@code CompletableFuture} completed
 *       (normally or exceptionally) when the task is run; {@link #searchTaskCount()} counts calls.</li>
 *   <li>{@link #runAll()} runs tasks FIFO until the queue is empty, including tasks queued while
 *       running. Exceptions of tasks are captured in their futures, never propagated to the test.</li>
 *   <li>After {@link #shutdownNow()}, submissions throw {@code RejectedExecutionException} and
 *       queued tasks are dropped.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Not thread-safe; confined to the test thread.</p>
 */
public final class ManualTaskScheduler implements TaskScheduler {

    /** Queued tasks in submission order. */
    private final Deque<Runnable> queue = new ArrayDeque<>();

    /** Number of {@link #supplySearch} calls. */
    private int searchTaskCount;

    /** Set by {@link #shutdownNow()}. */
    private boolean shutdown;

    /** Creates an empty scheduler. */
    public ManualTaskScheduler() {
        // empty
    }

    @Override
    public Future<?> submitImport(Runnable task) {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    @Override
    public <T> CompletableFuture<T> supplySearch(Supplier<T> task) {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    @Override
    public Future<?> submitBackground(Runnable task) {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    @Override
    public void shutdownNow() {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    /**
     * Runs the oldest queued task.
     *
     * @return {@code true} if a task was run
     */
    public boolean runNext() {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    /**
     * Runs queued tasks until none is left.
     *
     * @return the number of tasks run
     */
    public int runAll() {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    /**
     * Returns the number of queued tasks.
     *
     * @return pending count
     */
    public int pendingCount() {
        throw new UnsupportedOperationException("TODO: Member C");
    }

    /**
     * Returns how many search tasks were submitted.
     *
     * @return the count
     */
    public int searchTaskCount() {
        throw new UnsupportedOperationException("TODO: Member C");
    }
}
