package testsupport;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import controllers.LibraryListener;
import models.Document;
import models.DocumentSummary;
import models.ImportProgress;
import models.LoadSummary;

/**
 * {@link LibraryListener} that records every callback (with its thread name) and lets tests wait
 * for a condition without {@code Thread.sleep}. Used by Member C.
 *
 * <p>Example: {@code listener.await(recorder -> recorder.isTerminal(importId), Duration.ofSeconds(5))}.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe (all state guarded by {@code this}; waiters use wait/notifyAll).</p>
 */
public final class RecordingLibraryListener implements LibraryListener {

    /** A recorded export outcome; {@code message} is empty for successes. */
    public record ExportEvent(Document document, Path target, boolean succeeded, String message) {
    }

    private final List<List<DocumentSummary>> documentChanges = new ArrayList<>();
    private final List<ImportProgress> progressEvents = new ArrayList<>();
    private final List<ImportProgress> importFailures = new ArrayList<>();
    private final List<String> deleteFailures = new ArrayList<>();
    private final List<ExportEvent> exportEvents = new ArrayList<>();
    private final List<int[]> startupProgress = new ArrayList<>();
    private final List<LoadSummary> startupCompletions = new ArrayList<>();
    private final List<String> startupFailures = new ArrayList<>();
    private final List<String> callbackThreads = new ArrayList<>();

    /** Creates the listener. */
    public RecordingLibraryListener() {
        // empty
    }

    private void recorded() {
        callbackThreads.add(Thread.currentThread().getName());
        notifyAll();
    }

    @Override
    public synchronized void onDocumentsChanged(List<DocumentSummary> sortedDocuments) {
        documentChanges.add(sortedDocuments);
        recorded();
    }

    @Override
    public synchronized void onImportProgress(ImportProgress progress) {
        progressEvents.add(progress);
        recorded();
    }

    @Override
    public synchronized void onImportFailed(ImportProgress failed) {
        importFailures.add(failed);
        recorded();
    }

    @Override
    public synchronized void onDeleteFailed(String message) {
        deleteFailures.add(message);
        recorded();
    }

    @Override
    public synchronized void onExportCompleted(Document document, Path target) {
        exportEvents.add(new ExportEvent(document, target, true, ""));
        recorded();
    }

    @Override
    public synchronized void onExportFailed(Document document, Path target, String message) {
        exportEvents.add(new ExportEvent(document, target, false, message));
        recorded();
    }

    @Override
    public synchronized void onStartupLoadProgress(int processedCount, int totalCount) {
        startupProgress.add(new int[] {processedCount, totalCount});
        recorded();
    }

    @Override
    public synchronized void onStartupLoadCompleted(LoadSummary summary) {
        startupCompletions.add(summary);
        recorded();
    }

    @Override
    public synchronized void onStartupLoadFailed(String message) {
        startupFailures.add(message);
        recorded();
    }

    /**
     * Waits until {@code condition} holds for this listener or the timeout expires.
     *
     * @param condition evaluated while holding this listener's lock
     * @param timeout   maximum wait
     * @return {@code true} if the condition became true in time
     * @throws InterruptedException if interrupted
     */
    public synchronized boolean await(Predicate<RecordingLibraryListener> condition, Duration timeout)
            throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.test(this)) {
            long remainingMillis = (deadline - System.nanoTime()) / 1_000_000L;
            if (remainingMillis <= 0) {
                return false;
            }
            wait(remainingMillis);
        }
        return true;
    }

    /**
     * Returns every document-list change.
     *
     * @return immutable copy
     */
    public synchronized List<List<DocumentSummary>> documentChanges() {
        return List.copyOf(documentChanges);
    }

    /**
     * Returns the latest document list, or an empty list if none was received.
     *
     * @return the latest list
     */
    public synchronized List<DocumentSummary> latestDocuments() {
        return documentChanges.isEmpty() ? List.of() : documentChanges.get(documentChanges.size() - 1);
    }

    /**
     * Returns every import progress event.
     *
     * @return immutable copy
     */
    public synchronized List<ImportProgress> progressEvents() {
        return List.copyOf(progressEvents);
    }

    /**
     * Returns the progress events of one import.
     *
     * @param importId the import id
     * @return immutable list in delivery order
     */
    public synchronized List<ImportProgress> progressOf(long importId) {
        return progressEvents.stream().filter(progress -> progress.importId() == importId).toList();
    }

    /**
     * Tells whether the import reached a terminal state.
     *
     * @param importId the import id
     * @return {@code true} if a terminal progress was received
     */
    public synchronized boolean isTerminal(long importId) {
        return progressEvents.stream()
                .anyMatch(progress -> progress.importId() == importId && progress.state().isTerminal());
    }

    /**
     * Returns every {@code onImportFailed} argument.
     *
     * @return immutable copy
     */
    public synchronized List<ImportProgress> importFailures() {
        return List.copyOf(importFailures);
    }

    /**
     * Returns every {@code onDeleteFailed} message.
     *
     * @return immutable copy
     */
    public synchronized List<String> deleteFailures() {
        return List.copyOf(deleteFailures);
    }

    /**
     * Returns every export outcome.
     *
     * @return immutable copy
     */
    public synchronized List<ExportEvent> exportEvents() {
        return List.copyOf(exportEvents);
    }

    /**
     * Returns every startup progress event as {@code {processed, total}}.
     *
     * @return immutable copy (arrays are copies)
     */
    public synchronized List<int[]> startupProgress() {
        return startupProgress.stream().map(int[]::clone).toList();
    }

    /**
     * Returns every startup completion summary.
     *
     * @return immutable copy
     */
    public synchronized List<LoadSummary> startupCompletions() {
        return List.copyOf(startupCompletions);
    }

    /**
     * Returns every startup failure message.
     *
     * @return immutable copy
     */
    public synchronized List<String> startupFailures() {
        return List.copyOf(startupFailures);
    }

    /**
     * Returns the names of the threads all callbacks ran on, in order.
     *
     * @return immutable copy
     */
    public synchronized List<String> callbackThreads() {
        return List.copyOf(callbackThreads);
    }

    /**
     * Returns the total number of callbacks received.
     *
     * @return the count
     */
    public synchronized int callbackCount() {
        return callbackThreads.size();
    }
}
