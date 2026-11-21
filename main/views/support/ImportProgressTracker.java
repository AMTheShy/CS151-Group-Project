package views.support;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

import models.ImportProgress;
import models.ImportState;

/**
 * Display model of the import progress area on the left (U2): one row per running or recently
 * finished import, keyed by import id.
 *
 * <p><b>Owner:</b> Member B (pure display logic; placed in {@code views.support} because the views use
 * it - see developer guide, "Workload balancing").</p>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>{@link #apply(ImportProgress)} inserts a row for an unknown id and replaces the row for a
 *       known id. Updates for an id that was already {@link #remove removed} are ignored (a late
 *       READING update must not resurrect a finished row).</li>
 *   <li>A terminal progress never gets replaced by a non-terminal one (out-of-order protection).</li>
 *   <li>Rows are ordered by import id (= start order).</li>
 *   <li>COMPLETED and CANCELLED rows are removed by the view after
 *       {@code AppConstants.COMPLETED_IMPORT_ROW_LINGER_MILLIS} (a {@code javax.swing.Timer} in
 *       {@code ImportProgressPanel}); FAILED rows stay, shown in red, until the user dismisses them.</li>
 *   <li>{@link #statusText(ImportProgress)}: QUEUED &rarr; {@code "Queued"}; READING &rarr;
 *       {@code "Reading 45%"}; SAVING &rarr; {@code "Saving"}; COMPLETED &rarr; {@code "Done"};
 *       CANCELLED &rarr; {@code "Cancelled"}; FAILED &rarr; {@code "Failed: <message>"}.</li>
 * </ul>
 *
 * <p><b>Threading:</b> Not thread-safe; confined to the EDT.</p>
 */
public final class ImportProgressTracker {

    /** Current row per import id, ascending by id. */
    private final Map<Long, ImportProgress> rows = new TreeMap<>();

    /** Ids whose rows were removed; later updates for them are ignored (ids are not removed in order). */
    private final Set<Long> removedIds = new HashSet<>();

    /** Creates an empty tracker. */
    public ImportProgressTracker() {
        // empty
    }

    /**
     * Applies a progress update (B-57, B-58, B-60, B-61, B-62).
     *
     * @param progress the update, not {@code null}
     * @return {@code true} if the visible rows changed
     */
    public boolean apply(ImportProgress progress) {
        Objects.requireNonNull(progress, "progress");
        long importId = progress.importId();
        if (removedIds.contains(importId)) {
            return false;
        }
        ImportProgress previous = rows.get(importId);
        if (previous != null && previous.state().isTerminal()) {
            return false;
        }
        if (progress.equals(previous)) {
            return false;
        }
        rows.put(importId, progress);
        return true;
    }

    /**
     * Returns the rows in import-id order (B-63).
     *
     * @return immutable list
     */
    public List<ImportProgress> rows() {
        return List.copyOf(rows.values());
    }

    /**
     * Returns the row of one import.
     *
     * @param importId the id
     * @return the row, or empty
     */
    public Optional<ImportProgress> row(long importId) {
        return Optional.ofNullable(rows.get(importId));
    }

    /**
     * Removes a row (after the linger time, or when the user dismisses a FAILED row) (B-59, B-61).
     *
     * @param importId the id
     * @return {@code true} if a row was removed
     */
    public boolean remove(long importId) {
        removedIds.add(importId);
        return rows.remove(importId) != null;
    }

    /**
     * Formats the status column (B-64).
     *
     * @param progress the row, not {@code null}
     * @return the status text
     */
    public static String statusText(ImportProgress progress) {
        Objects.requireNonNull(progress, "progress");
        ImportState state = progress.state();
        return switch (state) {
            case QUEUED -> "Queued";
            case READING -> "Reading " + progress.percentComplete() + "%";
            case SAVING -> "Saving";
            case COMPLETED -> "Done";
            case CANCELLED -> "Cancelled";
            case FAILED -> "Failed: " + progress.errorMessage().orElseThrow();
        };
    }
}
