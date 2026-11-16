package controllers;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import models.Document;
import models.DocumentSummary;

/**
 * Use-case facade for everything about the document library: import (UC1), manage/delete (UC3),
 * download (UC5) and the startup load (D5). The view talks only to this interface (Facade + DIP).
 *
 * <p><b>Asynchrony:</b> every method that does I/O returns immediately after scheduling the work
 * on a background thread; results arrive through {@link LibraryListener} callbacks on the EDT. No
 * method of this interface blocks the caller for more than a few microseconds, so all of them
 * may be called on the EDT (U2, U6).</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C
 * ({@code controllers.DefaultDocumentLibraryController}). Used by Member D's views.</p>
 *
 * <p>Threading: Thread-safe; intended to be called on the EDT.</p>
 */
public interface DocumentLibraryController {

    /**
     * Registers a listener; its callbacks run on the EDT.
     *
     * @param listener the listener, not {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    void addLibraryListener(LibraryListener listener);

    /**
     * Unregisters a listener; unknown listeners are ignored.
     *
     * @param listener the listener, not {@code null}
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    void removeLibraryListener(LibraryListener listener);

    /**
     * Tells whether a document with this exact title exists right now; used by the import dialog
     * to decide whether to ask "overwrite?" (D2). Non-blocking.
     *
     * @param title a normalized title, not {@code null}
     * @return {@code true} if the title is currently taken
     * @throws NullPointerException if {@code title} is {@code null}
     */
    boolean isTitleTaken(String title);

    /**
     * Starts an asynchronous import (UC1, D1-D4, U2) and returns immediately.
     *
     * <p>The listener sees, on the EDT: QUEUED, then READING updates, SAVING, then exactly one
     * terminal state. On success the document list changes. If the title is taken at commit time
     * and {@code replaceExisting} is {@code false}, the import FAILS (D2 race) - it never
     * overwrites silently.</p>
     *
     * @param source          the file chosen by the user, not {@code null}
     * @param title           a normalized, valid title (see {@code models.DocumentTitles})
     * @param replaceExisting {@code true} if the user confirmed overwriting an existing document
     * @return the id of the new import, used in every {@code ImportProgress} of this import
     * @throws NullPointerException     if an argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is not a normalized, valid title
     * @throws IllegalStateException    if the controller has been shut down
     */
    long importDocument(Path source, String title, boolean replaceExisting);

    /**
     * Starts the asynchronous deletion of documents (UC3, D7) and returns immediately. Titles
     * that no longer exist are ignored. On completion the document list changes; on a partial
     * failure {@link LibraryListener#onDeleteFailed(String)} is called as well.
     *
     * @param titles titles to delete, not {@code null}; an empty collection does nothing
     * @throws NullPointerException  if {@code titles} or an element is {@code null}
     * @throws IllegalStateException if the controller has been shut down
     */
    void deleteDocuments(Collection<String> titles);

    /**
     * Starts the asynchronous download of a document snapshot to a file (UC5, D8) and returns
     * immediately. The snapshot is written even if the document was deleted or replaced in the
     * meantime. The view has already confirmed overwriting an existing target.
     *
     * @param document the snapshot being read, not {@code null}
     * @param target   the destination file, not {@code null}
     * @throws NullPointerException  if an argument is {@code null}
     * @throws IllegalStateException if the controller has been shut down
     */
    void exportDocument(Document document, Path target);

    /**
     * Starts loading the persisted documents in the background (D5) and returns immediately.
     * Only the first call has an effect; later calls are ignored.
     *
     * @throws IllegalStateException if the controller has been shut down
     */
    void loadPersistedDocuments();

    /**
     * Returns the current documents with previews; used to initialize a view. Non-blocking.
     *
     * @return immutable list sorted by title (D6); empty if none
     */
    List<DocumentSummary> currentDocuments();
}
