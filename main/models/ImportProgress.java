package models;

import java.util.Objects;
import java.util.Optional;

/**
 * A snapshot of the progress of one asynchronous import (U2).
 *
 * <p>The controller publishes a new {@code ImportProgress} for every state change and for
 * byte-count updates while reading. The view keeps one row per {@link #importId()}; the id
 * (not the title) is the key, because two imports with the same title may run at the same time
 * (D2).</p>
 *
 * <p>Invariants:</p>
 * <ul>
 *   <li>{@code importId > 0}; {@code title} is a valid title; {@code state} and
 *       {@code errorMessage} are not {@code null}.</li>
 *   <li>{@code 0 <= bytesRead}; {@code 0 <= totalBytes}; if {@code totalBytes > 0} then
 *       {@code bytesRead <= totalBytes}. {@code totalBytes == 0} means "unknown yet" while
 *       QUEUED, or an empty file.</li>
 *   <li>{@code errorMessage} is present if and only if {@code state == FAILED}.</li>
 * </ul>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param importId     unique id of the import, assigned by the controller, {@code > 0}
 * @param title        the title the document will get
 * @param bytesRead    number of bytes read so far
 * @param totalBytes   size of the source file in bytes, or 0 if not known yet
 * @param state        the current state
 * @param errorMessage the failure message; present only when {@code state == FAILED}
 */
public record ImportProgress(long importId, String title, long bytesRead, long totalBytes,
                             ImportState state, Optional<String> errorMessage) {

    /**
     * Validates the invariants.
     *
     * @throws NullPointerException     if a reference argument is {@code null}
     * @throws IllegalArgumentException if any invariant is violated
     */
    public ImportProgress {
        if (importId <= 0) {
            throw new IllegalArgumentException("importId must be > 0 but was " + importId);
        }
        DocumentTitles.requireValid(title);
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(errorMessage, "errorMessage");
        if (bytesRead < 0 || totalBytes < 0) {
            throw new IllegalArgumentException("byte counts must be >= 0");
        }
        if (totalBytes > 0 && bytesRead > totalBytes) {
            throw new IllegalArgumentException("bytesRead " + bytesRead + " > totalBytes " + totalBytes);
        }
        if ((state == ImportState.FAILED) != errorMessage.isPresent()) {
            throw new IllegalArgumentException("errorMessage must be present exactly when state is FAILED");
        }
    }

    /**
     * Creates the initial QUEUED progress.
     *
     * @param importId the import id, {@code > 0}
     * @param title    the valid title
     * @return a QUEUED progress with zero bytes
     */
    public static ImportProgress queued(long importId, String title) {
        return new ImportProgress(importId, title, 0, 0, ImportState.QUEUED, Optional.empty());
    }

    /**
     * Creates a READING progress.
     *
     * @param importId   the import id
     * @param title      the valid title
     * @param bytesRead  bytes read so far
     * @param totalBytes total file size
     * @return a READING progress
     */
    public static ImportProgress reading(long importId, String title, long bytesRead, long totalBytes) {
        return new ImportProgress(importId, title, bytesRead, totalBytes, ImportState.READING, Optional.empty());
    }

    /**
     * Creates a SAVING progress (all bytes read).
     *
     * @param importId   the import id
     * @param title      the valid title
     * @param totalBytes total file size
     * @return a SAVING progress with {@code bytesRead == totalBytes}
     */
    public static ImportProgress saving(long importId, String title, long totalBytes) {
        return new ImportProgress(importId, title, totalBytes, totalBytes, ImportState.SAVING, Optional.empty());
    }

    /**
     * Creates a COMPLETED progress.
     *
     * @param importId   the import id
     * @param title      the valid title
     * @param totalBytes total file size
     * @return a COMPLETED progress with {@code bytesRead == totalBytes}
     */
    public static ImportProgress completed(long importId, String title, long totalBytes) {
        return new ImportProgress(importId, title, totalBytes, totalBytes, ImportState.COMPLETED, Optional.empty());
    }

    /**
     * Creates a FAILED progress.
     *
     * @param importId   the import id
     * @param title      the valid title
     * @param bytesRead  bytes read before the failure
     * @param totalBytes total file size, or 0 if unknown
     * @param message    a human-readable reason, not {@code null}
     * @return a FAILED progress
     */
    public static ImportProgress failed(long importId, String title, long bytesRead, long totalBytes,
                                        String message) {
        Objects.requireNonNull(message, "message");
        return new ImportProgress(importId, title, bytesRead, totalBytes, ImportState.FAILED, Optional.of(message));
    }

    /**
     * Creates a CANCELLED progress.
     *
     * @param importId   the import id
     * @param title      the valid title
     * @param bytesRead  bytes read before cancellation
     * @param totalBytes total file size, or 0 if unknown
     * @return a CANCELLED progress
     */
    public static ImportProgress cancelled(long importId, String title, long bytesRead, long totalBytes) {
        return new ImportProgress(importId, title, bytesRead, totalBytes, ImportState.CANCELLED, Optional.empty());
    }

    /**
     * Returns the completed fraction for a progress bar.
     *
     * @return 1.0 if COMPLETED; 0.0 if {@code totalBytes == 0}; otherwise
     *         {@code bytesRead / (double) totalBytes}
     */
    public double fractionComplete() {
        if (state == ImportState.COMPLETED) {
            return 1.0;
        }
        if (totalBytes == 0) {
            return 0.0;
        }
        return bytesRead / (double) totalBytes;
    }

    /**
     * Returns the completed percentage, rounded down.
     *
     * @return an integer in 0..100
     */
    public int percentComplete() {
        return (int) Math.floor(fractionComplete() * 100.0);
    }
}
