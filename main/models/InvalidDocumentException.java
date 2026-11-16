package models;

import java.util.Objects;

/**
 * Thrown when a file cannot become a document because it violates the content rules (D3):
 * it contains a non-ASCII byte, is larger than {@link AppConstants#MAX_FILE_SIZE_BYTES}, or is not
 * a regular file.
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). Thrown by {@code models.storage.DocumentLoader}.</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public class InvalidDocumentException extends DocumentException {

    private static final long serialVersionUID = 1L;

    /** Machine-readable reason, so tests and the controller do not have to parse messages. */
    public enum Reason {
        /** A byte outside 0x00..0x7F was found. */
        NON_ASCII_CONTENT,
        /** The file is larger than the size limit. */
        FILE_TOO_LARGE,
        /** The path does not denote a regular file (missing, directory, device...). */
        NOT_A_REGULAR_FILE
    }

    /** Why the document is invalid. */
    private final Reason reason;

    /**
     * Creates the exception.
     *
     * @param reason  the reason, not {@code null}
     * @param message user-presentable message, e.g. {@code "The file contains a non-ASCII byte at position 17."}
     */
    public InvalidDocumentException(Reason reason, String message) {
        super(message);
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    /**
     * Returns the reason.
     *
     * @return the reason, never {@code null}
     */
    public Reason reason() {
        return reason;
    }
}
