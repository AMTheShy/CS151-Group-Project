package views;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

/**
 * Scripted {@link DialogService} for headless view tests: the test enqueues the answers the "user"
 * will give, and the service records every question it was asked.
 *
 * <p><b>Owner:</b> Member D (test helper of the views module; not part of the frozen contract).</p>
 *
 * <p><b>Required behavior:</b></p>
 * <ul>
 *   <li>Per dialog kind keep a FIFO queue of answers ({@code Optional<Path>} for choosers,
 *       {@code Optional<String>} for inputs, {@code Boolean} for confirmations), filled by methods
 *       such as {@code answerFileToImport(Optional<Path>)}, {@code answerTitle(Optional<String>)},
 *       {@code answerConfirmOverwriteDocument(boolean)}, {@code answerConfirmDelete(boolean)},
 *       {@code answerSearchQuery(Optional<String>)}, {@code answerDownloadTarget(Optional<Path>)},
 *       {@code answerConfirmOverwriteFile(boolean)}.</li>
 *   <li>Every call is recorded as a {@link Question} (kind + arguments) in {@link #questions()}.</li>
 *   <li>A call without a queued answer fails the test with {@code AssertionError} naming the
 *       dialog kind (an unexpected dialog is a bug).</li>
 *   <li>{@code showError}/{@code showInfo} only record.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Not thread-safe; EDT / test thread only.</p>
 */
public final class ScriptedDialogService implements DialogService {

    /**
     * One recorded dialog.
     *
     * @param kind      the {@link DialogService} method name, e.g. {@code askTitle}
     * @param arguments the arguments, rendered with {@code String.valueOf}
     */
    public record Question(String kind, List<String> arguments) {
    }

    /** Recorded dialogs in call order. */
    private final List<Question> questions = new ArrayList<>();

    /** Scripted answers for {@link #chooseFileToImport()}. */
    private final Deque<Optional<Path>> fileToImportAnswers = new ArrayDeque<>();

    /** Creates a service with no scripted answers. */
    public ScriptedDialogService() {
        // empty
    }

    /**
     * Returns all recorded dialogs.
     *
     * @return immutable copy
     */
    public List<Question> questions() {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Scripts the next answer of {@link #chooseFileToImport()}. (Add one such method per dialog kind.)
     *
     * @param answer the chosen file or empty for "Cancel"
     * @return this service
     */
    public ScriptedDialogService answerFileToImport(Optional<Path> answer) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public Optional<Path> chooseFileToImport() {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public Optional<String> askTitle(String suggestedTitle, Optional<String> errorMessage) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public boolean confirmOverwriteDocument(String title) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public boolean confirmDelete(List<String> titles) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public Optional<String> askSearchQuery(String initialText, Optional<String> errorMessage) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public Optional<Path> chooseDownloadTarget(String suggestedFileName) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public boolean confirmOverwriteFile(Path target) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public void showError(String title, String message) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    @Override
    public void showInfo(String title, String message) {
        throw new UnsupportedOperationException("TODO: Member D");
    }
}
