package testsupport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

import models.AppConstants;
import models.Document;
import models.DocumentException;
import models.DocumentNotFoundException;
import models.DocumentStorageException;
import models.storage.DocumentStore;

/**
 * In-memory {@link DocumentStore} with failure injection and an operation log; used by Member A
 * to test {@code DefaultDocumentRepository} without touching the disk.
 *
 * <p>Operation log entries have the form {@code "save:<title>"}, {@code "delete:<title>"},
 * {@code "load:<title>"}, {@code "list"}; they let tests assert the write-ahead order.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe (all methods synchronized).</p>
 */
public final class FakeDocumentStore implements DocumentStore {

    private final Map<String, Document> entries = new TreeMap<>(AppConstants.TITLE_ORDER);
    private final Map<String, DocumentStorageException> saveFailures = new HashMap<>();
    private final Map<String, DocumentStorageException> deleteFailures = new HashMap<>();
    private final Map<String, DocumentException> loadFailures = new HashMap<>();
    private final List<String> operations = new ArrayList<>();
    private DocumentStorageException listingFailure;

    /** Creates an empty store. */
    public FakeDocumentStore() {
        // empty
    }

    /**
     * Pre-populates an entry without logging an operation (simulates a previous run).
     *
     * @param document the document to store
     */
    public synchronized void putDirectly(Document document) {
        entries.put(document.title(), document);
    }

    /**
     * Makes every future {@link #save} of {@code title} throw {@code failure}.
     *
     * @param title   the title
     * @param failure the exception to throw
     */
    public synchronized void failSave(String title, DocumentStorageException failure) {
        saveFailures.put(title, failure);
    }

    /**
     * Makes every future {@link #delete} of {@code title} throw {@code failure}.
     *
     * @param title   the title
     * @param failure the exception to throw
     */
    public synchronized void failDelete(String title, DocumentStorageException failure) {
        deleteFailures.put(title, failure);
    }

    /**
     * Makes every future {@link #load} of {@code title} throw {@code failure}.
     *
     * @param title   the title
     * @param failure a {@link DocumentNotFoundException} or {@link DocumentStorageException}
     */
    public synchronized void failLoad(String title, DocumentException failure) {
        if (!(failure instanceof DocumentNotFoundException) && !(failure instanceof DocumentStorageException)) {
            throw new IllegalArgumentException("load can only fail with not-found or storage exceptions");
        }
        loadFailures.put(title, failure);
    }

    /**
     * Makes every future {@link #listStoredTitles()} throw {@code failure}.
     *
     * @param failure the exception to throw, or {@code null} to stop failing
     */
    public synchronized void failListing(DocumentStorageException failure) {
        listingFailure = failure;
    }

    /**
     * Returns the operation log.
     *
     * @return an immutable copy
     */
    public synchronized List<String> operations() {
        return List.copyOf(operations);
    }

    /**
     * Returns the stored entry for a title.
     *
     * @param title the title
     * @return the entry or empty
     */
    public synchronized Optional<Document> stored(String title) {
        return Optional.ofNullable(entries.get(title));
    }

    /**
     * Returns the number of stored entries.
     *
     * @return the count
     */
    public synchronized int storedCount() {
        return entries.size();
    }

    @Override
    public synchronized void save(Document document) throws DocumentStorageException {
        Objects.requireNonNull(document, "document");
        operations.add("save:" + document.title());
        DocumentStorageException failure = saveFailures.get(document.title());
        if (failure != null) {
            throw failure;
        }
        entries.put(document.title(), document);
    }

    @Override
    public synchronized void delete(String title) throws DocumentStorageException {
        Objects.requireNonNull(title, "title");
        operations.add("delete:" + title);
        DocumentStorageException failure = deleteFailures.get(title);
        if (failure != null) {
            throw failure;
        }
        entries.remove(title);
    }

    @Override
    public synchronized List<String> listStoredTitles() throws DocumentStorageException {
        operations.add("list");
        if (listingFailure != null) {
            throw listingFailure;
        }
        return List.copyOf(entries.keySet());
    }

    @Override
    public synchronized Document load(String title) throws DocumentNotFoundException, DocumentStorageException {
        Objects.requireNonNull(title, "title");
        operations.add("load:" + title);
        DocumentException failure = loadFailures.get(title);
        if (failure instanceof DocumentNotFoundException) {
            throw (DocumentNotFoundException) failure;
        }
        if (failure instanceof DocumentStorageException) {
            throw (DocumentStorageException) failure;
        }
        Document document = entries.get(title);
        if (document == null) {
            throw new DocumentNotFoundException(title);
        }
        return document;
    }
}
