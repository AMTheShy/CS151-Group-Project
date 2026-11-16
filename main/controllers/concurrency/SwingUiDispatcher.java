package controllers.concurrency;

import controllers.UiDispatcher;

/**
 * Production {@link UiDispatcher}: posts every task to the Swing event queue with
 * {@code javax.swing.SwingUtilities.invokeLater}.
 *
 * <p><b>Owner:</b> Member C. This is the <b>only</b> class in {@code controllers} (including
 * sub-packages) that may import {@code javax.swing} or {@code java.awt} (dependency rule 4.3).</p>
 *
 * <p><b>Required behavior:</b> always defer with {@code invokeLater}, even when already on the
 * EDT. Running inline when {@code SwingUtilities.isEventDispatchThread()} would let a later
 * notification overtake an earlier queued one and break FIFO ordering (test C-37). Null tasks are
 * rejected with {@code NullPointerException} on the calling thread.</p>
 *
 * <p><b>Threading:</b> Thread-safe (stateless); callable from any thread.</p>
 */
public final class SwingUiDispatcher implements UiDispatcher {

    /** Creates the dispatcher. */
    public SwingUiDispatcher() {
        // stateless
    }

    /** {@inheritDoc} Tests: C-36, C-37. */
    @Override
    public void dispatch(Runnable task) {
        throw new UnsupportedOperationException("TODO: Member C");
    }
}
