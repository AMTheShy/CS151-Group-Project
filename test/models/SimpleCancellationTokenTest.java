package models;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SimpleCancellationToken} and {@link CancellationToken#NONE}. Owner: Eric. */
class SimpleCancellationTokenTest {

    /** E-36: cancel flips the flag once and throwIfCancelled throws afterwards. */
    @Test
    @DisplayName("E-36 cancel is idempotent and makes throwIfCancelled throw OperationCancelledException")
    void cancel_calledTwice_firstReturnsTrueThenFalse() {
        SimpleCancellationToken token = new SimpleCancellationToken();
        assertFalse(token.isCancelled());
        assertDoesNotThrow(token::throwIfCancelled);

        assertTrue(token.cancel());
        assertFalse(token.cancel());

        assertTrue(token.isCancelled());
        assertThrows(OperationCancelledException.class, token::throwIfCancelled);
    }

    /** E-37: NONE is never cancelled. */
    @Test
    @DisplayName("E-37 CancellationToken.NONE is never cancelled")
    void none_always_isNotCancelled() {
        assertFalse(CancellationToken.NONE.isCancelled());
        assertDoesNotThrow(CancellationToken.NONE::throwIfCancelled);
    }

    /** E-38: cancellation from another thread becomes visible (concurrency). */
    @Test
    @DisplayName("E-38 cancellation by one thread is observed by a polling thread")
    void cancel_fromOtherThread_isObservedByPollingThread() throws InterruptedException {
        SimpleCancellationToken token = new SimpleCancellationToken();
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch observed = new CountDownLatch(1);
        AtomicBoolean sawCancellation = new AtomicBoolean();
        Thread poller = new Thread(() -> {
            started.countDown();
            while (!token.isCancelled()) {
                Thread.onSpinWait();
            }
            sawCancellation.set(true);
            observed.countDown();
        }, "token-poller");
        poller.start();
        assertTrue(started.await(5, TimeUnit.SECONDS));

        token.cancel();

        assertTrue(observed.await(5, TimeUnit.SECONDS), "poller did not observe cancellation in time");
        assertTrue(sawCancellation.get());
        poller.join(TimeUnit.SECONDS.toMillis(5));
    }
}
