package models.storage;

import java.util.List;

import models.Document;
import models.DocumentNotFoundException;
import models.DocumentStorageException;

/**
 * Persistence of the read-only document copies in the application data directory (D4, D5).
 *
 * <p>This abstraction exists so that {@link DocumentRepository} (concurrency, atomicity,
 * notifications) can be unit-tested with an in-memory fake store, and the file-system details
 * (file naming, read-only attribute, atomic moves) can be tested separately (SRP, DIP).</p>
 *
 * <p>Requirements for implementations:</p>
 * <ul>
 *   <li>The content is stored byte-for-byte (ASCII), line breaks unchanged.</li>
 *   <li>Stored content files are marked read-only.</li>
 *   <li>File names must not be derived naively from titles: titles are case-sensitive while
 *       Windows and macOS file systems are not, and titles may contain characters that are
 *       illegal in file names ({@code / \ : * ? " < > |}).</li>
 *   <li>{@link #save} replaces an existing entry for the same title so that a crash leaves either
 *       the old or the new entry readable, never a silently truncated one.</li>
 * </ul>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member A
 * ({@code models.storage.FileSystemDocumentStore}). Used only by the repository implementation.</p>
 *
 * <p>Threading: Implementations need not be thread-safe for concurrent calls on the
 * <em>same</em> title; the repository serializes mutations. Concurrent {@link #load} calls for
 * different titles must be safe.</p>
 */
public interface DocumentStore {

    /**
     * Persists a document, replacing any existing entry with the same title.
     *
     * @param document the document, not {@code null}
     * @throws NullPointerException     if {@code document} is {@code null}
     * @throws DocumentStorageException if writing failed; the previous entry (if any) is still intact
     */
    void save(Document document) throws DocumentStorageException;

    /**
     * Deletes the entry for a title. Deleting a title that is not stored does nothing.
     *
     * @param title the title, not {@code null}
     * @throws NullPointerException     if {@code title} is {@code null}
     * @throws DocumentStorageException if the entry exists but could not be deleted
     */
    void delete(String title) throws DocumentStorageException;

    /**
     * Lists the titles of all complete entries, ignoring temporary or incomplete files.
     *
     * @return an immutable list sorted by {@code AppConstants.TITLE_ORDER}; empty if none
     * @throws DocumentStorageException if the storage directory cannot be read or created
     */
    List<String> listStoredTitles() throws DocumentStorageException;

    /**
     * Loads one entry.
     *
     * @param title the title, not {@code null}
     * @return the document with its original {@code importedAt}
     * @throws NullPointerException      if {@code title} is {@code null}
     * @throws DocumentNotFoundException if no entry exists for the title
     * @throws DocumentStorageException  if the entry exists but is unreadable or corrupt
     */
    Document load(String title) throws DocumentNotFoundException, DocumentStorageException;
}
