package controllers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import controllers.concurrency.TaskScheduler;

/**
 * Orderly application shutdown (race R8): cancel searches, cancel imports and the startup load,
 * then stop the thread pools.
 *
 * <p><b>Owner:</b> Member C.</p>
 *
 * <p><b>Order (required):</b> (1) {@code searchController.cancelAll()} - cancels tokens so CPU
 * loops stop within one check interval; (2) {@code libraryController.cancelAll()} - cancels import
 * tokens and blocks further callbacks; (3) {@code scheduler.shutdownNow()} - interrupts workers and
 * rejects new tasks. Never waits for termination on the calling (EDT) thread; total time &lt; 100 ms.
 * The second and later calls do nothing ({@link #shutDown} CAS).</p>
 *
 * <p><b>Threading:</b> Thread-safe.</p>
 */
public final class DefaultLifecycleController implements LifecycleController {

    /** Library controller whose imports are cancelled. */
    private final DefaultDocumentLibraryController libraryController;
    /** Search controller whose searches are cancelled. */
    private final DefaultSearchController searchController;
    /** Pools to stop. */
    private final TaskScheduler scheduler;
    /** Guards idempotency. */
    private final AtomicBoolean shutDown = new AtomicBoolean();

    /**
     * Creates the lifecycle controller.
     *
     * @param libraryController the library controller, not {@code null}
     * @param searchController  the search controller, not {@code null}
     * @param scheduler         the scheduler, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public DefaultLifecycleController(DefaultDocumentLibraryController libraryController,
                                      DefaultSearchController searchController,
                                      TaskScheduler scheduler) {
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.searchController = Objects.requireNonNull(searchController, "searchController");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** {@inheritDoc} Test: C-38. */
    @Override
    public void shutdown() {
        if (shutDown.compareAndSet(false, true)) {
            searchController.cancelAll();
            libraryController.cancelAll();
            scheduler.shutdownNow();
        }
    }
}