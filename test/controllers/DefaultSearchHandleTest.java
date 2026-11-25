package controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import models.SearchQuery;

/**
 * Tests for {@code DefaultSearchHandle}. Owner: Member C.
 *
 * <p>Case C-39 of {@code design/test-plan.md}. The method name and display name
 * identify the test case.</p>
 */
class DefaultSearchHandleTest {

    private DefaultSearchHandle handle;

    /** Creates fresh fixtures for every test. */
    @BeforeEach
    void setUp() {
        handle = new DefaultSearchHandle(SearchQuery.of("q"));
    }

    /**
     * C-39 verifies search cancellation before and after completion, then races
     * cancellation against completion across 1,000 rounds. Exactly one wins each round.
     */
    @Test
    @Tag("concurrency")
    @DisplayName("C-39 CAS state machine (R5)")
    void cancelAndTryComplete_raceOnState_exactlyOneWins() throws Exception {
        FutureTask<Void> attached = new FutureTask<>(() -> null);
        handle.attachFutures(List.of(attached));

        handle.cancel();

        assertTrue(handle.isCancelled());
        assertTrue(handle.isDone());
        assertTrue(handle.token().isCancelled());
        assertTrue(attached.isCancelled());
        assertFalse(handle.tryComplete());
        assertFalse(handle.tryFail());

        FutureTask<Void> late = new FutureTask<>(() -> null);
        handle.attachFutures(List.of(late));
        assertTrue(late.isCancelled());

        DefaultSearchHandle completed =
                new DefaultSearchHandle(SearchQuery.of("q"));

        assertTrue(completed.tryComplete());
        completed.cancel();

        assertTrue(completed.isDone());
        assertFalse(completed.isCancelled());

        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 1000; round++) {
                DefaultSearchHandle racing =
                        new DefaultSearchHandle(SearchQuery.of("q"));
                CountDownLatch start = new CountDownLatch(1);

                Future<?> cancellation = workers.submit(() -> {
                    start.await();
                    racing.cancel();
                    return null;
                });

                Future<Boolean> completion = workers.submit(() -> {
                    start.await();
                    return racing.tryComplete();
                });

                start.countDown();

                cancellation.get(5, TimeUnit.SECONDS);
                boolean completionWon = completion.get(5, TimeUnit.SECONDS);

                assertTrue(racing.isDone());
                assertEquals(!completionWon, racing.isCancelled(),
                        "Round " + round);
            }
        } finally {
            workers.shutdownNow();
        }
    }
}