package models;

/**
 * Life-cycle states of one asynchronous import (U2).
 *
 * <p>Allowed transitions: {@code QUEUED -> READING -> SAVING -> COMPLETED}; any non-terminal
 * state may go to {@code FAILED} or {@code CANCELLED}. Terminal states never change again.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 */
public enum ImportState {

    /** Accepted by the controller, waiting for a free import thread. */
    QUEUED,

    /** The file is being read and validated; {@code bytesRead} grows. */
    READING,

    /** The content is valid and is being written to the read-only copy and the repository. */
    SAVING,

    /** The document is in the repository. Terminal. */
    COMPLETED,

    /** The import failed; an error message is available. Terminal. */
    FAILED,

    /** The import was aborted, for example because the application is shutting down. Terminal. */
    CANCELLED;

    /**
     * Tells whether this is a terminal state.
     *
     * @return {@code true} for {@code COMPLETED}, {@code FAILED} and {@code CANCELLED}
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
