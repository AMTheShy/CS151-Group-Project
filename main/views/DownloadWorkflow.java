package views;

import java.util.Objects;

import controllers.DocumentLibraryController;
import models.Document;

/**
 * The interactive part of the reader's Download button (UC5, D8).
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Sequence (required):</b></p>
 * <ol>
 *   <li>{@code target = dialogs.chooseDownloadTarget(document.title() + AppConstants.EXPORT_FILE_EXTENSION)};
 *       empty &rarr; return false.</li>
 *   <li>If {@code Files.exists(target)} and {@code !dialogs.confirmOverwriteFile(target)} &rarr;
 *       return false. ({@code Files.exists} is a metadata check of microseconds and is the one
 *       file-system call allowed on the EDT; reading or writing content is not.)</li>
 *   <li>{@code controller.exportDocument(document, target)}; return true. Success and failure are
 *       reported later through {@code LibraryListener.onExportCompleted/onExportFailed}; the main
 *       frame shows the error dialog with the specific message.</li>
 * </ol>
 * <p>The {@link Document} passed in is the snapshot the user is reading, so the download works
 * even if that document was deleted or replaced meanwhile (D8).</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class DownloadWorkflow {

    /** Starts the background export. */
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
    public DownloadWorkflow(DocumentLibraryController libraryController, DialogService dialogs) {
        this.libraryController = Objects.requireNonNull(libraryController, "libraryController");
        this.dialogs = Objects.requireNonNull(dialogs, "dialogs");
    }

    /**
     * Runs the sequence above.
     *
     * @param document the snapshot to download, not {@code null}
     * @return {@code true} if an export was started
     * @throws NullPointerException if {@code document} is {@code null}
     */
    public boolean run(Document document) {
        throw new UnsupportedOperationException("TODO: Member D");
    }
}
