package controllers.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ExecutorTaskSchedulerTest {

    private ExecutorTaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ExecutorTaskScheduler(2, 3, 2);
    }

    @AfterEach
    void stopScheduler() {
        scheduler.shutdownNow();
    }

    @Test
    @Tag("concurrency")
    @DisplayName("C-31 bounded import pool (4.4)")
    void submitImport_fiveBlockingTasks_atMostTwoRunConcurrently() throws Exception {
        var entered = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        var active = new AtomicInteger();
        var peak = new AtomicInteger();
        var futures = new ArrayList<Future<?>>();

        for (int i = 0; i < 5; i++) {
            futures.add(scheduler.submitImport(() -> {
                int count = active.incrementAndGet();
                peak.accumulateAndGet(count, Math::max);
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(failure);
                } finally {
                    active.decrementAndGet();
                }
            }));
        }

        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
        }

        for (Future<?> future : futures) {
            future.get(5, TimeUnit.SECONDS);
        }
        assertEquals(2, peak.get());
    }

    @Test
    @Tag("concurrency")
    @DisplayName("C-32 search pool size")
    void supplySearch_manyTasks_useConfiguredParallelism() throws Exception {
        var barrier = new CyclicBarrier(3);
        var futures = new ArrayList<CompletableFuture<Integer>>();

        for (int i = 0; i < 6; i++) {
            futures.add(scheduler.supplySearch(() -> {
                try {
                    return barrier.await(5, TimeUnit.SECONDS);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }));
        }

        for (var future : futures) {
            assertTrue(future.get(5, TimeUnit.SECONDS) >= 0);
        }
    }

    @Test
    @DisplayName("C-33 thread naming")
    void workerThreads_areNamedByPool() throws Exception {
        var imported = new CompletableFuture<Thread>();
        var background = new CompletableFuture<Thread>();

        scheduler.submitImport(() -> imported.complete(Thread.currentThread()));
        scheduler.submitBackground(() -> background.complete(Thread.currentThread()));
        var search = scheduler.supplySearch(Thread::currentThread);

        checkThread(imported.get(5, TimeUnit.SECONDS), "lse-import-");
        checkThread(search.get(5, TimeUnit.SECONDS), "lse-search-");
        checkThread(background.get(5, TimeUnit.SECONDS), "lse-background-");
    }

    @Test
    @Tag("concurrency")
    @DisplayName("C-34 shutdown (R8)")
    void shutdownNow_interruptsRunningAndRejectsNew() throws Exception {
        var started = new CountDownLatch(1);
        var interrupted = new CountDownLatch(1);

        scheduler.submitBackground(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException expected) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(started.await(5, TimeUnit.SECONDS));
        scheduler.shutdownNow();
        assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        assertTrue(scheduler.isShutdown());
        assertThrows(RejectedExecutionException.class,
                () -> scheduler.submitBackground(() -> {}));
    }

    @Test
    @DisplayName("C-35 exceptions are not swallowed")
    void submitBackground_throwingTask_isLoggedNotLost() throws Exception {
        Logger logger = Logger.getLogger(ExecutorTaskScheduler.class.getName());
        var records = new LinkedBlockingQueue<LogRecord>();

        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(record);
            }

            @Override
            public void flush() {}

            @Override
            public void close() {}
        };

        logger.addHandler(handler);
        try {
            RuntimeException expected = new IllegalStateException("task failure");
            Future<?> future = scheduler.submitBackground(() -> {
                throw expected;
            });

            assertSame(expected, assertThrows(ExecutionException.class,
                    () -> future.get(5, TimeUnit.SECONDS)).getCause());

            LogRecord record = records.poll(5, TimeUnit.SECONDS);
            assertNotNull(record);
            assertEquals(Level.SEVERE, record.getLevel());
            assertSame(expected, record.getThrown());
        } finally {
            logger.removeHandler(handler);
        }
    }

    private static void checkThread(Thread thread, String prefix) {
        assertTrue(thread.getName().startsWith(prefix));
        assertFalse(thread.isDaemon());
    }
}