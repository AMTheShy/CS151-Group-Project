package models;

/**
 * Thrown by {@link CancellationToken#throwIfCancelled()} to abort a cancelled operation.
 *
 * <p>Unchecked on purpose: cancellation is not an error of the operation, it unwinds the stack of
 * the worker thread and is caught by the controller that requested it, which then reports
 * CANCELLED (imports) or stays silent (searches). Code in models must never catch it except to
 * clean up and re-throw (see {@code CONVENTIONS.md}, "Exceptions").</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public class OperationCancelledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message a short description, for logs
     */
    public OperationCancelledException(String message) {
        super(message);
    }
}
