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

public final class ExecutorTaskScheduler implements TaskScheduler {

    private static final Logger LOGGER =
            Logger.getLogger(ExecutorTaskScheduler.class.getName());

    private final ExecutorService importPool;
    private final ExecutorService searchPool;
    private final ExecutorService backgroundPool;
    private final Set<CompletableFuture<?>> searches = ConcurrentHashMap.newKeySet();

    public ExecutorTaskScheduler(int importThreads, int searchThreads, int backgroundThreads) {
        this.importPool = newFixedPool("lse-import", importThreads);
        this.searchPool = newFixedPool("lse-search", searchThreads);
        this.backgroundPool = newFixedPool("lse-background", backgroundThreads);
    }

    public static ExecutorTaskScheduler createDefault() {
        return new ExecutorTaskScheduler(
                AppConstants.IMPORT_THREAD_COUNT,
                AppConstants.searchThreadCount(),
                AppConstants.BACKGROUND_THREAD_COUNT);
    }

    @Override
    public Future<?> submitImport(Runnable task) {
        return importPool.submit(logFailures(task));
    }

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

    @Override
    public Future<?> submitBackground(Runnable task) {
        return backgroundPool.submit(logFailures(task));
    }

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

    @Override
    public boolean isShutdown() {
        return importPool.isShutdown() && searchPool.isShutdown() && backgroundPool.isShutdown();
    }

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