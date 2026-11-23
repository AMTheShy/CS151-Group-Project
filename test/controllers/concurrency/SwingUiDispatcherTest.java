package controllers.concurrency;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SwingUiDispatcherTest {

    private SwingUiDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new SwingUiDispatcher();
    }

    @Test
    @DisplayName("C-36 EDT guarantee (4.3)")
    void dispatch_fromWorkerThread_runsOnEventDispatchThread() {
        var ranOnEdt = new CompletableFuture<Boolean>();
        dispatcher.dispatch(() ->
                ranOnEdt.complete(SwingUtilities.isEventDispatchThread()));
        assertTrue(assertDoesNotThrow(() -> ranOnEdt.get(5, TimeUnit.SECONDS)));
    }

    @Test
    @DisplayName("C-37 FIFO ordering")
    void dispatch_fromEventDispatchThread_isDeferredPreservingFifo() {
        var order = new ArrayList<String>();

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            dispatcher.dispatch(() -> order.add("first"));
            dispatcher.dispatch(() -> order.add("second"));
            assertTrue(order.isEmpty());
        }));

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() ->
                assertEquals(List.of("first", "second"), order)));
    }
}