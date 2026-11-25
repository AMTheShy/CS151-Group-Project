package controllers.concurrency;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
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
 *
 * <p><b>Implementation notes:</b> Shutdown also cancels the futures associated with dropped
 * tasks, so callers can observe their terminal state.</p>
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
    }

    @Override
    public Future<?> submitImport(Runnable task) {
        return enqueue(task);
    }

    @Override
    public <T> CompletableFuture<T> supplySearch(Supplier<T> task) {
        Objects.requireNonNull(task, "task");
        requireRunning();

        searchTaskCount++;
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.addLast(new QueuedSearch<>(task, future));
        return future;
    }

    @Override
    public Future<?> submitBackground(Runnable task) {
        return enqueue(task);
    }

    @Override
    public void shutdownNow() {
        shutdown = true;
        while (!queue.isEmpty()) {
            Runnable task = queue.removeFirst();
            if (task instanceof Future<?> future) {
                future.cancel(false);
            } else if (task instanceof QueuedSearch<?> search) {
                search.future().cancel(false);
            }
        }
    }

    @Override
    public boolean isShutdown() {
        return shutdown;
    }

    /**
     * Runs the oldest queued task.
     *
     * @return {@code true} if a task was run
     */
    public boolean runNext() {
        Runnable task = queue.pollFirst();
        if (task == null) {
            return false;
        }

        task.run();
        return true;
    }

    /**
     * Runs queued tasks until none is left.
     *
     * @return the number of tasks run
     */
    public int runAll() {
        int count = 0;
        while (runNext()) {
            count++;
        }
        return count;
    }

    /**
     * Returns the number of queued tasks.
     *
     * @return pending count
     */
    public int pendingCount() {
        return queue.size();
    }

    /**
     * Returns how many search tasks were submitted.
     *
     * @return the count
     */
    public int searchTaskCount() {
        return searchTaskCount;
    }

    private Future<?> enqueue(Runnable task) {
        Objects.requireNonNull(task, "task");
        requireRunning();

        FutureTask<Void> future = new FutureTask<>(task, null) {
            @Override
            protected void done() {
                if (isCancelled()) {
                    queue.remove(this);
                }
            }
        };

        queue.addLast(future);
        return future;
    }

    private void requireRunning() {
        if (shutdown) {
            throw new RejectedExecutionException("Scheduler is shut down");
        }
    }

    private record QueuedSearch<T>(
            Supplier<T> task,
            CompletableFuture<T> future
    ) implements Runnable {

        @Override
        public void run() {
            if (!future.isCancelled()) {
                try {
                    future.complete(task.get());
                } catch (Throwable failure) {
                    future.completeExceptionally(failure);
                }
            }
        }
    }
}