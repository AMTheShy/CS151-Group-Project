package testsupport;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import controllers.UiDispatcher;

/**
 * Test double of {@link UiDispatcher} that queues tasks until the test calls {@link #flush()}.
 * It simulates the Swing event queue: tasks run later, in FIFO order, on the thread that flushes
 * (the test thread plays the role of the EDT).
 *
 * <p>Typical use: start a search, let it compute, call {@code handle.cancel()} <em>before</em>
 * {@link #flush()}, then assert that the listener was never called (race R5).</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe; tasks may be dispatched from worker threads.</p>
 */
public final class QueueingUiDispatcher implements UiDispatcher {

    /** Pending tasks in dispatch order; guarded by {@code this}. */
    private final Deque<Runnable> pending = new ArrayDeque<>();

    /** Creates an empty dispatcher. */
    public QueueingUiDispatcher() {
        // nothing to initialize
    }

    /**
     * Queues the task.
     *
     * @param task the task, not {@code null}
     */
    @Override
    public synchronized void dispatch(Runnable task) {
        pending.addLast(Objects.requireNonNull(task, "task"));
    }

    /**
     * Returns the number of queued tasks.
     *
     * @return pending count
     */
    public synchronized int pendingCount() {
        return pending.size();
    }

    /**
     * Runs queued tasks in FIFO order on the calling thread until the queue is empty, including
     * tasks dispatched while flushing.
     *
     * @return the number of tasks run
     */
    public int flush() {
        int executed = 0;
        while (true) {
            Runnable next;
            synchronized (this) {
                next = pending.pollFirst();
            }
            if (next == null) {
                return executed;
            }
            next.run();
            executed++;
        }
    }
}
