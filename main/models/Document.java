package models;

import java.time.Instant;
import java.util.Objects;

/**
 * An imported, immutable text document (UC1, D4).
 *
 * <p>A {@code Document} is the in-memory read-only copy of an imported file. Once created it
 * never changes: replacing a document with the same title creates a <em>new</em>
 * {@code Document} object, so anyone holding a reference (a search result, the reader, a
 * pending download) keeps a consistent snapshot (S5, D8).</p>
 *
 * <p>Invariants:</p>
 * <ul>
 *   <li>{@code title} satisfies {@link DocumentTitles#requireValid(String)}.</li>
 *   <li>{@code content} is not {@code null}. It is expected to be pure ASCII; this is guaranteed
 *       by the loader ({@code models.storage.DocumentLoader}) and is <em>not</em> re-checked here
 *       for performance reasons (a 20 MB check per construction would be wasted work).</li>
 *   <li>{@code importedAt} is not {@code null}.</li>
 * </ul>
 *
 * <p>Equality: two documents are equal if and only if their titles and contents are equal;
 * {@code importedAt} is deliberately ignored so that a document re-loaded from disk equals the
 * document that was saved.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 */
public final class Document {

    /** Unique, case-sensitive key of the document (D1). */
    private final String title;

    /** Full ASCII text exactly as imported; line breaks are preserved verbatim (D3). */
    private final String content;

    /** The moment the document was imported into the system. */
    private final Instant importedAt;

    /**
     * Creates a document.
     *
     * @param title      a normalized, valid title (see {@link DocumentTitles})
     * @param content    the full text, not {@code null}; may be empty
     * @param importedAt when the document was imported, not {@code null}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IllegalArgumentException if {@code title} is not normalized or not valid
     */
    public Document(String title, String content, Instant importedAt) {
        this.title = DocumentTitles.requireValid(title);
        this.content = Objects.requireNonNull(content, "content");
        this.importedAt = Objects.requireNonNull(importedAt, "importedAt");
    }

    /**
     * Returns the unique title.
     *
     * @return the title, never {@code null}
     */
    public String title() {
        return title;
    }

    /**
     * Returns the full content.
     *
     * @return the content, never {@code null}
     */
    public String content() {
        return content;
    }

    /**
     * Returns the import time.
     *
     * @return the import instant, never {@code null}
     */
    public Instant importedAt() {
        return importedAt;
    }

    /**
     * Returns the number of characters of the content (equal to the number of bytes on disk
     * because the content is ASCII).
     *
     * @return {@code content().length()}
     */
    public int length() {
        return content.length();
    }

    /**
     * Two documents are equal if their titles and contents are equal.
     *
     * @param other the object to compare with
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Document)) {
            return false;
        }
        Document that = (Document) other;
        return title.equals(that.title) && content.equals(that.content);
    }

    /**
     * Hash code consistent with {@link #equals(Object)}.
     *
     * @return hash of title and content
     */
    @Override
    public int hashCode() {
        return Objects.hash(title, content);
    }

    /**
     * Returns a short description; never includes the (possibly multi-megabyte) content.
     *
     * @return for example {@code Document[title=Notes, length=1234]}
     */
    @Override
    public String toString() {
        return "Document[title=" + title + ", length=" + content.length() + "]";
    }
}
