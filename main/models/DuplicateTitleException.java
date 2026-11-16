package models;

import java.util.Objects;

/**
 * Thrown when a document is added without permission to replace and a document with the same
 * title already exists (D2, including the race where another import took the title between the
 * user's confirmation and the commit).
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). Thrown by
 * {@code models.storage.DocumentRepository#add}.</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public class DuplicateTitleException extends DocumentException {

    private static final long serialVersionUID = 1L;

    /** The title that is already taken. */
    private final String title;

    /**
     * Creates the exception with the standard message
     * {@code A document titled "<title>" already exists.}
     *
     * @param title the conflicting title, not {@code null}
     */
    public DuplicateTitleException(String title) {
        super("A document titled \"" + Objects.requireNonNull(title, "title") + "\" already exists.");
        this.title = title;
    }

    /**
     * Returns the conflicting title.
     *
     * @return the title
     */
    public String title() {
        return title;
    }
}
