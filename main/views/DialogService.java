package views;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * All modal user interaction of the application (file choosers, input and confirmation dialogs,
 * error dialogs) behind one interface, so the workflows and panels can be unit-tested with a
 * scripted fake and without a display.
 *
 * <p><b>Owner:</b> Member D (module-internal interface of {@code views}; not part of the frozen
 * contract). Production implementation: {@link SwingDialogService}. Test double:
 * {@code test/views/ScriptedDialogService}.</p>
 *
 * <p>Every method shows a <em>modal dialog</em> (never a second frame, U1) and returns after the
 * user closed it. "Cancel" or closing the dialog is reported as {@code Optional.empty()} /
 * {@code false}.</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public interface DialogService {

    /**
     * Lets the user pick a file to import ({@code JFileChooser}, files only, single selection).
     *
     * @return the chosen file, or empty if cancelled
     */
    Optional<Path> chooseFileToImport();

    /**
     * Asks for the document title (D1), pre-filled with {@code suggestedTitle}. If
     * {@code errorMessage} is present it is shown above the input field (used when re-asking
     * after a validation error).
     *
     * @param suggestedTitle the initial text
     * @param errorMessage   optional validation error from the previous attempt
     * @return the raw text entered, or empty if cancelled
     */
    Optional<String> askTitle(String suggestedTitle, Optional<String> errorMessage);

    /**
     * Asks whether an existing document may be replaced (D2).
     *
     * @param title the conflicting title
     * @return {@code true} if the user chose "Overwrite"
     */
    boolean confirmOverwriteDocument(String title);

    /**
     * Shows the delete confirmation listing all titles in a size-bounded scroll pane (D7).
     *
     * @param titles the titles to delete, in display order
     * @return {@code true} if the user confirmed
     */
    boolean confirmDelete(List<String> titles);

    /**
     * Asks for a search query (UC2). If {@code errorMessage} is present it is shown above the field.
     *
     * @param initialText  text to pre-fill (the previous invalid input, or empty)
     * @param errorMessage optional validation error from the previous attempt
     * @return the raw query text, or empty if cancelled
     */
    Optional<String> askSearchQuery(String initialText, Optional<String> errorMessage);

    /**
     * Lets the user choose the download target ({@code JFileChooser} save dialog, D8).
     *
     * @param suggestedFileName the default file name, e.g. {@code Notes.txt}
     * @return the chosen path, or empty if cancelled
     */
    Optional<Path> chooseDownloadTarget(String suggestedFileName);

    /**
     * Asks whether an existing file may be overwritten by a download (D8).
     *
     * @param target the existing file
     * @return {@code true} if the user confirmed
     */
    boolean confirmOverwriteFile(Path target);

    /**
     * Shows an error dialog ({@code JOptionPane.ERROR_MESSAGE}).
     *
     * @param title   dialog title, e.g. {@code Import failed}
     * @param message the message, possibly long (wrapped)
     */
    void showError(String title, String message);

    /**
     * Shows an information dialog.
     *
     * @param title   dialog title
     * @param message the message
     */
    void showInfo(String title, String message);
}
