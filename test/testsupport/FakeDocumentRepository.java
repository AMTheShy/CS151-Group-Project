package testsupport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import models.AppConstants;
import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;
import models.DuplicateTitleException;
import models.LoadSummary;
import models.ProgressListener;
import models.storage.DocumentRepository;
import models.storage.RepositoryListener;

/**
 * A simple, correct, thread-safe in-memory {@link DocumentRepository} with call recording and
 * failure injection. Member C uses it to test controllers before Member A's repository exists.
 *
 * <p>Semantics follow the {@code DocumentRepository} contract: atomic check-and-insert, immutable
 * sorted snapshots, one notification per successful mutation delivered while mutations are
 * serialized. It does not persist anything.</p>
 *
 * <p>Race simulation: {@link #setBeforeAddHook(Consumer)} runs a hook at the start of every
 * {@link #add} call, before the duplicate check and outside the lock, so a test can insert a
 * conflicting document "just before the commit" (D2).</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeDocumentRepository implements DocumentRepository {

    /** One recorded call of {@link #add}. */
    public record AddCall(Document document, boolean replaceExisting) {
    }

    private final ConcurrentSkipListMap<String, Document> documents =
            new ConcurrentSkipListMap<>(AppConstants.TITLE_ORDER);
    private final List<RepositoryListener> listeners = new CopyOnWriteArrayList<>();
    private final List<AddCall> addCalls = new CopyOnWriteArrayList<>();
    private final List<List<String>> removeCalls = new CopyOnWriteArrayList<>();
    private final Set<String> removalFailures = new HashSet<>();
    private final List<Document> persistedDocuments = new ArrayList<>();
    private final Object mutationLock = new Object();
    private volatile DocumentStorageException nextAddFailure;
    private volatile DocumentStorageException loadFailure;
    private volatile Consumer<Document> beforeAddHook = document -> { };
    private final AtomicInteger loadPersistedCalls = new AtomicInteger();

    /** Creates an empty repository. */
    public FakeDocumentRepository() {
        // empty
    }

    /**
     * Inserts a document without notification or recording (test arrangement).
     *
     * @param document the document
     */
    public void putDirectly(Document document) {
        synchronized (mutationLock) {
            documents.put(document.title(), document);
        }
    }

    /**
     * Makes the next {@link #add} call fail with {@code failure} (one-shot).
     *
     * @param failure the exception
     */
    public void failNextAdd(DocumentStorageException failure) {
        nextAddFailure = failure;
    }

    /**
     * Makes removal of {@code title} fail in {@link #removeAll}.
     *
     * @param title the title
     */
    public void failRemovalOf(String title) {
        synchronized (mutationLock) {
            removalFailures.add(title);
        }
    }

    /**
     * Sets what {@link #loadPersisted} will load.
     *
     * @param persisted documents "on disk"
     */
    public void setPersistedDocuments(List<Document> persisted) {
        synchronized (mutationLock) {
            persistedDocuments.clear();
            persistedDocuments.addAll(persisted);
            persistedDocuments.sort(AppConstants.DOCUMENT_TITLE_ORDER);
        }
    }

    /**
     * Makes {@link #loadPersisted} fail with {@code failure}.
     *
     * @param failure the exception, or {@code null} for success
     */
    public void failLoad(DocumentStorageException failure) {
        loadFailure = failure;
    }

    /**
     * Sets a hook that runs at the start of each {@link #add}, before the duplicate check.
     *
     * @param hook the hook, not {@code null}
     */
    public void setBeforeAddHook(Consumer<Document> hook) {
        beforeAddHook = Objects.requireNonNull(hook, "hook");
    }

    /**
     * Returns the recorded {@link #add} calls, including failed ones.
     *
     * @return immutable copy in call order
     */
    public List<AddCall> addCalls() {
        return List.copyOf(addCalls);
    }

    /**
     * Returns the recorded {@link #removeAll} arguments.
     *
     * @return immutable copy in call order
     */
    public List<List<String>> removeCalls() {
        return List.copyOf(removeCalls);
    }

    /**
     * Returns how often {@link #loadPersisted} was called.
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

    @Override
    public Optional<Document> add(Document document, boolean replaceExisting)
            throws DuplicateTitleException, DocumentStorageException {
        Objects.requireNonNull(document, "document");
        addCalls.add(new AddCall(document, replaceExisting));
        beforeAddHook.accept(document);
        synchronized (mutationLock) {
            DocumentStorageException failure = nextAddFailure;
            if (failure != null) {
                nextAddFailure = null;
                throw failure;
            }
            Document previous = documents.get(document.title());
            if (previous != null && !replaceExisting) {
                throw new DuplicateTitleException(document.title());
            }
            documents.put(document.title(), document);
            notifyListeners();
            return Optional.ofNullable(previous);
        }
    }

    @Override
    public Optional<Document> findByTitle(String title) {
        return Optional.ofNullable(documents.get(Objects.requireNonNull(title, "title")));
    }

    @Override
    public boolean containsTitle(String title) {
        return documents.containsKey(Objects.requireNonNull(title, "title"));
    }

    @Override
    public List<Document> listAllSortedByTitle() {
        return List.copyOf(documents.values());
    }

    @Override
    public int size() {
        return documents.size();
    }

    @Override
    public List<String> removeAll(Collection<String> titles) throws DocumentStorageException {
        Objects.requireNonNull(titles, "titles");
        List<String> requested = List.copyOf(titles);
        removeCalls.add(requested);
        synchronized (mutationLock) {
            List<String> removed = new ArrayList<>();
            List<String> failed = new ArrayList<>();
            for (String title : requested) {
                if (removalFailures.contains(title)) {
                    failed.add(title);
                } else if (documents.remove(title) != null) {
                    removed.add(title);
                }
            }
            if (!removed.isEmpty()) {
                notifyListeners();
            }
            if (!failed.isEmpty()) {
                throw new DocumentStorageException("Could not delete: " + String.join(", ", failed) + ".");
            }
            return List.copyOf(removed);
        }
    }

    @Override
    public LoadSummary loadPersisted(ProgressListener progress, CancellationToken token)
            throws DocumentStorageException {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(token, "token");
        loadPersistedCalls.incrementAndGet();
        if (loadFailure != null) {
            throw loadFailure;
        }
        List<Document> toLoad;
        synchronized (mutationLock) {
            toLoad = List.copyOf(persistedDocuments);
        }
        int loaded = 0;
        int skipped = 0;
        for (int index = 0; index < toLoad.size(); index++) {
            token.throwIfCancelled();
            Document document = toLoad.get(index);
            synchronized (mutationLock) {
                if (documents.putIfAbsent(document.title(), document) == null) {
                    loaded++;
                    notifyListeners();
                } else {
                    skipped++;
                }
            }
            progress.onProgress(index + 1, toLoad.size());
        }
        return new LoadSummary(loaded, skipped, List.of());
    }

    @Override
    public void addRepositoryListener(RepositoryListener listener) {
        Objects.requireNonNull(listener, "listener");
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeRepositoryListener(RepositoryListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    private void notifyListeners() {
        List<Document> snapshot = List.copyOf(documents.values());
        for (RepositoryListener listener : listeners) {
            listener.onDocumentsChanged(snapshot);
        }
    }
}
