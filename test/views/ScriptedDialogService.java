package views;

import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
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
 *   <li>An absent {@code errorMessage} argument is recorded as {@code ""}.</li>
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

    /** Scripted answers for {@link #askTitle(String, Optional)}. */
    private final Deque<Optional<String>> titleAnswers = new ArrayDeque<>();

    /** Scripted answers for {@link #confirmOverwriteDocument(String)}. */
    private final Deque<Boolean> confirmOverwriteDocumentAnswers = new ArrayDeque<>();

    /** Scripted answers for {@link #confirmDelete(List)}. */
    private final Deque<Boolean> confirmDeleteAnswers = new ArrayDeque<>();

    /** Scripted answers for {@link #askSearchQuery(String, Optional)}. */
    private final Deque<Optional<String>> searchQueryAnswers = new ArrayDeque<>();

    /** Scripted answers for {@link #chooseDownloadTarget(String)}. */
    private final Deque<Optional<Path>> downloadTargetAnswers = new ArrayDeque<>();

    /** Scripted answers for {@link #confirmOverwriteFile(Path)}. */
    private final Deque<Boolean> confirmOverwriteFileAnswers = new ArrayDeque<>();

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
        return List.copyOf(questions);
    }

    /**
     * Returns the recorded dialogs of one kind, in call order.
     *
     * @param kind the {@link DialogService} method name, e.g. {@code askTitle}
     * @return immutable list, possibly empty
     */
    public List<Question> questionsOf(String kind) {
        Objects.requireNonNull(kind, "kind");
        return questions.stream().filter(question -> question.kind().equals(kind)).toList();
    }

    /**
     * Scripts the next answer of {@link #chooseFileToImport()}.
     *
     * @param answer the chosen file or empty for "Cancel"
     * @return this service
     */
    public ScriptedDialogService answerFileToImport(Optional<Path> answer) {
        fileToImportAnswers.add(Objects.requireNonNull(answer, "answer"));
        return this;
    }

    /**
     * Scripts the next answer of {@link #askTitle(String, Optional)}.
     *
     * @param answer the typed text or empty for "Cancel"
     * @return this service
     */
    public ScriptedDialogService answerTitle(Optional<String> answer) {
        titleAnswers.add(Objects.requireNonNull(answer, "answer"));
        return this;
    }

    /**
     * Scripts the next answer of {@link #confirmOverwriteDocument(String)}.
     *
     * @param answer {@code true} for "Overwrite"
     * @return this service
     */
    public ScriptedDialogService answerConfirmOverwriteDocument(boolean answer) {
        confirmOverwriteDocumentAnswers.add(answer);
        return this;
    }

    /**
     * Scripts the next answer of {@link #confirmDelete(List)}.
     *
     * @param answer {@code true} for "Delete"
     * @return this service
     */
    public ScriptedDialogService answerConfirmDelete(boolean answer) {
        confirmDeleteAnswers.add(answer);
        return this;
    }

    /**
     * Scripts the next answer of {@link #askSearchQuery(String, Optional)}.
     *
     * @param answer the typed query or empty for "Cancel"
     * @return this service
     */
    public ScriptedDialogService answerSearchQuery(Optional<String> answer) {
        searchQueryAnswers.add(Objects.requireNonNull(answer, "answer"));
        return this;
    }

    /**
     * Scripts the next answer of {@link #chooseDownloadTarget(String)}.
     *
     * @param answer the chosen target or empty for "Cancel"
     * @return this service
     */
    public ScriptedDialogService answerDownloadTarget(Optional<Path> answer) {
        downloadTargetAnswers.add(Objects.requireNonNull(answer, "answer"));
        return this;
    }

    /**
     * Scripts the next answer of {@link #confirmOverwriteFile(Path)}.
     *
     * @param answer {@code true} for "Overwrite"
     * @return this service
     */
    public ScriptedDialogService answerConfirmOverwriteFile(boolean answer) {
        confirmOverwriteFileAnswers.add(answer);
        return this;
    }

    @Override
    public Optional<Path> chooseFileToImport() {
        record("chooseFileToImport");
        return next(fileToImportAnswers, "chooseFileToImport");
    }

    @Override
    public Optional<String> askTitle(String suggestedTitle, Optional<String> errorMessage) {
        record("askTitle", suggestedTitle, errorMessage.orElse(""));
        return next(titleAnswers, "askTitle");
    }

    @Override
    public boolean confirmOverwriteDocument(String title) {
        record("confirmOverwriteDocument", title);
        return next(confirmOverwriteDocumentAnswers, "confirmOverwriteDocument");
    }

    @Override
    public boolean confirmDelete(List<String> titles) {
        record("confirmDelete", titles.toArray());
        return next(confirmDeleteAnswers, "confirmDelete");
    }

    @Override
    public Optional<String> askSearchQuery(String initialText, Optional<String> errorMessage) {
        record("askSearchQuery", initialText, errorMessage.orElse(""));
        return next(searchQueryAnswers, "askSearchQuery");
    }

    @Override
    public Optional<Path> chooseDownloadTarget(String suggestedFileName) {
        record("chooseDownloadTarget", suggestedFileName);
        return next(downloadTargetAnswers, "chooseDownloadTarget");
    }

    @Override
    public boolean confirmOverwriteFile(Path target) {
        record("confirmOverwriteFile", target);
        return next(confirmOverwriteFileAnswers, "confirmOverwriteFile");
    }

    @Override
    public void showError(String title, String message) {
        record("showError", title, message);
    }

    @Override
    public void showInfo(String title, String message) {
        record("showInfo", title, message);
    }

    /**
     * Records one dialog. An absent optional error is recorded as the empty string.
     *
     * @param kind      the method name
     * @param arguments the arguments
     */
    private void record(String kind, Object... arguments) {
        List<String> rendered = new ArrayList<>();
        for (Object argument : arguments) {
            rendered.add(String.valueOf(argument));
        }
        questions.add(new Question(kind, List.copyOf(rendered)));
    }

    /**
     * Takes the next scripted answer.
     *
     * @param answers the queue
     * @param kind    the dialog kind, for the failure message
     * @param <T>     answer type
     * @return the answer
     * @throws AssertionError if no answer was scripted
     */
    private static <T> T next(Deque<T> answers, String kind) {
        if (answers.isEmpty()) {
            throw new AssertionError("Unexpected dialog: " + kind + " (no scripted answer)");
        }
        return answers.poll();
    }
}
