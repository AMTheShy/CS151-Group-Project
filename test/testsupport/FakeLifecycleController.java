package testsupport;

import java.util.concurrent.atomic.AtomicInteger;

import controllers.LifecycleController;

/**
 * Recording {@link LifecycleController}; used by Member D to test that Exit / window closing
 * shuts the application down.
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeLifecycleController implements LifecycleController {

    private final AtomicInteger shutdownCalls = new AtomicInteger();

    /** Creates the controller. */
    public FakeLifecycleController() {
        // empty
    }

    /**
     * Returns how often {@link #shutdown()} was called.
     *
     * @return the count
     */
    public int shutdownCalls() {
        return shutdownCalls.get();
    }

    @Override
    public void shutdown() {
        shutdownCalls.incrementAndGet();
    }
}
