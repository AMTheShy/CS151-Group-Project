package controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import controllers.concurrency.TaskScheduler;
import models.Document;
import models.DocumentSearchResult;
import models.OperationCancelledException;
import models.SearchQuery;
import models.search.SearchEngine;
import models.storage.DocumentRepository;

/**
 * Runs searches in parallel over a repository snapshot and reports one outcome on the EDT
 * (UC2, S5, U3, 3.4: 100 MB in &le; 1 s).
 *
 * <p><b>Owner:</b> Member C (Controllers, Concurrency &amp; Integration).</p>
 *
 * <p><b>Algorithm of {@link #startSearch} (required):</b></p>
 * <ol>
 *   <li>Reject if shut down ({@code IllegalStateException}). Create a {@link DefaultSearchHandle};
 *       add it to {@link #activeSearches}.</li>
 *   <li>{@code snapshot = repository.listAllSortedByTitle()} - taken <em>now</em>, on the calling
 *       thread (S5; cheap, lock-free).</li>
 *   <li>For each document: {@code scheduler.supplySearch(() -> engine.search(doc, query, handle.token()))}.
 *       Attach all futures to the handle.</li>
 *   <li>{@code CompletableFuture.allOf(futures).whenComplete(...)} - <b>never</b> block a pool
 *       thread with {@code join()} on sibling tasks (deadlock R10). In the completion stage:
 *       if any future failed with something other than cancellation, build a failure message;
 *       otherwise collect the present results in snapshot order (already sorted by title, D6)
 *       into an immutable list.</li>
 *   <li>Dispatch to the EDT: {@code if (handle.tryComplete()) listener.onSearchCompleted(results)}
 *       (or {@code tryFail()} / {@code onSearchFailed}). Remove the handle from
 *       {@link #activeSearches} in every outcome.</li>
 *   <li>Return the handle immediately. An empty repository completes with an empty list.</li>
 * </ol>
 *
 * <p><b>Failure handling:</b> an unexpected {@code RuntimeException} from the engine fails the
 * whole search with message {@code Search failed: <cause message>} (results of other documents
 * are discarded, C-25) and is logged with {@code Level.SEVERE}. {@code OperationCancelledException}
 * and {@code CancellationException} are not failures - after a cancel, nothing is delivered.</p>
 *
 * <p><b>Threading:</b> Thread-safe. {@link #startSearch} is called on the EDT and returns in
 * microseconds; all scanning happens on the search pool.</p>
 *
 * <p><b>Implementation notes:</b> The active-search set also guards admission during
 * shutdown. If scheduling fails partway through, already submitted futures are cancelled
 * before the failure is reported.</p>
 */
public final class DefaultSearchController implements SearchController {

    /** Logger for engine failures. */
    private static final Logger LOGGER = Logger.getLogger(DefaultSearchController.class.getName());

    /** Source of the snapshot (only {@code listAllSortedByTitle} is used). */
    private final DocumentRepository repository;
    /** The matching strategy. */
    private final SearchEngine searchEngine;
    /** Runs the per-document tasks on the search pool. */
    private final TaskScheduler scheduler;
    /** Moves the single outcome to the EDT. */
    private final UiDispatcher uiDispatcher;
    /** Searches that have not reached a terminal state; used by {@link #cancelAll()}. */
    private final Set<DefaultSearchHandle> activeSearches = ConcurrentHashMap.newKeySet();

    /** Guards admission of new searches under the active-search lock. */
    private boolean closed;

    /**
     * Creates the controller.
     *
     * @param repository   the document repository, not {@code null}
     * @param searchEngine the search strategy, not {@code null}
     * @param scheduler    the task scheduler, not {@code null}
     * @param uiDispatcher the EDT dispatcher, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public DefaultSearchController(DocumentRepository repository, SearchEngine searchEngine,
                                   TaskScheduler scheduler, UiDispatcher uiDispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.searchEngine = Objects.requireNonNull(searchEngine, "searchEngine");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
    }

    /**
     * {@inheritDoc}
     *
     * <p>See the algorithm in the class description. Tests: C-18..C-29.</p>
     */
    @Override
    public SearchHandle startSearch(SearchQuery query, SearchListener listener) {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(listener, "listener");

        DefaultSearchHandle handle = new DefaultSearchHandle(query);
        synchronized (activeSearches) {
            if (closed || scheduler.isShutdown()) {
                throw new IllegalStateException("Search controller is shut down");
            }
            activeSearches.add(handle);
        }

        var futures = new ArrayList<CompletableFuture<Optional<DocumentSearchResult>>>();

        try {
            for (Document document : repository.listAllSortedByTitle()) {
                if (handle.isCancelled()) {
                    break;
                }
                var future = scheduler.supplySearch(() -> searchOne(document, query, handle));
                futures.add(future);
            }
            handle.attachFutures(futures);
        } catch (RuntimeException failure) {
            futures.forEach(future -> future.cancel(true));
            reportFailure(handle, listener, failure);
            return handle;
        }

        CompletableFuture.allOf(futures.toArray(CompletableFuture<?>[]::new))
                .whenComplete((ignored, failure) -> {
                    if (handle.isCancelled()) {
                        activeSearches.remove(handle);
                    } else if (failure != null) {
                        reportFailure(handle, listener, failure);
                    } else {
                        var results = futures.stream()
                                .map(CompletableFuture::join)
                                .flatMap(Optional::stream)
                                .toList();
                        deliverResults(handle, listener, results);
                    }
                });

        return handle;
    }

    /**
     * Cancels every active search; used by {@link DefaultLifecycleController#shutdown()}.
     * Test: C-30.
     */
    public void cancelAll() {
        List<DefaultSearchHandle> snapshot;
        synchronized (activeSearches) {
            closed = true;
            snapshot = List.copyOf(activeSearches);
        }
        snapshot.forEach(DefaultSearchHandle::cancel);
        activeSearches.removeAll(snapshot);
    }

    /**
     * Searches one document (runs on a search-pool thread).
     *
     * @param document the document snapshot
     * @param query    the query
     * @param handle   the handle whose token is checked
     * @return the result, or empty if no match
     */
    private Optional<DocumentSearchResult> searchOne(
            Document document, SearchQuery query, DefaultSearchHandle handle) {
        handle.token().throwIfCancelled();
        return searchEngine.search(document, query, handle.token());
    }

    /** Delivers a search failure or retires a cancelled search. */
    private void reportFailure(DefaultSearchHandle handle, SearchListener listener, Throwable failure) {
        Throwable cause = failure instanceof CompletionException && failure.getCause() != null
                ? failure.getCause() : failure;
        if (cause instanceof CancellationException || cause instanceof OperationCancelledException
                || handle.isCancelled()) {
            handle.cancel();
            activeSearches.remove(handle);
            return;
        }

        LOGGER.log(Level.SEVERE, "Search failed", cause);
        String detail = Objects.toString(cause.getMessage(), cause.getClass().getSimpleName());
        deliverFailure(handle, listener, "Search failed: " + detail);
    }

    /**
     * Dispatches {@code onSearchCompleted} to the EDT, guarded by {@code handle.tryComplete()}.
     *
     * @param handle   the search handle
     * @param listener the listener
     * @param results  the immutable, title-sorted results
     */
    private void deliverResults(DefaultSearchHandle handle, SearchListener listener,
                                List<DocumentSearchResult> results) {
        uiDispatcher.dispatch(() -> {
            try {
                if (handle.tryComplete()) {
                    listener.onSearchCompleted(results);
                }
            } finally {
                activeSearches.remove(handle);
            }
        });
    }

    /**
     * Dispatches {@code onSearchFailed} to the EDT, guarded by {@code handle.tryFail()}.
     *
     * @param handle   the search handle
     * @param listener the listener
     * @param message  the user-presentable failure message
     */
    private void deliverFailure(DefaultSearchHandle handle, SearchListener listener, String message) {
        uiDispatcher.dispatch(() -> {
            try {
                if (handle.tryFail()) {
                    listener.onSearchFailed(message);
                }
            } finally {
                activeSearches.remove(handle);
            }
        });
    }
}