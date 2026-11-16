package controllers.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * The only gateway to background threads in the whole application (4.4). Controllers submit work
 * here instead of creating threads; tests substitute a deterministic scheduler
 * ({@code test/controllers/concurrency/ManualTaskScheduler}).
 *
 * <p>Three separate pools keep unrelated work from starving each other:</p>
 * <ul>
 *   <li><b>import</b> - bounded ({@code AppConstants.IMPORT_THREAD_COUNT} = 2): reading/validating
 *       user files. Bounded so ten simultaneous imports do not thrash the disk.</li>
 *   <li><b>search</b> - one thread per core ({@code AppConstants.searchThreadCount()}): one task
 *       per document of a search; CPU-bound.</li>
 *   <li><b>background</b> - {@code AppConstants.BACKGROUND_THREAD_COUNT} = 2: startup load,
 *       deletions, exports. Separate so a long startup load never delays a download.</li>
 * </ul>
 *
 * <p><b>Owner:</b> Member C (module-internal interface of {@code controllers}; not part of the
 * frozen contract because no other module uses it).</p>
 *
 * <p><b>Threading:</b> Implementations must be thread-safe.</p>
 */
public interface TaskScheduler {

    /**
     * Runs an import task on the import pool.
     *
     * @param task the task, not {@code null}
     * @return the future of the task (cancel with {@code cancel(true)} to interrupt)
     * @throws java.util.concurrent.RejectedExecutionException if the scheduler is shut down
     */
    Future<?> submitImport(Runnable task);

    /**
     * Runs a CPU-bound search step on the search pool. Returned futures are combined by the search
     * controller with {@code CompletableFuture.allOf}, so that no pool thread ever blocks waiting
     * for other tasks of the same pool (deadlock avoidance, design doc R10).
     *
     * @param task the computation, not {@code null}
     * @param <T>  result type
     * @return a future completed with the task's result or exception
     * @throws java.util.concurrent.RejectedExecutionException if the scheduler is shut down
     */
    <T> CompletableFuture<T> supplySearch(Supplier<T> task);

    /**
     * Runs other background work (startup load, delete, export) on the background pool.
     *
     * @param task the task, not {@code null}
     * @return the future of the task
     * @throws java.util.concurrent.RejectedExecutionException if the scheduler is shut down
     */
    Future<?> submitBackground(Runnable task);

    /**
     * Stops accepting tasks and interrupts running ones on all pools; returns immediately without
     * waiting. Idempotent.
     */
    void shutdownNow();

    /**
     * Tells whether {@link #shutdownNow()} was called.
     *
     * @return {@code true} after shutdown
     */
    boolean isShutdown();
}
