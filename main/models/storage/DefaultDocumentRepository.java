package models.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import models.AppConstants;
import models.CancellationToken;
import models.Document;
import models.DocumentNotFoundException;
import models.DocumentStorageException;
import models.DuplicateTitleException;
import models.LoadSummary;
import models.ProgressListener;

/**
 * Thread-safe implementation of {@link DocumentRepository}: an in-memory, title-sorted index of
 * immutable documents, persisted write-ahead through a {@link DocumentStore}.
 *
 * <p><b>Owner:</b> Member A (Storage &amp; File I/O).</p>
 *
 * <p><b>Requirements covered:</b> UC1 (store), UC3 (list, delete), D1 (unique case-sensitive
 * titles), D2 (duplicate handling and the commit-time race), D4 (read-only copy via the store),
 * D5 (startup load), D6 (sorted snapshots), S5 (immutable snapshots for searches).</p>
 *
 * <p><b>What it does NOT do:</b> no file reading of user files (that is {@link DocumentLoader}),
 * no thread creation, no UI notification (listeners are told on the mutating thread; the
 * controller moves to the EDT).</p>
 *
 * <p><b>Required internal design</b> (this is the design the team agreed on; deviations need
 * Eric's approval):</p>
 * <ul>
 *   <li>State: a {@code ConcurrentSkipListMap<String, Document>} ordered by
 *       {@link AppConstants#TITLE_ORDER}. Lock-free reads ({@link #findByTitle},
 *       {@link #containsTitle}, {@link #listAllSortedByTitle}, {@link #size}) read the map directly.
 *       {@code listAllSortedByTitle()} returns {@code List.copyOf(documents.values())} - an
 *       immutable snapshot, O(n) per call, which is fine for hundreds of documents.</li>
 *   <li>All mutations (add, remove, the insert step of the startup load) run while holding
 *       {@link #mutationLock}. Inside the lock: check, write to the store, update the map, notify
 *       listeners. Holding the lock during the store write (about 10 ms for 5 MB) serializes
 *       imports at the commit step only; reading the user's file happens earlier, without the lock.</li>
 *   <li>Notifications are sent while still holding the lock, so listeners receive snapshots in
 *       mutation order (design doc, race R9). Listeners are therefore required to be fast and must
 *       not call back into mutating methods; if a listener throws, log it with
 *       {@code Level.WARNING} and continue with the next listener - never let it break the
 *       mutation.</li>
 *   <li>Write-ahead: store first, memory second. On {@link DocumentStorageException} from the
 *       store, the map is unchanged and no notification is sent.</li>
 * </ul>
 *
 * <p><b>Startup load interleaving (race R6):</b> {@link #loadPersisted} reads each entry from the
 * store <em>without</em> holding the lock (slow I/O), then takes the lock and inserts it only if
 * the title is still absent ({@code putIfAbsent} semantics). A document imported by the user while
 * loading therefore always wins, and is counted in {@link LoadSummary#skippedCount()}.</p>
 *
 * <p><b>Threading:</b> Thread-safe. Methods doing store I/O must not be called on the EDT.</p>
 */
public final class DefaultDocumentRepository implements DocumentRepository {

    /** Logger for listener failures and load problems. */
    private static final Logger LOGGER = Logger.getLogger(DefaultDocumentRepository.class.getName());

    /** Persistence of the read-only copies; injected so tests can use {@code FakeDocumentStore}. */
    private final DocumentStore store;

    /** The in-memory index, sorted by {@link AppConstants#TITLE_ORDER}; safe for lock-free reads. */
    private final ConcurrentSkipListMap<String, Document> documents =
            new ConcurrentSkipListMap<>(AppConstants.TITLE_ORDER);

    /** Serializes all mutations and their notifications. */
    private final ReentrantLock mutationLock = new ReentrantLock();

    /** Registered observers; copy-on-write because iteration (notify) is far more common than change. */
    private final CopyOnWriteArrayList<RepositoryListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Creates an empty repository backed by {@code store}. Does not touch the disk; call
     * {@link #loadPersisted} to load previous documents.
     *
     * @param store the persistence of the read-only copies, not {@code null}
     * @throws NullPointerException if {@code store} is {@code null}
     */
    public DefaultDocumentRepository(DocumentStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Implementation steps, all inside {@link #mutationLock}: (1) look up the title; if present
     * and {@code !replaceExisting}, throw {@link DuplicateTitleException} without touching the
     * store; (2) {@code store.save(document)}; (3) {@code documents.put}; (4) notify listeners;
     * (5) return the previous document. Tests: A-01..A-06, A-09, A-15, A-18.</p>
     */
    @Override
    public Optional<Document> add(Document document, boolean replaceExisting)
            throws DuplicateTitleException, DocumentStorageException {
        Objects.requireNonNull(document, "document");
        String title = document.title();
        mutationLock.lock();
        try {
            Document previous = documents.get(title);
            if (previous != null && !replaceExisting) {
                throw new DuplicateTitleException(title);
            }
            store.save(document);
            documents.put(title, document);
            notifyListeners();
            return Optional.ofNullable(previous);
        } finally {
            mutationLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lock-free map lookup. Test: A-01.</p>
     */
    @Override
    public Optional<Document> findByTitle(String title) {
        Objects.requireNonNull(title, "title");
        return Optional.ofNullable(documents.get(title));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Lock-free map lookup; case-sensitive (D1). Test: A-04.</p>
     */
    @Override
    public boolean containsTitle(String title) {
        Objects.requireNonNull(title, "title");
        return documents.containsKey(title);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code List.copyOf(documents.values())}; the map is already sorted. Tests: A-05, A-17.</p>
     */
    @Override
    public List<Document> listAllSortedByTitle() {
        return List.copyOf(documents.values());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns {@code documents.size()}. Test: A-16.</p>
     */
    @Override
    public int size() {
        return documents.size();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Inside the lock, for each title in order: skip if absent; else {@code store.delete(title)},
     * then {@code documents.remove(title)}; on a store failure remember the title and continue.
     * Notify once if at least one title was removed. Finally throw a
     * {@link DocumentStorageException} with message
     * {@code Could not delete "<t1>", "<t2>": <first cause message>.} if any failed.
     * Tests: A-07, A-08, A-09.</p>
     */
    @Override
    public List<String> removeAll(Collection<String> titles) throws DocumentStorageException {
        Objects.requireNonNull(titles, "titles");
        List<String> requested = List.copyOf(titles); // throws NullPointerException for null elements
        List<String> removed = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        DocumentStorageException firstFailure = null;
        mutationLock.lock();
        try {
            for (String title : requested) {
                if (!documents.containsKey(title)) {
                    continue;
                }
                try {
                    store.delete(title);
                    documents.remove(title);
                    removed.add(title);
                } catch (DocumentStorageException failure) {
                    LOGGER.log(Level.WARNING, "Could not delete \"" + title + "\"", failure);
                    failed.add(title);
                    if (firstFailure == null) {
                        firstFailure = failure;
                    }
                }
            }
            if (!removed.isEmpty()) {
                notifyListeners();
            }
        } finally {
            mutationLock.unlock();
        }
        if (firstFailure != null) {
            throw new DocumentStorageException("Could not delete " + quoteAll(failed) + ": "
                    + withoutFinalPeriod(firstFailure.getMessage()) + ".", firstFailure);
        }
        return List.copyOf(removed);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Steps: {@code titles = store.listStoredTitles()} (failure propagates as
     * {@link DocumentStorageException}); for each title: {@code token.throwIfCancelled()}; if
     * already in memory count as skipped; otherwise {@code store.load(title)} outside the lock
     * (a {@code DocumentNotFoundException} or {@code DocumentStorageException} is recorded as a
     * problem {@code "<title>: <message>"} and logged); then inside the lock insert if still absent
     * (else skipped) and notify; report {@code progress.onProgress(index + 1, titles.size())}.
     * Tests: A-11..A-14.</p>
     */
    @Override
    public LoadSummary loadPersisted(ProgressListener progress, CancellationToken token)
            throws DocumentStorageException {
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(token, "token");
        List<String> titles = store.listStoredTitles();
        int total = titles.size();
        int loadedCount = 0;
        int skippedCount = 0;
        List<String> problems = new ArrayList<>();
        for (int index = 0; index < total; index++) {
            token.throwIfCancelled();
            String title = titles.get(index);
            if (documents.containsKey(title)) {
                skippedCount++;
            } else {
                try {
                    Document persisted = store.load(title); // slow I/O, deliberately outside the lock (R6)
                    if (insertIfAbsent(persisted)) {
                        loadedCount++;
                    } else {
                        skippedCount++;
                    }
                } catch (DocumentNotFoundException | DocumentStorageException failure) {
                    LOGGER.log(Level.WARNING, "Skipping persisted document \"" + title + "\"", failure);
                    problems.add(title + ": " + failure.getMessage());
                }
            }
            progress.onProgress(index + 1, total);
        }
        return new LoadSummary(loadedCount, skippedCount, problems);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Adds only if not yet registered. Test: A-10.</p>
     */
    @Override
    public void addRepositoryListener(RepositoryListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.addIfAbsent(listener);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Test: A-10.</p>
     */
    @Override
    public void removeRepositoryListener(RepositoryListener listener) {
        Objects.requireNonNull(listener, "listener");
        listeners.remove(listener);
    }

    /**
     * Sends the current sorted snapshot to every listener. Must be called while holding
     * {@link #mutationLock}. A throwing listener is logged with {@code LOGGER.log(Level.WARNING, ...)}
     * and does not prevent the other listeners from being notified.
     */
    private void notifyListeners() {
        List<Document> snapshot = List.copyOf(documents.values());
        for (RepositoryListener listener : listeners) {
            try {
                listener.onDocumentsChanged(snapshot);
            } catch (RuntimeException failure) {
                LOGGER.log(Level.WARNING, "A repository listener failed; continuing with the others", failure);
            }
        }
    }

    /**
     * Inserts a persisted document under {@link #mutationLock} if its title is still absent, and
     * notifies listeners when it was inserted. A document the user imported meanwhile wins (R6).
     *
     * @param persisted the document loaded from the store
     * @return {@code true} if inserted, {@code false} if the title was taken in the meantime
     */
    private boolean insertIfAbsent(Document persisted) {
        mutationLock.lock();
        try {
            if (documents.putIfAbsent(persisted.title(), persisted) != null) {
                return false;
            }
            notifyListeners();
            return true;
        } finally {
            mutationLock.unlock();
        }
    }

    /**
     * Formats titles for messages: {@code "A", "B"}.
     *
     * @param titles the titles
     * @return the quoted, comma-separated titles
     */
    private static String quoteAll(List<String> titles) {
        StringJoiner joiner = new StringJoiner(", ");
        for (String title : titles) {
            joiner.add("\"" + title + "\"");
        }
        return joiner.toString();
    }

    /**
     * Removes one trailing period so that a full-sentence cause can be embedded in another sentence.
     *
     * @param message the message, possibly {@code null}
     * @return the message without a final period; empty text for {@code null}
     */
    private static String withoutFinalPeriod(String message) {
        if (message == null) {
            return "";
        }
        return message.endsWith(".") ? message.substring(0, message.length() - 1) : message;
    }
}
