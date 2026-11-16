package models.storage;

import java.nio.file.Path;

import models.CancellationToken;
import models.DocumentStorageException;
import models.InvalidDocumentException;
import models.OperationCancelledException;
import models.ProgressListener;

/**
 * Reads a user-chosen file into a String while validating it (UC1, D3, U2).
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>The path must denote a regular, readable file.</li>
 *   <li>The size must be at most {@code AppConstants.MAX_FILE_SIZE_BYTES}; this is checked before
 *       reading <em>and</em> enforced while reading (the file could grow).</li>
 *   <li>Every byte must be in 0x00..0x7F; the first offending byte aborts the read.</li>
 *   <li>An empty file is valid and yields {@code ""}.</li>
 *   <li>Bytes are decoded one-to-one as US-ASCII; line breaks ({@code \r\n}, {@code \n},
 *       {@code \r}) are preserved verbatim.</li>
 * </ul>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member A
 * ({@code models.storage.StreamingDocumentLoader}). Used by Member C's library controller.</p>
 *
 * <p>Threading: Implementations must be stateless or thread-safe; several imports call
 * {@link #load} concurrently. Never call on the EDT.</p>
 */
public interface DocumentLoader {

    /**
     * Reads and validates a file.
     *
     * <p>Progress is reported in bytes: {@code progress.onProgress(bytesRead, totalBytes)} at
     * least once per {@code AppConstants.IO_BUFFER_SIZE_BYTES} read and once at the end. The token
     * is checked at least once per buffer.</p>
     *
     * @param source   the file to read, not {@code null}
     * @param progress receives byte progress, not {@code null}
     * @param token    cancellation token, not {@code null}
     * @return the full content
     * @throws NullPointerException        if an argument is {@code null}
     * @throws InvalidDocumentException    if the file is not a regular file, too large, or not ASCII;
     *                                     the message names the problem (e.g. the offset of the
     *                                     first non-ASCII byte)
     * @throws DocumentStorageException    if an I/O error occurs
     * @throws OperationCancelledException if the token was cancelled
     */
    String load(Path source, ProgressListener progress, CancellationToken token)
            throws InvalidDocumentException, DocumentStorageException;
}
