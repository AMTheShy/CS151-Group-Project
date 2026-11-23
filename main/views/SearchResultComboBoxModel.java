package views;

import java.util.List;
import java.util.Objects;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import models.DocumentSearchResult;

/**
 * Model of the drop-down list at the top of a search tab (U4): only matching documents, in the
 * order received (already sorted by title, D6).
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Rules:</b> {@link #setResults(List)} replaces all elements, clears the selection and fires
 * {@code fireContentsChanged(this, -1, -1)} plus an interval event covering the new elements. The
 * selection is {@code null} until the user picks an item (the reader then stays empty with a hint
 * "Select a document above"). {@link #getSelectedItem()} returns {@code null} when nothing is
 * selected - this is required by the Swing {@code ComboBoxModel} API and is the documented
 * exception to the "never return null" rule in {@code CONVENTIONS.md}.</p>
 *
 * <p><b>Threading:</b> EDT only (Swing model).</p>
 */
public final class SearchResultComboBoxModel extends AbstractListModel<DocumentSearchResult>
        implements ComboBoxModel<DocumentSearchResult> {

    private static final long serialVersionUID = 1L;

    /** Current elements, immutable. */
    private transient List<DocumentSearchResult> results = List.of();

    /** Selected element or {@code null} (Swing API). */
    private transient Object selectedItem;

    /** Creates an empty model. */
    public SearchResultComboBoxModel() {
        // empty
    }

    /**
     * Replaces all elements (D-07, D-09).
     *
     * @param sortedResults the results, not {@code null}
     */
    public void setResults(List<DocumentSearchResult> sortedResults) {
        Objects.requireNonNull(sortedResults, "sortedResults");
        int oldSize = results.size();
        results = List.copyOf(sortedResults);
        selectedItem = null;
        if (oldSize > 0) {
            fireIntervalRemoved(this, 0, oldSize - 1);
        }
        fireContentsChanged(this, -1, -1);
        if (!results.isEmpty()) {
            fireIntervalAdded(this, 0, results.size() - 1);
        }
    }

    /**
     * Returns the current elements.
     *
     * @return immutable list
     */
    public List<DocumentSearchResult> results() {
        return results;
    }

    @Override
    public int getSize() {
        return results.size();
    }

    @Override
    public DocumentSearchResult getElementAt(int index) {
        return results.get(index);
    }

    /**
     * Sets the selection and fires {@code contentsChanged} (D-08). Accepts {@code null} or an
     * element of the model; anything else is ignored.
     *
     * @param item the new selection
     */
    @Override
    public void setSelectedItem(Object item) {
        if (item != null && !results.contains(item)) {
            return;
        }
        if (Objects.equals(selectedItem, item)) {
            return;
        }
        selectedItem = item;
        fireContentsChanged(this, -1, -1);
    }

    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }
}
