package controllers.concurrency;
//MEMBER C
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

/** Runs queued tasks only when a test calls runNext or runAll. */
public final class ManualTaskScheduler implements TaskScheduler {

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private int searchTaskCount;
    private boolean shutdown;

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

    public boolean runNext() {
        Runnable task = queue.pollFirst();
        if (task == null) {
            return false;
        }

        task.run();
        return true;
    }

    public int runAll() {
        int count = 0;
        while (runNext()) {
            count++;
        }
        return count;
    }

    public int pendingCount() {
        return queue.size();
    }

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