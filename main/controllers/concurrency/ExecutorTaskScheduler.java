package controllers.concurrency;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.AppConstants;
import models.OperationCancelledException;

/**
 * Production {@link TaskScheduler} backed by three fixed-size {@link ExecutorService}s.
 *
 * <p><b>Owner:</b> Member C (Controllers, Concurrency &amp; Integration).</p>
 *
 * <p><b>Requirements covered:</b> 4.4 (bounded import pool, per-core search pool), U6 (no work
 * on the EDT), "application exit shuts the pools down" (race R8).</p>
 *
 * <p><b>Required behavior:</b></p>
 * <ul>
 *   <li>Each pool is a {@code ThreadPoolExecutor} with exactly the configured number of threads
 *       and an unbounded {@code LinkedBlockingQueue} (submitting never blocks the EDT).</li>
 *   <li>Threads are created by a factory that names them {@code lse-import-1},
 *       {@code lse-search-3}, {@code lse-background-2}... (debuggability; test C-33), marks them
 *       non-daemon (so an interrupted write finishes its {@code finally} cleanup before the JVM
 *       exits), and installs an {@code UncaughtExceptionHandler} that logs with
 *       {@code Level.SEVERE}.</li>
 *   <li>{@link #supplySearch} uses {@code CompletableFuture.supplyAsync(task, searchPool)}.</li>
 *   <li>Tasks submitted with {@link #submitImport}/{@link #submitBackground} that throw are not
 *       lost: wrap them so the exception is logged (a plain {@code submit} would swallow it inside
 *       the {@code Future}; test C-35).</li>
 *   <li>{@link #shutdownNow()} calls {@code shutdownNow()} on all three pools (interrupting
 *       workers) and returns immediately; it never calls {@code awaitTermination} on the EDT.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Thread-safe.</p>
 *
 * <p><b>Implementation notes:</b> Search futures are tracked until completion so shutdown
 * can cancel futures for work removed from the executor queues.</p>
 */
public final class ExecutorTaskScheduler implements TaskScheduler {

    /** Logger for uncaught task failures. */
    private static final Logger LOGGER =
            Logger.getLogger(ExecutorTaskScheduler.class.getName());

    /** Pool for imports, size {@code importThreads}. */
    private final ExecutorService importPool;

    /** Pool for per-document search tasks, size {@code searchThreads}. */
    private final ExecutorService searchPool;

    /** Pool for startup load, delete and export, size {@code backgroundThreads}. */
    private final ExecutorService backgroundPool;

    /** Search futures that may still need cancellation during shutdown. */
    private final Set<CompletableFuture<?>> searches = ConcurrentHashMap.newKeySet();

    /**
     * Creates the three pools.
     *
     * @param importThreads     threads of the import pool, {@code >= 1}
     * @param searchThreads     threads of the search pool, {@code >= 1}
     * @param backgroundThreads threads of the background pool, {@code >= 1}
     * @throws IllegalArgumentException if a count is {@code < 1}
     */
    public ExecutorTaskScheduler(int importThreads, int searchThreads, int backgroundThreads) {
        this.importPool = newFixedPool("lse-import", importThreads);
        this.searchPool = newFixedPool("lse-search", searchThreads);
        this.backgroundPool = newFixedPool("lse-background", backgroundThreads);
    }

    /**
     * Creates a scheduler with the production sizes from {@link AppConstants}.
     *
     * @return a new scheduler
     */
    public static ExecutorTaskScheduler createDefault() {
        return new ExecutorTaskScheduler(
                AppConstants.IMPORT_THREAD_COUNT,
                AppConstants.searchThreadCount(),
                AppConstants.BACKGROUND_THREAD_COUNT);
    }

    /** {@inheritDoc} Test: C-31. */
    @Override
    public Future<?> submitImport(Runnable task) {
        return importPool.submit(logFailures(task));
    }

    /** {@inheritDoc} Test: C-32. */
    @Override
    public <T> CompletableFuture<T> supplySearch(Supplier<T> task) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(
                Objects.requireNonNull(task, "task"), searchPool);
        searches.add(future);
        future.whenComplete((result, failure) -> searches.remove(future));

        if (searchPool.isShutdown()) {
            future.cancel(false);
        }

        return future;
    }

    /** {@inheritDoc} */
    @Override
    public Future<?> submitBackground(Runnable task) {
        return backgroundPool.submit(logFailures(task));
    }

    /** {@inheritDoc} Test: C-34. */
    @Override
    public void shutdownNow() {
        for (ExecutorService pool : List.of(importPool, searchPool, backgroundPool)) {
            for (Runnable dropped : pool.shutdownNow()) {
                if (dropped instanceof Future<?> future) {
                    future.cancel(false);
                }
            }
        }
        searches.forEach(future -> future.cancel(false));
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdown() {
        return importPool.isShutdown() && searchPool.isShutdown() && backgroundPool.isShutdown();
    }

    /** Logs unexpected task failures before exposing them through the task future. */
    private static Runnable logFailures(Runnable task) {
        Objects.requireNonNull(task, "task");
        return () -> {
            try {
                task.run();
            } catch (CancellationException | OperationCancelledException cancellation) {
                throw cancellation;
            } catch (RuntimeException | Error failure) {
                LOGGER.log(Level.SEVERE, "Background task failed", failure);
                throw failure;
            }
        };
    }

    /**
     * Creates a fixed pool with named, non-daemon threads and a logging uncaught-exception handler.
     *
     * @param namePrefix thread name prefix, e.g. {@code lse-search}
     * @param threads    number of threads, {@code >= 1}
     * @return the pool
     * @throws IllegalArgumentException if {@code threads < 1}
     */
    private static ExecutorService newFixedPool(String namePrefix, int threads) {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(threads, task -> {
            Thread thread = new Thread(task, namePrefix + "-" + sequence.incrementAndGet());
            thread.setDaemon(false);
            thread.setUncaughtExceptionHandler((worker, failure) ->
                    LOGGER.log(Level.SEVERE, "Uncaught failure on " + worker.getName(), failure));
            return thread;
        });
    }
}