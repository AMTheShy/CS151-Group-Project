package testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Tests the UI dispatcher doubles. Owner: Eric. */
class QueueingUiDispatcherTest {

    /** E-53: tasks wait until flush and then run FIFO, including tasks queued while flushing. */
    @Test
    @DisplayName("E-53 queued tasks run only on flush, in FIFO order, including nested dispatches")
    void flush_queuedAndNestedTasks_runInFifoOrder() {
        QueueingUiDispatcher dispatcher = new QueueingUiDispatcher();
        List<String> order = new ArrayList<>();
        dispatcher.dispatch(() -> {
            order.add("first");
            dispatcher.dispatch(() -> order.add("nested"));
        });
        dispatcher.dispatch(() -> order.add("second"));
        assertEquals(List.of(), order);

        int executed = dispatcher.flush();

        assertEquals(3, executed);
        assertEquals(List.of("first", "second", "nested"), order);
        assertEquals(0, dispatcher.pendingCount());
    }

    /** E-54: the immediate dispatcher runs inline. */
    @Test
    @DisplayName("E-54 ImmediateUiDispatcher runs the task synchronously on the calling thread")
    void dispatch_immediateDispatcher_runsInline() {
        List<String> threads = new ArrayList<>();

        new ImmediateUiDispatcher().dispatch(() -> threads.add(Thread.currentThread().getName()));

        assertEquals(List.of(Thread.currentThread().getName()), threads);
    }
}
