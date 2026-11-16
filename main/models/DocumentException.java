package models;

/**
 * Root of the checked exceptions of the document domain.
 *
 * <p>Checked exceptions are used for <em>expected, recoverable</em> failures that the user must
 * be told about (invalid file, duplicate title, disk error). Programming errors (null arguments,
 * violated preconditions) use unchecked {@link NullPointerException} /
 * {@link IllegalArgumentException} / {@link IllegalStateException}. Cancellation uses the
 * unchecked {@link OperationCancelledException}. See {@code CONVENTIONS.md}, "Exceptions".</p>
 *
 * <p>The message of every subclass must be a complete English sentence that can be shown to the
 * user as is in an error dialog.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public abstract class DocumentException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message user-presentable message
     */
    protected DocumentException(String message) {
        super(message);
    }

    /**
     * Creates the exception with a cause.
     *
     * @param message user-presentable message
     * @param cause   the underlying exception, for logs
     */
    protected DocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
