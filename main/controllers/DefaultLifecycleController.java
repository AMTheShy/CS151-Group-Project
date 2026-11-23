package controllers;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import controllers.concurrency.TaskScheduler;

/** Cancels active work and stops the background thread pools on exit. */
public final class DefaultLifecycleController implements LifecycleController {

    private final DefaultDocumentLibraryController libraryController;
    private final DefaultSearchController searchController;
    private final TaskScheduler scheduler;
    private final AtomicBoolean shutDown = new AtomicBoolean();

    public DefaultLifecycleController(DefaultDocumentLibraryController libraryController,
                                      DefaultSearchController searchController,
                                      TaskScheduler scheduler) {
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.searchController = Objects.requireNonNull(searchController, "searchController");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    @Override
    public void shutdown() {
        if (shutDown.compareAndSet(false, true)) {
            searchController.cancelAll();
            libraryController.cancelAll();
            scheduler.shutdownNow();
        }
    }
}