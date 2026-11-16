package controllers;

import java.nio.file.Path;
import java.util.List;

import models.Document;
import models.DocumentSummary;
import models.ImportProgress;
import models.LoadSummary;

/**
 * Observer of the document library, implemented by views (UC1, UC3, UC5, D5, U2).
 *
 * <p><b>Every method is guaranteed to be called on the EDT</b>, in the order the events happened.
 * View code can therefore update Swing components directly. All methods have empty default
 * implementations so that a view component implements only the events it cares about (ISP).</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member D's views; invoked by Member C's
 * {@code DocumentLibraryController} implementation.</p>
 *
 * <p>Threading: EDT only (guaranteed by the caller).</p>
 */
public interface LibraryListener {

    /**
     * The set of documents changed (import, replace, delete, startup load).
     *
     * @param sortedDocuments immutable list of all documents with previews, sorted by title (D6)
     */
    default void onDocumentsChanged(List<DocumentSummary> sortedDocuments) {
    }

    /**
     * An import changed state or made progress. Called for every state, including the terminal
     * states COMPLETED, FAILED and CANCELLED (each terminal state exactly once per import).
     *
     * @param progress the latest progress snapshot
     */
    default void onImportProgress(ImportProgress progress) {
    }

    /**
     * An import failed. Called exactly once per failed import, right after the
     * {@link #onImportProgress(ImportProgress)} call carrying the FAILED state, so that the main
     * window can show an error dialog.
     *
     * @param failed the FAILED progress; {@code failed.errorMessage()} is present
     */
    default void onImportFailed(ImportProgress failed) {
    }

    /**
     * Some of the documents requested for deletion could not be deleted.
     *
     * @param message user-presentable message naming the documents and the cause
     */
    default void onDeleteFailed(String message) {
    }

    /**
     * A download (export) finished successfully.
     *
     * @param document the exported snapshot
     * @param target   the written file
     */
    default void onExportCompleted(Document document, Path target) {
    }

    /**
     * A download (export) failed.
     *
     * @param document the snapshot that should have been exported
     * @param target   the intended target file
     * @param message  user-presentable message
     */
    default void onExportFailed(Document document, Path target, String message) {
    }

    /**
     * Progress of loading the persisted documents at startup (D5).
     *
     * @param processedCount entries processed so far
     * @param totalCount     total number of persisted entries
     */
    default void onStartupLoadProgress(int processedCount, int totalCount) {
    }

    /**
     * Loading the persisted documents at startup finished (also called when it found nothing).
     *
     * @param summary counts and problems
     */
    default void onStartupLoadCompleted(LoadSummary summary) {
    }

    /**
     * Loading the persisted documents at startup failed completely (storage directory unreadable).
     *
     * @param message user-presentable message
     */
    default void onStartupLoadFailed(String message) {
    }
}
