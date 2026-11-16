package controllers;

/**
 * Moves work onto the UI thread (4.4).
 *
 * <p>Controllers never call Swing directly. To notify a listener they wrap the call in a
 * {@code Runnable} and pass it to {@link #dispatch(Runnable)}. In production
 * ({@code controllers.concurrency.SwingUiDispatcher}) this posts to the Swing event queue; in
 * tests ({@code testsupport.ImmediateUiDispatcher}) it runs the task synchronously, and
 * {@code testsupport.QueueingUiDispatcher} holds tasks until the test flushes them. This is what
 * makes the controllers fully testable without a GUI (DIP).</p>
 *
 * <p>Contract for implementations: tasks are executed in the order they were dispatched (FIFO),
 * each exactly once, on the UI thread. A production implementation must always defer (never run
 * the task inline, even when called on the UI thread) to keep this ordering.</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member C
 * ({@code SwingUiDispatcher}) and by the test support.</p>
 *
 * <p>Threading: Thread-safe; may be called from any thread.</p>
 */
@FunctionalInterface
public interface UiDispatcher {

    /**
     * Schedules a task for execution on the UI thread.
     *
     * @param task the task, not {@code null}
     * @throws NullPointerException if {@code task} is {@code null}
     */
    void dispatch(Runnable task);
}
