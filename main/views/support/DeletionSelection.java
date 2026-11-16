package views.support;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import models.AppConstants;

/**
 * The set of titles whose Delete checkbox is ticked in the library table (D7).
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p>Selection is keyed by title (not by row index) so that it survives table refreshes: when the
 * document list changes, {@link #retainOnly(Collection)} drops titles that no longer exist and
 * keeps the others ticked (D-04, D-11).</p>
 *
 * <p><b>Threading:</b> Not thread-safe; confined to the EDT.</p>
 */
public final class DeletionSelection {

    /** Ticked titles, kept in {@link AppConstants#TITLE_ORDER}. */
    private final Set<String> selectedTitles = new TreeSet<>(AppConstants.TITLE_ORDER);

    /** Creates an empty selection. */
    public DeletionSelection() {
        // empty
    }

    /**
     * Ticks or unticks a title.
     *
     * @param title    the title, not {@code null}
     * @param selected the new state
     */
    public void setSelected(String title, boolean selected) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Flips the state of a title.
     *
     * @param title the title, not {@code null}
     * @return the new state
     */
    public boolean toggle(String title) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Tells whether a title is ticked.
     *
     * @param title the title, not {@code null}
     * @return {@code true} if ticked
     */
    public boolean isSelected(String title) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Drops every ticked title that is not in {@code existingTitles}.
     *
     * @param existingTitles titles that still exist, not {@code null}
     */
    public void retainOnly(Collection<String> existingTitles) {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Returns the ticked titles.
     *
     * @return immutable list sorted by {@code TITLE_ORDER}
     */
    public List<String> selectedTitles() {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /**
     * Tells whether nothing is ticked (the Delete button is then disabled, D7).
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        throw new UnsupportedOperationException("TODO: Member D");
    }

    /** Unticks everything. */
    public void clear() {
        throw new UnsupportedOperationException("TODO: Member D");
    }
}
