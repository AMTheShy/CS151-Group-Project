package testsupport;

import java.util.Objects;

import controllers.UiDispatcher;

/**
 * Test double of {@link UiDispatcher} that runs every task synchronously on the calling thread.
 *
 * <p>Use it when a test only cares about <em>what</em> is delivered, not about thread hand-off.
 * To test ordering or "cancelled before delivery" races, use {@link QueueingUiDispatcher}.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe (stateless); tasks run on whatever thread calls {@link #dispatch}.</p>
 */
public final class ImmediateUiDispatcher implements UiDispatcher {

    /** Creates the dispatcher. */
    public ImmediateUiDispatcher() {
        // stateless
    }

    /**
     * Runs the task immediately.
     *
     * @param task the task, not {@code null}
     */
    @Override
    public void dispatch(Runnable task) {
        Objects.requireNonNull(task, "task").run();
    }
}
