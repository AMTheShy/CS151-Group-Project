package models;

import java.util.Objects;
import java.util.Optional;

/**
 * A validated, normalized search query (UC2, S1, S2).
 *
 * <p>Normalization (S2): only leading and trailing whitespace is removed with
 * {@link String#trim()}. Internal whitespace is kept exactly as typed, because a space in the
 * query must match exactly one space character in the document (S1).</p>
 *
 * <p>Validation: the normalized text must not be empty and must be pure ASCII (non-ASCII is a
 * Non-goal and could never match an ASCII document anyway).</p>
 *
 * <p>The query also carries {@link #foldedText()}, the ASCII-lower-cased text, so that the search
 * engine does not have to fold the query again for every document. Because ASCII folding keeps
 * the length, {@code foldedText().length() == text().length()} (S2).</p>
 *
 * <p>Instances are created only through {@link #of(String)}.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 */
public final class SearchQuery {

    /** The normalized query text as typed (case preserved). */
    private final String text;

    /** {@link AsciiText#toLowerAscii(String)} of {@link #text}. */
    private final String foldedText;

    private SearchQuery(String text) {
        this.text = text;
        this.foldedText = AsciiText.toLowerAscii(text);
    }

    /**
     * Validates raw user input without creating a query; used by the view to show an error
     * message before starting a search.
     *
     * @param rawQuery the text typed by the user, not {@code null}
     * @return an empty {@code Optional} if the input is acceptable, otherwise an English error
     *         message suitable for a dialog
     * @throws NullPointerException if {@code rawQuery} is {@code null}
     */
    public static Optional<String> validate(String rawQuery) {
        Objects.requireNonNull(rawQuery, "rawQuery");
        String normalized = rawQuery.trim();
        if (normalized.isEmpty()) {
            return Optional.of("Search query must not be empty.");
        }
        if (!AsciiText.isAscii(normalized)) {
            return Optional.of("Search query may contain only ASCII characters.");
        }
        return Optional.empty();
    }

    /**
     * Creates a query from raw user input (S2).
     *
     * @param rawQuery the text typed by the user, not {@code null}
     * @return the normalized query
     * @throws NullPointerException     if {@code rawQuery} is {@code null}
     * @throws IllegalArgumentException if {@link #validate(String)} reports an error; the
     *                                  exception message is that error
     */
    public static SearchQuery of(String rawQuery) {
        Optional<String> error = validate(rawQuery);
        if (error.isPresent()) {
            throw new IllegalArgumentException(error.get());
        }
        return new SearchQuery(rawQuery.trim());
    }

    /**
     * Returns the normalized query text with the original letter case.
     *
     * @return the query text, never empty
     */
    public String text() {
        return text;
    }

    /**
     * Returns the ASCII lower-cased query text, same length as {@link #text()}.
     *
     * @return the folded query text
     */
    public String foldedText() {
        return foldedText;
    }

    /**
     * Returns the number of characters of the query; every match has exactly this length.
     *
     * @return {@code text().length()}, always {@code >= 1}
     */
    public int length() {
        return text.length();
    }

    /**
     * Two queries are equal if their normalized texts are equal (case-sensitive).
     *
     * @param other the object to compare with
     * @return {@code true} if equal
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof SearchQuery)) {
            return false;
        }
        return text.equals(((SearchQuery) other).text);
    }

    /**
     * Hash code consistent with {@link #equals(Object)}.
     *
     * @return hash of the text
     */
    @Override
    public int hashCode() {
        return text.hashCode();
    }

    /**
     * Returns a debug representation.
     *
     * @return for example {@code SearchQuery["computer algorithm"]}
     */
    @Override
    public String toString() {
        return "SearchQuery[\"" + text + "\"]";
    }
}
