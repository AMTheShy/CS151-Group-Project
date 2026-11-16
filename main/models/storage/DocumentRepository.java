package models.storage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;
import models.DuplicateTitleException;
import models.LoadSummary;
import models.OperationCancelledException;
import models.ProgressListener;

/**
 * The single authority on which documents exist: an in-memory index of immutable
 * {@link Document}s keyed by title, kept in sync with the read-only copies on disk (UC1, UC3,
 * D1, D2, D4, D5).
 *
 * <p><b>Consistency model (write-ahead to disk):</b> every mutation first changes the persisted
 * copy (through a {@link DocumentStore}) and only then the in-memory index. If the disk operation
 * fails, the in-memory state is unchanged and the method throws
 * {@link DocumentStorageException}. Therefore, what is in memory is always also on disk.</p>
 *
 * <p><b>Atomicity:</b> mutations ({@link #add}, {@link #removeAll}, the insertions of
 * {@link #loadPersisted}) are serialized: the check "does the title exist?" and the following
 * insert/replace happen atomically with respect to every other mutation. Readers never block
 * and always see either the old or the new document for a title, never a mixture.</p>
 *
 * <p><b>Snapshots:</b> {@link #listAllSortedByTitle()} returns an immutable list that never
 * changes afterwards, even when the repository changes.</p>
 *
 * <p><b>Notifications:</b> after each successful mutation that changed something, every
 * registered {@link RepositoryListener} receives exactly one
 * {@link RepositoryListener#onDocumentsChanged(List)} with the new sorted snapshot. Notifications
 * are delivered on the mutating thread, while mutations are still serialized, so listeners see
 * snapshots in the same order as the mutations happened. No notification is sent for a failed or
 * no-op mutation.</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member A
 * ({@code models.storage.DefaultDocumentRepository}). Used by Member C's controllers.</p>
 *
 * <p>Threading: Implementations must be thread-safe. All methods may be called from any thread
 * except the EDT for the methods doing disk I/O ({@link #add}, {@link #removeAll},
 * {@link #loadPersisted}). The read methods are non-blocking and may be called on the EDT.</p>
 */
public interface DocumentRepository {

    /**
     * Adds a document, or replaces the document with the same title (D1, D2).
     *
     * <p>Steps, performed atomically with respect to other mutations: (1) if a document with the
     * same title exists and {@code replaceExisting} is {@code false}, throw
     * {@link DuplicateTitleException}; (2) persist the read-only copy (replacing any old copy);
     * (3) publish {@code document} in memory; (4) notify listeners.</p>
     *
     * @param document        the new document, not {@code null}
     * @param replaceExisting {@code true} if the user confirmed overwriting an existing document
     *                        with the same title; {@code true} also succeeds when no such document
     *                        exists
     * @return the replaced document if one existed, otherwise empty
     * @throws NullPointerException     if {@code document} is {@code null}
     * @throws DuplicateTitleException  if the title is taken and {@code replaceExisting} is {@code false}
     * @throws DocumentStorageException if the read-only copy could not be written; nothing changed
     */
    Optional<Document> add(Document document, boolean replaceExisting)
            throws DuplicateTitleException, DocumentStorageException;

    /**
     * Looks up a document by its exact, case-sensitive title.
     *
     * @param title the title, not {@code null}
     * @return the current document with this title, or empty
     * @throws NullPointerException if {@code title} is {@code null}
     */
    Optional<Document> findByTitle(String title);

    /**
     * Tells whether a document with exactly this title exists (case-sensitive).
     *
     * @param title the title, not {@code null}
     * @return {@code true} if present at the moment of the call
     * @throws NullPointerException if {@code title} is {@code null}
     */
    boolean containsTitle(String title);

    /**
     * Returns all documents sorted by {@code AppConstants.DOCUMENT_TITLE_ORDER} (D6).
     *
     * @return an immutable snapshot; empty if there are no documents; never {@code null}
     */
    List<Document> listAllSortedByTitle();

    /**
     * Returns the number of documents.
     *
     * @return the current count
     */
    int size();

    /**
     * Removes the documents with the given titles from disk and from memory (UC3).
     *
     * <p>Titles that do not exist (for example already deleted concurrently) are ignored. Each
     * title is removed from disk first and then from memory. If deleting the copy of a title fails,
     * that document stays in the repository, the remaining titles are still processed, listeners
     * are notified once about the successful removals, and finally a
     * {@link DocumentStorageException} naming the failed titles is thrown.</p>
     *
     * @param titles the titles to remove, not {@code null}, no {@code null} elements
     * @return the titles that were actually removed, in the iteration order of {@code titles}
     * @throws NullPointerException     if {@code titles} or an element is {@code null}
     * @throws DocumentStorageException if at least one persisted copy could not be deleted
     */
    List<String> removeAll(Collection<String> titles) throws DocumentStorageException;

    /**
     * Loads the documents persisted by previous runs into memory (D5). Intended to be called once,
     * at startup, on a background thread.
     *
     * <p>For each persisted title (in {@code TITLE_ORDER}): check the token; if a document with
     * this title is already in memory (the user imported it while loading), skip it - the newer
     * in-memory document wins; otherwise load it and insert it. Corrupt or unreadable entries are
     * recorded in {@link LoadSummary#problems()} and skipped; they do not abort the load. Progress
     * is reported in documents: {@code progress.onProgress(processed, total)} after each entry.
     * Listeners are notified after each inserted document, or in batches (implementation choice),
     * but at least once at the end if anything was inserted.</p>
     *
     * @param progress receives (processed entries, total entries), not {@code null}
     * @param token    checked before each entry, not {@code null}
     * @return what was loaded, skipped and failed
     * @throws NullPointerException        if an argument is {@code null}
     * @throws DocumentStorageException    if the storage directory itself cannot be read
     * @throws OperationCancelledException if the token was cancelled; documents inserted before
     *                                     the cancellation stay in the repository
     */
    LoadSummary loadPersisted(ProgressListener progress, CancellationToken token) throws DocumentStorageException;

    /**
     * Registers a listener. Registering the same listener twice has no additional effect.
     *
     * @param listener the listener, not {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    void addRepositoryListener(RepositoryListener listener);

    /**
     * Unregisters a listener; unknown listeners are ignored.
     *
     * @param listener the listener, not {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    void removeRepositoryListener(RepositoryListener listener);
}
