package controllers;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import controllers.concurrency.TaskScheduler;
import models.AppConstants;
import models.Document;
import models.DocumentStorageException;
import models.DocumentSummary;
import models.DocumentTitles;
import models.DuplicateTitleException;
import models.ImportProgress;
import models.InvalidDocumentException;
import models.OperationCancelledException;
import models.SimpleCancellationToken;
import models.search.SnippetExtractor;
import models.storage.DocumentExporter;
import models.storage.DocumentLoader;
import models.storage.DocumentRepository;

public final class DefaultDocumentLibraryController implements DocumentLibraryController {

    private static final Logger LOGGER =
            Logger.getLogger(DefaultDocumentLibraryController.class.getName());

    private final DocumentRepository repository;
    private final DocumentLoader loader;
    private final DocumentExporter exporter;
    private final SnippetExtractor snippetExtractor;
    private final TaskScheduler scheduler;
    private final UiDispatcher uiDispatcher;

    private final CopyOnWriteArrayList<LibraryListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong nextImportId = new AtomicLong(1);
    private final Map<Long, SimpleCancellationToken> activeImports = new ConcurrentHashMap<>();
    private final AtomicBoolean startupLoadStarted = new AtomicBoolean();
    private final AtomicBoolean shutDown = new AtomicBoolean();

    private volatile List<DocumentSummary> summaries = List.of();

    public DefaultDocumentLibraryController(DocumentRepository repository, DocumentLoader loader,
                                            DocumentExporter exporter, SnippetExtractor snippetExtractor,
                                            TaskScheduler scheduler, UiDispatcher uiDispatcher) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.loader = Objects.requireNonNull(loader, "loader");
        this.exporter = Objects.requireNonNull(exporter, "exporter");
        this.snippetExtractor = Objects.requireNonNull(snippetExtractor, "snippetExtractor");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
        repository.addRepositoryListener(this::onRepositoryChanged);
    }

    @Override
    public void addLibraryListener(LibraryListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public void removeLibraryListener(LibraryListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public boolean isTitleTaken(String title) {
        return repository.containsTitle(Objects.requireNonNull(title, "title"));
    }

    @Override
    public long importDocument(Path source, String title, boolean replaceExisting) {
        Objects.requireNonNull(source, "source");
        DocumentTitles.requireValid(title);
        requireRunning();

        long id = nextImportId.getAndIncrement();
        SimpleCancellationToken token = new SimpleCancellationToken();
        activeImports.put(id, token);
        try {
            requireRunning();
            publishProgress(ImportProgress.queued(id, title));
            scheduler.submitImport(() -> runImport(id, source, title, replaceExisting, token));
            return id;
        } catch (RuntimeException failure) {
            activeImports.remove(id);
            throw failure;
        }
    }

    @Override
    public void deleteDocuments(Collection<String> titles) {
        List<String> copy = List.copyOf(titles);
        requireRunning();
        if (copy.isEmpty()) {
            return;
        }

        scheduler.submitBackground(() -> {
            try {
                repository.removeAll(copy);
            } catch (DocumentStorageException failure) {
                publish(() -> listeners.forEach(listener ->
                        listener.onDeleteFailed(failure.getMessage())));
            }
        });
    }

    @Override
    public void exportDocument(Document document, Path target) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(target, "target");
        requireRunning();

        scheduler.submitBackground(() -> {
            try {
                exporter.export(document, target, shutDown::get);
                publish(() -> listeners.forEach(listener ->
                        listener.onExportCompleted(document, target)));
            } catch (DocumentStorageException failure) {
                publish(() -> listeners.forEach(listener ->
                        listener.onExportFailed(document, target, failure.getMessage())));
            } catch (OperationCancelledException ignored) {
                // The application is shutting down.
            }
        });
    }

    @Override
    public void loadPersistedDocuments() {
        requireRunning();
        if (!startupLoadStarted.compareAndSet(false, true)) {
            return;
        }

        scheduler.submitBackground(() -> {
            try {
                var summary = repository.loadPersisted((done, total) -> publish(() ->
                        listeners.forEach(listener ->
                                listener.onStartupLoadProgress((int) done, (int) total))),
                        shutDown::get);
                publish(() -> listeners.forEach(listener ->
                        listener.onStartupLoadCompleted(summary)));
            } catch (DocumentStorageException failure) {
                publish(() -> listeners.forEach(listener ->
                        listener.onStartupLoadFailed(failure.getMessage())));
            } catch (OperationCancelledException ignored) {
                // The application is shutting down.
            }
        });
    }

    @Override
    public List<DocumentSummary> currentDocuments() {
        return summaries;
    }

    public void cancelAll() {
        if (shutDown.compareAndSet(false, true)) {
            activeImports.values().forEach(SimpleCancellationToken::cancel);
            activeImports.clear();
        }
    }

    private void requireRunning() {
        if (shutDown.get() || scheduler.isShutdown()) {
            throw new IllegalStateException("Document library is shut down");
        }
    }

    private void onRepositoryChanged(List<Document> sortedSnapshot) {
        List<DocumentSummary> updated = toSummaries(sortedSnapshot);
        summaries = updated;
        publish(() -> listeners.forEach(listener -> listener.onDocumentsChanged(updated)));
    }

    private void runImport(long importId, Path source, String title, boolean replaceExisting,
                           SimpleCancellationToken token) {
        long[] counts = {0, 0};
        int[] lastPercent = {-1};

        try {
            token.throwIfCancelled();
            String content = loader.load(source, (read, total) -> {
                counts[0] = read;
                counts[1] = total;
                ImportProgress progress = ImportProgress.reading(importId, title, read, total);
                int percent = progress.percentComplete();
                if (percent > lastPercent[0]) {
                    lastPercent[0] = percent;
                    publishProgress(progress);
                }
            }, token);

            token.throwIfCancelled();
            counts[0] = content.length();
            counts[1] = content.length();
            publishProgress(ImportProgress.saving(importId, title, counts[1]));

            token.throwIfCancelled();
            repository.add(new Document(title, content, Instant.now()), replaceExisting);
            publishProgress(ImportProgress.completed(importId, title, counts[1]));
        } catch (OperationCancelledException ignored) {
            publishProgress(ImportProgress.cancelled(importId, title, counts[0], counts[1]));
        } catch (InvalidDocumentException | DuplicateTitleException | DocumentStorageException failure) {
            failImport(importId, title, counts, Objects.toString(failure.getMessage(), "Import failed"));
        } catch (RuntimeException failure) {
            LOGGER.log(Level.SEVERE, "Unexpected error while importing", failure);
            failImport(importId, title, counts, "Unexpected error while importing: "
                    + Objects.toString(failure.getMessage(), failure.getClass().getSimpleName()));
        } finally {
            activeImports.remove(importId);
        }
    }

    private void publishProgress(ImportProgress progress) {
        publish(() -> listeners.forEach(listener -> listener.onImportProgress(progress)));
    }

    private void failImport(long id, String title, long[] counts, String message) {
        ImportProgress progress = ImportProgress.failed(id, title, counts[0], counts[1], message);
        publish(() -> {
            listeners.forEach(listener -> listener.onImportProgress(progress));
            listeners.forEach(listener -> listener.onImportFailed(progress));
        });
    }

    private void publish(Runnable notification) {
        uiDispatcher.dispatch(() -> {
            if (!shutDown.get()) {
                notification.run();
            }
        });
    }

    private List<DocumentSummary> toSummaries(List<Document> sortedSnapshot) {
        return sortedSnapshot.stream()
                .map(document -> new DocumentSummary(document,
                        snippetExtractor.preview(document.content(), AppConstants.PREVIEW_LENGTH)))
                .toList();
    }
}