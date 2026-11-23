package controllers.concurrency;

import java.util.Objects;

import javax.swing.SwingUtilities;

import controllers.UiDispatcher;

/** Schedules callbacks on Swing's event dispatch thread. */
public final class SwingUiDispatcher implements UiDispatcher {

    @Override
    public void dispatch(Runnable task) {
        SwingUtilities.invokeLater(Objects.requireNonNull(task, "task"));
    }
}