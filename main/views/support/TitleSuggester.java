package views.support;

import java.nio.file.Path;
import java.util.Objects;

import models.AppConstants;
import models.AsciiText;

/**
 * Derives the default title offered in the import dialog from the chosen file (D1).
 *
 * <p><b>Owner:</b> Member B (pure display logic; placed in {@code views.support} because the views use
 * it - see developer guide, "Workload balancing").</p>
 *
 * <p><b>Rules, applied in order:</b></p>
 * <ol>
 *   <li>Take the last path element only ({@code path.getFileName().toString()}).</li>
 *   <li>Remove the last extension: everything from the last {@code '.'} on, unless that dot is
 *       the first character ({@code ".bashrc"} stays {@code ".bashrc"}) or there is no dot.
 *       {@code "archive.tar.gz"} &rarr; {@code "archive.tar"}; {@code "notes."} &rarr; {@code "notes"}.</li>
 *   <li>Replace every character that is not printable ASCII by {@code '_'} (so the suggestion is
 *       always acceptable to {@code DocumentTitles}).</li>
 *   <li>Trim; truncate to {@code AppConstants.MAX_TITLE_LENGTH}; trim again.</li>
 * </ol>
 * <p>The result may be empty (e.g. file name {@code "   .txt"}); the dialog then shows an empty
 * field and validation asks the user for a title.</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe (static, no I/O - only the path string is used).</p>
 */
public final class TitleSuggester {

    private TitleSuggester() {
        throw new AssertionError("TitleSuggester is a utility class and must not be instantiated");
    }

    /**
     * Suggests a title (B-52..B-56).
     *
     * @param file the chosen file, not {@code null}; it is not accessed
     * @return the suggested title, possibly empty, never {@code null}
     * @throws NullPointerException     if {@code file} is {@code null}
     * @throws IllegalArgumentException if {@code file} has no file name (e.g. a root)
     */
    public static String suggestTitle(Path file) {
        Objects.requireNonNull(file, "file");
        Path name = file.getFileName();
        if (name == null) {
            throw new IllegalArgumentException("file must have a file name: " + file);
        }
        String filename = name.toString();
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart > 0) {
            filename = filename.substring(0, extensionStart);
        }
        StringBuilder suggestion = new StringBuilder(filename.length());
        for (int index = 0; index < filename.length(); index++) {
            char character = filename.charAt(index);
            suggestion.append(AsciiText.isPrintableAscii(character) ? character : '_');
        }
        String trimmed = suggestion.toString().trim();
        return trimmed.substring(0, Math.min(trimmed.length(), AppConstants.MAX_TITLE_LENGTH)).trim();
    }
}
