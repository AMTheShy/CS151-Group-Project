package models;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of loading the persisted documents at startup (D5).
 *
 * <p>Owner: Eric (contract layer, frozen v1.0). Produced by
 * {@code models.storage.DocumentRepository#loadPersisted}, shown by the view.</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param loadedCount  number of documents added to the repository
 * @param skippedCount number of persisted documents not added because a document with the same
 *                     title was already in memory (imported by the user while loading)
 * @param problems     one human-readable message per persisted entry that could not be loaded
 *                     (corrupt or unreadable); immutable
 */
public record LoadSummary(int loadedCount, int skippedCount, List<String> problems) {

    /**
     * Validates and copies.
     *
     * @throws NullPointerException     if {@code problems} or an element is {@code null}
     * @throws IllegalArgumentException if a count is negative
     */
    public LoadSummary {
        if (loadedCount < 0 || skippedCount < 0) {
            throw new IllegalArgumentException("counts must be >= 0");
        }
        problems = List.copyOf(Objects.requireNonNull(problems, "problems"));
    }

    /**
     * Returns the summary of loading nothing.
     *
     * @return {@code new LoadSummary(0, 0, List.of())}
     */
    public static LoadSummary empty() {
        return new LoadSummary(0, 0, List.of());
    }

    /**
     * Tells whether any entry failed to load.
     *
     * @return {@code !problems().isEmpty()}
     */
    public boolean hasProblems() {
        return !problems.isEmpty();
    }
}
