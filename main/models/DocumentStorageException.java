package models;

/**
 * Thrown when reading or writing documents on disk fails for an I/O reason (permission denied,
 * disk full, corrupt persisted entry, target directory missing...).
 *
 * <p>Implementations wrap the original {@link java.io.IOException} as the cause and write a
 * message that names the file and the problem, e.g.
 * {@code Could not write "C:\out\a.txt": access denied.}</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public class DocumentStorageException extends DocumentException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message user-presentable message
     */
    public DocumentStorageException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param message user-presentable message
     * @param cause   the underlying I/O exception
     */
    public DocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
