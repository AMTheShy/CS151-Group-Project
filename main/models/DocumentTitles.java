package models;

import java.util.Objects;
import java.util.Optional;

/**
 * The single source of truth for title normalization and validation (D1, decision N2).
 *
 * <p>Rules for a valid title:</p>
 * <ol>
 *   <li>The raw input is normalized with {@link String#trim()} (leading and trailing characters
 *       {@code <= ' '} are removed).</li>
 *   <li>The normalized title is not empty.</li>
 *   <li>The normalized title has at most {@link AppConstants#MAX_TITLE_LENGTH} characters.</li>
 *   <li>Every character is printable ASCII ({@link AsciiText#isPrintableAscii(char)}).</li>
 * </ol>
 *
 * <p>Titles are unique keys compared case-sensitively ({@code "Foo"} and {@code "foo"} are two
 * different titles). Use {@link AppConstants#TITLE_ORDER} for sorting.</p>
 *
 * <p>Typical use: the import dialog calls {@link #validate(String)} to show an error before it
 * closes; the controller and {@link Document} call {@link #requireValid(String)} as a guard.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe (stateless static methods).</p>
 */
public final class DocumentTitles {

    private DocumentTitles() {
        throw new AssertionError("DocumentTitles is a utility class and must not be instantiated");
    }

    /**
     * Normalizes raw user input into a candidate title.
     *
     * @param rawTitle the text typed by the user, not {@code null}
     * @return {@code rawTitle.trim()}
     * @throws NullPointerException if {@code rawTitle} is {@code null}
     */
    public static String normalize(String rawTitle) {
        Objects.requireNonNull(rawTitle, "rawTitle");
        return rawTitle.trim();
    }

    /**
     * Validates raw user input.
     *
     * @param rawTitle the text typed by the user, not {@code null}; it is normalized first
     * @return an empty {@code Optional} if {@code normalize(rawTitle)} is a valid title, otherwise
     *         a human-readable English error message suitable for an error dialog
     * @throws NullPointerException if {@code rawTitle} is {@code null}
     */
    public static Optional<String> validate(String rawTitle) {
        String title = normalize(rawTitle);
        if (title.isEmpty()) {
            return Optional.of("Title must not be empty.");
        }
        if (title.length() > AppConstants.MAX_TITLE_LENGTH) {
            return Optional.of("Title must be at most " + AppConstants.MAX_TITLE_LENGTH
                    + " characters long (it has " + title.length() + ").");
        }
        for (int index = 0; index < title.length(); index++) {
            if (!AsciiText.isPrintableAscii(title.charAt(index))) {
                return Optional.of("Title may contain only printable ASCII characters "
                        + "(invalid character at position " + (index + 1) + ").");
            }
        }
        return Optional.empty();
    }

    /**
     * Tells whether raw user input normalizes to a valid title.
     *
     * @param rawTitle the text typed by the user, not {@code null}
     * @return {@code validate(rawTitle).isEmpty()}
     * @throws NullPointerException if {@code rawTitle} is {@code null}
     */
    public static boolean isValid(String rawTitle) {
        return validate(rawTitle).isEmpty();
    }

    /**
     * Guards an already-normalized title.
     *
     * @param title a title that must already be normalized and valid, not {@code null}
     * @return {@code title} unchanged, for fluent use in constructors
     * @throws NullPointerException     if {@code title} is {@code null}
     * @throws IllegalArgumentException if {@code title} is not normalized
     *                                  ({@code !title.equals(normalize(title))}) or is invalid;
     *                                  the message is the one of {@link #validate(String)}
     */
    public static String requireValid(String title) {
        Objects.requireNonNull(title, "title");
        if (!title.equals(normalize(title))) {
            throw new IllegalArgumentException("Title must not have leading or trailing whitespace: \""
                    + title + "\"");
        }
        Optional<String> error = validate(title);
        if (error.isPresent()) {
            throw new IllegalArgumentException(error.get());
        }
        return title;
    }
}
