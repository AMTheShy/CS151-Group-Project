package testsupport;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import controllers.SearchListener;
import models.DocumentSearchResult;

/**
 * {@link SearchListener} that records every callback and the name of the thread it ran on.
 * Used by Member C (controller and integration tests).
 *
 * <p>Use {@link #awaitCallback(Duration)} instead of {@code Thread.sleep} to wait for a result
 * produced by real worker threads.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class RecordingSearchListener implements SearchListener {

    private final List<List<DocumentSearchResult>> completions = new CopyOnWriteArrayList<>();
    private final List<String> failures = new CopyOnWriteArrayList<>();
    private final List<String> callbackThreads = new CopyOnWriteArrayList<>();
    private final CountDownLatch firstCallback = new CountDownLatch(1);

    /** Creates the listener. */
    public RecordingSearchListener() {
        // empty
    }

    @Override
    public void onSearchCompleted(List<DocumentSearchResult> sortedResults) {
        completions.add(sortedResults);
        callbackThreads.add(Thread.currentThread().getName());
        firstCallback.countDown();
    }

    @Override
    public void onSearchFailed(String message) {
        failures.add(message);
        callbackThreads.add(Thread.currentThread().getName());
        firstCallback.countDown();
    }

    /**
     * Waits for the first callback.
     *
     * @param timeout maximum wait
     * @return {@code true} if a callback arrived in time
     * @throws InterruptedException if interrupted
     */
    public boolean awaitCallback(Duration timeout) throws InterruptedException {
        return firstCallback.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Returns every {@code onSearchCompleted} argument.
     *
     * @return immutable copy
     */
    public List<List<DocumentSearchResult>> completions() {
        return List.copyOf(completions);
    }

    /**
     * Returns every {@code onSearchFailed} message.
     *
     * @return immutable copy
     */
    public List<String> failures() {
        return List.copyOf(failures);
    }

    /**
     * Returns the names of the threads the callbacks ran on.
     *
     * @return immutable copy
     */
    public List<String> callbackThreads() {
        return List.copyOf(callbackThreads);
    }

    /**
     * Returns the total number of callbacks.
     *
     * @return completions plus failures
     */
    public int callbackCount() {
        return completions.size() + failures.size();
    }
}
