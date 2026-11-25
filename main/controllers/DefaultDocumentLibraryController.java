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

/**
 * Implements the library use cases asynchronously and reports everything to
 * {@link LibraryListener}s on the EDT (UC1, UC3, UC5, D1-D5, D8, U2).
 *
 * <p><b>Owner:</b> Member C (Controllers, Concurrency &amp; Integration).</p>
 *
 * <p><b>Collaborators (all injected interfaces):</b> {@link DocumentRepository} (state),
 * {@link DocumentLoader} (read user files), {@link DocumentExporter} (downloads),
 * {@link SnippetExtractor} (previews only), {@link TaskScheduler} (threads),
 * {@link UiDispatcher} (EDT hand-off). It never imports Swing.</p>
 *
 * <p><b>Construction:</b> the constructor stores its arguments and registers one private
 * {@code RepositoryListener} on the repository (lambda calling {@link #onRepositoryChanged}); this
 * is the only side effect allowed in the constructor.</p>
 *
 * <p><b>Import pipeline (per {@link #importDocument}):</b></p>
 * <ol>
 *   <li>On the caller (EDT): {@code DocumentTitles.requireValid(title)}; reject if shut down;
 *       {@code id = nextImportId.getAndIncrement()}; create a {@link SimpleCancellationToken} and put
 *       it in {@link #activeImports}; dispatch {@code ImportProgress.queued(id, title)}; submit the
 *       task to the import pool; return {@code id}. Total EDT time: microseconds.</li>
 *   <li>On the import thread: {@code content = loader.load(source, progressListener, token)} where
 *       the progress listener dispatches {@code ImportProgress.reading(...)} (throttle to at most
 *       one dispatch per 1% or per buffer to avoid flooding the EDT).</li>
 *   <li>Dispatch SAVING; {@code repository.add(new Document(title, content, Instant.now()), replaceExisting)}.
 *       The repository's own notification updates the document list.</li>
 *   <li>Dispatch COMPLETED.</li>
 *   <li>Failures: {@code InvalidDocumentException}, {@code DuplicateTitleException},
 *       {@code DocumentStorageException} &rarr; FAILED with {@code e.getMessage()};
 *       {@code OperationCancelledException} &rarr; CANCELLED; any other {@code RuntimeException}
 *       &rarr; log SEVERE and FAILED with {@code Unexpected error while importing: <message>}
 *       (never swallow, C-15). For FAILED, dispatch {@code onImportProgress(failed)} then
 *       {@code onImportFailed(failed)} in the same EDT task.</li>
 *   <li>{@code finally}: remove the token from {@link #activeImports}.</li>
 * </ol>
 *
 * <p><b>Document list:</b> {@link #onRepositoryChanged} (worker thread) converts the snapshot to
 * {@link DocumentSummary}s with {@code snippetExtractor.preview(content, AppConstants.PREVIEW_LENGTH)},
 * stores them in the volatile {@link #summaries} and dispatches {@code onDocumentsChanged}.
 * {@link #currentDocuments()} returns {@link #summaries} (no I/O, EDT-safe).</p>
 *
 * <p><b>Delete / export / startup load</b> run on the background pool. Export uses the given
 * {@code Document} snapshot, never re-reads the repository (D8). Startup load is guarded by
 * {@link #startupLoadStarted} (second call is ignored) and maps progress to
 * {@code onStartupLoadProgress}, the result to {@code onStartupLoadCompleted}, a
 * {@code DocumentStorageException} to {@code onStartupLoadFailed}.</p>
 *
 * <p><b>After shutdown</b> ({@link #cancelAll()} was called): methods that start work throw
 * {@code IllegalStateException}; dispatched tasks check {@link #shutDown} on the EDT and drop
 * themselves, so no listener is called after shutdown.</p>
 *
 * <p><b>Threading:</b> Thread-safe. Public methods are called on the EDT and never block.</p>
 *
 * <p><b>Implementation notes:</b> Reading progress is published when the integer percentage
 * increases. Export and startup load check the shutdown flag through their cancellation tokens.
 * If submitting an import fails, its token is removed before the failure is propagated.</p>
 */
public final class DefaultDocumentLibraryController implements DocumentLibraryController {

    /** Logger for unexpected failures. */
    private static final Logger LOGGER =
            Logger.getLogger(DefaultDocumentLibraryController.class.getName());

    /** Authoritative document state. */
    private final DocumentRepository repository;
    /** Reads and validates user files. */
    private final DocumentLoader loader;
    /** Writes downloads. */
    private final DocumentExporter exporter;
    /** Computes table previews. */
    private final SnippetExtractor snippetExtractor;
    /** Runs all background work. */
    private final TaskScheduler scheduler;
    /** Delivers callbacks on the EDT. */
    private final UiDispatcher uiDispatcher;

    /** View observers; copy-on-write because they rarely change. */
    private final CopyOnWriteArrayList<LibraryListener> listeners = new CopyOnWriteArrayList<>();
    /** Source of unique, positive import ids. */
    private final AtomicLong nextImportId = new AtomicLong(1);
    /** Cancellation tokens of imports that have not finished, keyed by import id. */
    private final Map<Long, SimpleCancellationToken> activeImports = new ConcurrentHashMap<>();
    /** Ensures the startup load runs at most once. */
    private final AtomicBoolean startupLoadStarted = new AtomicBoolean();
    /** Set by {@link #cancelAll()}; checked before starting work and before every callback. */
    private final AtomicBoolean shutDown = new AtomicBoolean();

    /** Latest document list with previews; replaced atomically on each repository change. */
    private volatile List<DocumentSummary> summaries = List.of();

    /**
     * Creates the controller and subscribes to repository changes.
     *
     * @param repository       the repository, not {@code null}
     * @param loader           the file loader, not {@code null}
     * @param exporter         the exporter, not {@code null}
     * @param snippetExtractor used for previews, not {@code null}
     * @param scheduler        the task scheduler, not {@code null}
     * @param uiDispatcher     the EDT dispatcher, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
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

    /** {@inheritDoc} Test: C-16. */
    @Override
    public void addLibraryListener(LibraryListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    /** {@inheritDoc} Test: C-16. */
    @Override
    public void removeLibraryListener(LibraryListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /** {@inheritDoc} Delegates to {@code repository.containsTitle}. */
    @Override
    public boolean isTitleTaken(String title) {
        return repository.containsTitle(Objects.requireNonNull(title, "title"));
    }

    /** {@inheritDoc} See "Import pipeline" above. Tests: C-01..C-08, C-15. */
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

    /**
     * {@inheritDoc}
     *
     * <p>Copies the titles, submits {@code repository.removeAll(copy)} to the background pool; on
     * {@code DocumentStorageException} dispatches {@code onDeleteFailed(e.getMessage())}. Test: C-09.</p>
     */
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

    /**
     * {@inheritDoc}
     *
     * <p>Submits {@code exporter.export(document, target, token)} to the background pool and
     * dispatches {@code onExportCompleted} or {@code onExportFailed(document, target, message)}.
     * Test: C-10.</p>
     */
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

    /** {@inheritDoc} Tests: C-11, C-12, C-14. */
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

    /** {@inheritDoc} Test: C-13. */
    @Override
    public List<DocumentSummary> currentDocuments() {
        return summaries;
    }

    /**
     * Cancels all running imports and the startup load, and blocks all further callbacks.
     * Called by {@link DefaultLifecycleController#shutdown()}. Idempotent. Test: C-17.
     */
    public void cancelAll() {
        if (shutDown.compareAndSet(false, true)) {
            activeImports.values().forEach(SimpleCancellationToken::cancel);
            activeImports.clear();
        }
    }

    /** Rejects new work after shutdown. */
    private void requireRunning() {
        if (shutDown.get() || scheduler.isShutdown()) {
            throw new IllegalStateException("Document library is shut down");
        }
    }

    /**
     * Repository callback (worker thread, under the repository's mutation lock - must be fast):
     * builds summaries, stores them, dispatches {@code onDocumentsChanged}.
     *
     * @param sortedSnapshot the new repository snapshot
     */
    private void onRepositoryChanged(List<Document> sortedSnapshot) {
        List<DocumentSummary> updated = toSummaries(sortedSnapshot);
        summaries = updated;
        publish(() -> listeners.forEach(listener -> listener.onDocumentsChanged(updated)));
    }

    /**
     * Executes one import on an import-pool thread (steps 2-6 of the pipeline).
     *
     * @param importId        the import id
     * @param source          the user's file
     * @param title           the valid title
     * @param replaceExisting overwrite flag confirmed by the user
     * @param token           this import's cancellation token
     */
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

    /** Queues an import progress update on the UI thread. */
    private void publishProgress(ImportProgress progress) {
        publish(() -> listeners.forEach(listener -> listener.onImportProgress(progress)));
    }

    /** Queues the failed progress state and its notification together. */
    private void failImport(long id, String title, long[] counts, String message) {
        ImportProgress progress = ImportProgress.failed(id, title, counts[0], counts[1], message);
        publish(() -> {
            listeners.forEach(listener -> listener.onImportProgress(progress));
            listeners.forEach(listener -> listener.onImportFailed(progress));
        });
    }

    /**
     * Dispatches a listener notification to the EDT; the task does nothing if the controller was
     * shut down by the time it runs.
     *
     * @param notification calls one method on every listener
     */
    private void publish(Runnable notification) {
        uiDispatcher.dispatch(() -> {
            if (!shutDown.get()) {
                notification.run();
            }
        });
    }

    /**
     * Converts a repository snapshot to table rows with previews.
     *
     * @param sortedSnapshot the documents, sorted
     * @return immutable summaries in the same order
     */
    private List<DocumentSummary> toSummaries(List<Document> sortedSnapshot) {
        return sortedSnapshot.stream()
                .map(document -> new DocumentSummary(document,
                        snippetExtractor.preview(document.content(), AppConstants.PREVIEW_LENGTH)))
                .toList();
    }
}