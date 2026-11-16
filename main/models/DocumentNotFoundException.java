package models;

import java.util.Objects;

/**
 * Thrown when a persisted document that was expected to exist cannot be found, for example by
 * {@code models.storage.DocumentStore#load} when the entry vanished between listing and loading.
 *
 * <p>Lookups in the repository use {@code Optional} instead of this exception; it is reserved for
 * situations where absence is unexpected.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable after construction, thread-safe.</p>
 */
public class DocumentNotFoundException extends DocumentException {

    private static final long serialVersionUID = 1L;

    /** The title that was not found. */
    private final String title;

    /**
     * Creates the exception with the message {@code No document titled "<title>" was found.}
     *
     * @param title the missing title, not {@code null}
     */
    public DocumentNotFoundException(String title) {
        super("No document titled \"" + Objects.requireNonNull(title, "title") + "\" was found.");
        this.title = title;
    }

    /**
     * Returns the missing title.
     *
     * @return the title
     */
    public String title() {
        return title;
    }
}
