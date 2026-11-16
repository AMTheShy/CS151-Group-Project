package testsupport;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import controllers.DocumentLibraryController;
import controllers.LibraryListener;
import models.Document;
import models.DocumentSummary;
import models.DocumentTitles;
import models.ImportProgress;
import models.LoadSummary;

/**
 * Recording {@link DocumentLibraryController} whose listener callbacks are fired manually by the
 * test. Used by Member D to test views without any real controller, thread or disk.
 *
 * <p>Requests ({@link #importDocument}, {@link #deleteDocuments}, ...) are only recorded; nothing
 * happens until the test calls one of the {@code fire...} methods, which invoke all listeners
 * synchronously on the calling thread. Swing tests should call them on the EDT (e.g. inside
 * {@code SwingUtilities.invokeAndWait}) to honor the "callbacks on the EDT" contract.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeDocumentLibraryController implements DocumentLibraryController {

    /** One recorded import request. */
    public record ImportRequest(long importId, Path source, String title, boolean replaceExisting) {
    }

    /** One recorded export request. */
    public record ExportRequest(Document document, Path target) {
    }

    private final List<LibraryListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ImportRequest> importRequests = new CopyOnWriteArrayList<>();
    private final List<List<String>> deleteRequests = new CopyOnWriteArrayList<>();
    private final List<ExportRequest> exportRequests = new CopyOnWriteArrayList<>();
    private final Set<String> takenTitles = ConcurrentHashMap.newKeySet();
    private final AtomicLong nextImportId = new AtomicLong(1);
    private final AtomicInteger loadPersistedCalls = new AtomicInteger();
    private volatile List<DocumentSummary> currentDocuments = List.of();

    /** Creates a controller with no documents. */
    public FakeDocumentLibraryController() {
        // empty
    }

    /**
     * Declares a title as taken for {@link #isTitleTaken}.
     *
     * @param title the title
     */
    public void markTitleTaken(String title) {
        takenTitles.add(title);
    }

    /**
     * Sets what {@link #currentDocuments()} returns, without notifying listeners.
     *
     * @param documents the summaries
     */
    public void setCurrentDocuments(List<DocumentSummary> documents) {
        currentDocuments = List.copyOf(documents);
    }

    /**
     * Returns the recorded import requests.
     *
     * @return immutable copy
     */
    public List<ImportRequest> importRequests() {
        return List.copyOf(importRequests);
    }

    /**
     * Returns the recorded delete requests.
     *
     * @return immutable copy
     */
    public List<List<String>> deleteRequests() {
        return List.copyOf(deleteRequests);
    }

    /**
     * Returns the recorded export requests.
     *
     * @return immutable copy
     */
    public List<ExportRequest> exportRequests() {
        return List.copyOf(exportRequests);
    }

    /**
     * Returns how often {@link #loadPersistedDocuments()} was called.
     *
     * @return the count
     */
    public int loadPersistedCalls() {
        return loadPersistedCalls.get();
    }

    /**
     * Returns the number of registered listeners.
     *
     * @return the count
     */
    public int listenerCount() {
        return listeners.size();
    }

    /**
     * Sets the current documents and fires {@link LibraryListener#onDocumentsChanged(List)}.
     *
     * @param documents the new list
     */
    public void fireDocumentsChanged(List<DocumentSummary> documents) {
        setCurrentDocuments(documents);
        for (LibraryListener listener : listeners) {
            listener.onDocumentsChanged(currentDocuments);
        }
    }

    /**
     * Fires {@link LibraryListener#onImportProgress(ImportProgress)}.
     *
     * @param progress the progress
     */
    public void fireImportProgress(ImportProgress progress) {
        for (LibraryListener listener : listeners) {
            listener.onImportProgress(progress);
        }
    }

    /**
     * Fires {@link LibraryListener#onImportFailed(ImportProgress)}.
     *
     * @param failed a FAILED progress
     */
    public void fireImportFailed(ImportProgress failed) {
        for (LibraryListener listener : listeners) {
            listener.onImportFailed(failed);
        }
    }

    /**
     * Fires {@link LibraryListener#onDeleteFailed(String)}.
     *
     * @param message the message
     */
    public void fireDeleteFailed(String message) {
        for (LibraryListener listener : listeners) {
            listener.onDeleteFailed(message);
        }
    }

    /**
     * Fires {@link LibraryListener#onExportCompleted(Document, Path)}.
     *
     * @param document the document
     * @param target   the target
     */
    public void fireExportCompleted(Document document, Path target) {
        for (LibraryListener listener : listeners) {
            listener.onExportCompleted(document, target);
        }
    }

    /**
     * Fires {@link LibraryListener#onExportFailed(Document, Path, String)}.
     *
     * @param document the document
     * @param target   the target
     * @param message  the message
     */
    public void fireExportFailed(Document document, Path target, String message) {
        for (LibraryListener listener : listeners) {
            listener.onExportFailed(document, target, message);
        }
    }

    /**
     * Fires {@link LibraryListener#onStartupLoadProgress(int, int)}.
     *
     * @param processed processed count
     * @param total     total count
     */
    public void fireStartupLoadProgress(int processed, int total) {
        for (LibraryListener listener : listeners) {
            listener.onStartupLoadProgress(processed, total);
        }
    }

    /**
     * Fires {@link LibraryListener#onStartupLoadCompleted(LoadSummary)}.
     *
     * @param summary the summary
     */
    public void fireStartupLoadCompleted(LoadSummary summary) {
        for (LibraryListener listener : listeners) {
            listener.onStartupLoadCompleted(summary);
        }
    }

    /**
     * Fires {@link LibraryListener#onStartupLoadFailed(String)}.
     *
     * @param message the message
     */
    public void fireStartupLoadFailed(String message) {
        for (LibraryListener listener : listeners) {
            listener.onStartupLoadFailed(message);
        }
    }

    @Override
    public void addLibraryListener(LibraryListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLibraryListener(LibraryListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    @Override
    public boolean isTitleTaken(String title) {
        return takenTitles.contains(Objects.requireNonNull(title, "title"));
    }

    @Override
    public long importDocument(Path source, String title, boolean replaceExisting) {
        Objects.requireNonNull(source, "source");
        DocumentTitles.requireValid(title);
        long importId = nextImportId.getAndIncrement();
        importRequests.add(new ImportRequest(importId, source, title, replaceExisting));
        return importId;
    }

    @Override
    public void deleteDocuments(Collection<String> titles) {
        Objects.requireNonNull(titles, "titles");
        deleteRequests.add(List.copyOf(titles));
    }

    @Override
    public void exportDocument(Document document, Path target) {
        exportRequests.add(new ExportRequest(Objects.requireNonNull(document, "document"),
                Objects.requireNonNull(target, "target")));
    }

    @Override
    public void loadPersistedDocuments() {
        loadPersistedCalls.incrementAndGet();
    }

    @Override
    public List<DocumentSummary> currentDocuments() {
        return currentDocuments;
    }
}
