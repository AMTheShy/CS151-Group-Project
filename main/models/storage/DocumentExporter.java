package models.storage;

import java.nio.file.Path;

import models.CancellationToken;
import models.Document;
import models.DocumentStorageException;
import models.OperationCancelledException;

/**
 * Writes a document snapshot to a user-chosen file (UC5, D8).
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>The written bytes are exactly the ASCII bytes of {@code document.content()}.</li>
 *   <li>An existing target is overwritten (the view asked the user for confirmation before).</li>
 *   <li>The parent directory must exist; it is not created.</li>
 *   <li>On failure or cancellation no partially written target is left behind; a pre-existing
 *       target is left untouched (write to a temporary file in the same directory, then move).</li>
 * </ul>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member A
 * ({@code models.storage.FileSystemDocumentExporter}). Used by Member C's library controller.</p>
 *
 * <p>Threading: Implementations must be stateless or thread-safe. Never call on the EDT.</p>
 */
public interface DocumentExporter {

    /**
     * Writes the document to the target path.
     *
     * @param document the snapshot to write, not {@code null}
     * @param target   the destination file, not {@code null}
     * @param token    checked at least once per {@code AppConstants.IO_BUFFER_SIZE_BYTES}, not {@code null}
     * @throws NullPointerException        if an argument is {@code null}
     * @throws DocumentStorageException    if writing failed; the message names the target and the cause
     * @throws OperationCancelledException if the token was cancelled
     */
    void export(Document document, Path target, CancellationToken token) throws DocumentStorageException;
}
