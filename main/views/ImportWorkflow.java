package views;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import controllers.DocumentLibraryController;
import models.DocumentTitles;
import views.support.TitleSuggester;

/**
 * The interactive part of File &rarr; Import... (UC1, D1, D2, U2): everything that needs the user
 * happens here on the EDT; the actual import is handed to the controller and runs in the
 * background.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Sequence (required):</b></p>
 * <ol>
 *   <li>{@code dialogs.chooseFileToImport()}; empty &rarr; return empty.</li>
 *   <li>{@code suggestion = TitleSuggester.suggestTitle(file)}; loop:
 *       {@code raw = dialogs.askTitle(suggestion-or-previous-input, error)}; empty &rarr; return empty;
 *       {@code error = DocumentTitles.validate(raw)}; repeat until valid.
 *       {@code title = DocumentTitles.normalize(raw)}.</li>
 *   <li>{@code replace = false}; if {@code controller.isTitleTaken(title)}:
 *       {@code if (!dialogs.confirmOverwriteDocument(title)) return empty; replace = true;} (D2)</li>
 *   <li>{@code return OptionalLong.of(controller.importDocument(file, title, replace))} - returns at
 *       once; progress appears on the left via {@code LibraryListener}.</li>
 * </ol>
 * <p>The file is <em>not</em> read or checked here (no I/O on the EDT, U6); size and ASCII errors
 * come back asynchronously as FAILED progress.</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class ImportWorkflow {

    /** Starts the background import. */
    private final DocumentLibraryController libraryController;

    /** Shows the dialogs. */
    private final DialogService dialogs;

    /**
     * Creates the workflow.
     *
     * @param libraryController the library controller, not {@code null}
     * @param dialogs           the dialog service, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public ImportWorkflow(DocumentLibraryController libraryController, DialogService dialogs) {
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
    }

    /**
     * Runs the sequence above.
     *
     * @return the import id if an import was started, otherwise empty (user cancelled)
     */
    public OptionalLong run() {
        Optional<Path> chosen = dialogs.chooseFileToImport();
        if (chosen.isEmpty()) {
            return OptionalLong.empty();
        }
        Path file = chosen.get();
        String input = TitleSuggester.suggestTitle(file);
        Optional<String> error = Optional.empty();
        while (true) {
            Optional<String> raw = dialogs.askTitle(input, error);
            if (raw.isEmpty()) {
                return OptionalLong.empty();
            }
            input = raw.get();
            error = DocumentTitles.validate(input);
            if (error.isEmpty()) {
                break;
            }
        }
        String title = DocumentTitles.normalize(input);
        boolean replace = false;
        if (libraryController.isTitleTaken(title)) {
            if (!dialogs.confirmOverwriteDocument(title)) {
                return OptionalLong.empty();
            }
            replace = true;
        }
        return OptionalLong.of(libraryController.importDocument(file, title, replace));
    }
}
