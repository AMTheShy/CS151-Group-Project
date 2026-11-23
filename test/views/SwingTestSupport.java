package views;

import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import models.AppConstants;
import models.DocumentSearchResult;
import models.DocumentSummary;
import models.MatchRange;
import models.MatchRanges;
import testsupport.FakeSnippetExtractor;
import testsupport.ReferenceMatcher;
import testsupport.TestDocuments;

/**
 * Shared helpers for the view tests: running code on the EDT (Q9), waiting for the reader's chained
 * EDT steps, and building table rows and search results.
 *
 * <p><b>Owner:</b> Member D (test helper of the views module; not part of the frozen contract).</p>
 *
 * <p><b>Threading:</b> Called from the JUnit thread; {@link #onEdt} and {@link #onEdtGet} hop to the EDT.</p>
 */
final class SwingTestSupport {

    /** Upper bound for waiting on chained EDT steps. */
    private static final long RENDER_TIMEOUT_SECONDS = 10;

    private SwingTestSupport() {
        throw new AssertionError("SwingTestSupport is a utility class and must not be instantiated");
    }

    /**
     * Runs an action on the EDT and waits for it; failures are rethrown on the test thread.
     *
     * @param action the action, not {@code null}
     */
    static void onEdt(EdtAction action) {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    action.run();
                } catch (Exception | AssertionError thrown) {
                    failure.set(thrown);
                }
            });
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the EDT", interrupted);
        } catch (InvocationTargetException invocationFailure) {
            throw new IllegalStateException("EDT action failed", invocationFailure.getCause());
        }
        Throwable thrown = failure.get();
        if (thrown instanceof AssertionError) {
            throw (AssertionError) thrown;
        }
        if (thrown instanceof RuntimeException) {
            throw (RuntimeException) thrown;
        }
        if (thrown != null) {
            throw new IllegalStateException("EDT action failed", thrown);
        }
    }

    /**
     * Computes a value on the EDT.
     *
     * @param supplier the computation, not {@code null}
     * @param <T>      result type
     * @return the value
     */
    static <T> T onEdtGet(Supplier<T> supplier) {
        AtomicReference<T> result = new AtomicReference<>();
        onEdt(() -> result.set(supplier.get()));
        return result.get();
    }

    /**
     * Waits until the reader has run all chained EDT steps of its last render (each poll queues
     * behind the pending steps, so no sleeping is needed - T6).
     *
     * @param reader the reader, not {@code null}
     */
    static void awaitRender(DocumentReaderPanel reader) {
        awaitRenderMeasuringLongestRoundTrip(reader);
    }

    /**
     * Waits like {@link #awaitRender} and returns the longest EDT round trip of the polls.
     *
     * <p>The reader chains its steps one at a time, so a poll posted just before a step waits for
     * that whole step: the longest round trip is an upper bound of the longest single EDT step
     * (U6, D-47) without replacing the AWT event queue.</p>
     *
     * @param reader the reader, not {@code null}
     * @return the longest round trip in nanoseconds
     */
    static long awaitRenderMeasuringLongestRoundTrip(DocumentReaderPanel reader) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(RENDER_TIMEOUT_SECONDS);
        long longestRoundTrip = 0;
        boolean complete = false;
        while (!complete) {
            if (System.nanoTime() > deadline) {
                fail("reader did not finish rendering within " + RENDER_TIMEOUT_SECONDS + " s");
            }
            long start = System.nanoTime();
            complete = onEdtGet(reader::isRenderComplete);
            longestRoundTrip = Math.max(longestRoundTrip, System.nanoTime() - start);
        }
        return longestRoundTrip;
    }

    /**
     * Builds a library row.
     *
     * @param title the title
     * @return a summary with preview {@code "Preview of <title>"}
     */
    static DocumentSummary summary(String title) {
        return new DocumentSummary(TestDocuments.document(title), "Preview of " + title);
    }

    /**
     * Builds library rows.
     *
     * @param titles titles, already in {@code TITLE_ORDER}
     * @return the summaries in the given order
     */
    static List<DocumentSummary> summaries(String... titles) {
        return Arrays.stream(titles).map(SwingTestSupport::summary).toList();
    }

    /**
     * Builds a search result with the matches of the reference oracle.
     *
     * @param title   the document title
     * @param content the content; must contain at least one match
     * @param query   the query text
     * @return the result
     */
    static DocumentSearchResult result(String title, String content, String query) {
        MatchRanges matches = ReferenceMatcher.findAll(content, query);
        return result(title, content, matches);
    }

    /**
     * Builds a search result from precomputed matches.
     *
     * @param title   the document title
     * @param content the content
     * @param matches the matches, non-empty
     * @return the result
     */
    static DocumentSearchResult result(String title, String content, List<MatchRange> matches) {
        return new DocumentSearchResult(TestDocuments.document(title, content), matches,
                new FakeSnippetExtractor().extract(content, matches.get(0), AppConstants.SNIPPET_CONTEXT));
    }

    /**
     * A block of test code that may throw.
     */
    @FunctionalInterface
    interface EdtAction {

        /**
         * Runs the action.
         *
         * @throws Exception anything; rethrown to the test
         */
        void run() throws Exception;
    }
}
