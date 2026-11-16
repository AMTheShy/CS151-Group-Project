package controllers;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import controllers.concurrency.ManualTaskScheduler;

/**
 * Tests for {@code DefaultLifecycleController}. Owner: Member C.
 *
 * <p>Cases C-38..C-38 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DefaultLifecycleControllerTest {

    private ManualTaskScheduler scheduler;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        scheduler = new ManualTaskScheduler();
    }

    /**
     * C-38: application exit (R8) [unit].
     *
     * <p>Arrange: library and search controllers on fakes with running work; shutdown() twice, timed.</p>
     * <p>Act: shutdown.</p>
     * <p>Assert: searches and imports cancelled; scheduler.isShutdown(); second call no-op; each call &lt;
     * 100 ms.</p>
     */
    @Test
    @Disabled("TODO: Member C")
    @DisplayName("C-38 application exit (R8)")
    void shutdown_calledTwice_cancelsWorkStopsSchedulerOnce() {
        fail("TODO: Member C - C-38");
    }
}
