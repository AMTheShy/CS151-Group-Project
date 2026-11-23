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

/** Runs searches over a repository snapshot and delivers the outcome on the UI thread. */
public final class DefaultSearchController implements SearchController {

    private static final Logger LOGGER = Logger.getLogger(DefaultSearchController.class.getName());

    private final DocumentRepository repository;
    private final SearchEngine searchEngine;
    private final TaskScheduler scheduler;
    private final UiDispatcher uiDispatcher;
    private final Set<DefaultSearchHandle> activeSearches = ConcurrentHashMap.newKeySet();

    private boolean closed;

    public DefaultSearchController(DocumentRepository repository, SearchEngine searchEngine,
                                   TaskScheduler scheduler, UiDispatcher uiDispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.searchEngine = Objects.requireNonNull(searchEngine, "searchEngine");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
    }

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

    public void cancelAll() {
        List<DefaultSearchHandle> snapshot;
        synchronized (activeSearches) {
            closed = true;
            snapshot = List.copyOf(activeSearches);
        }
        snapshot.forEach(DefaultSearchHandle::cancel);
        activeSearches.removeAll(snapshot);
    }

    private Optional<DocumentSearchResult> searchOne(
            Document document, SearchQuery query, DefaultSearchHandle handle) {
        handle.token().throwIfCancelled();
        return searchEngine.search(document, query, handle.token());
    }

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