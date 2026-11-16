package controllers.concurrency;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code SwingUiDispatcher}. Owner: Member C.
 *
 * <p>Cases C-36..C-37 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SwingUiDispatcherTest {

    private SwingUiDispatcher dispatcher;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        dispatcher = new SwingUiDispatcher();
    }

    /**
     * C-36: EDT guarantee (4.3) [unit].
     *
     * <p>Arrange: dispatch from a plain test thread; latch.</p>
     * <p>Act: dispatch.</p>
     * <p>Assert: task observes SwingUtilities.isEventDispatchThread() true (EDT exists also when
     * headless).</p>
     */
    @Test
    @Disabled("TODO: Member C")
    @DisplayName("C-36 EDT guarantee (4.3)")
    void dispatch_fromWorkerThread_runsOnEventDispatchThread() {
        fail("TODO: Member C - C-36");
    }

    /**
     * C-37: FIFO ordering [unit].
     *
     * <p>Arrange: inside invokeAndWait: dispatch(A); record B inline; then dispatch(C).</p>
     * <p>Act: dispatch.</p>
     * <p>Assert: recorded order B, A, C.</p>
     */
    @Test
    @Disabled("TODO: Member C")
    @DisplayName("C-37 FIFO ordering")
    void dispatch_fromEventDispatchThread_isDeferredPreservingFifo() {
        fail("TODO: Member C - C-37");
    }
}
