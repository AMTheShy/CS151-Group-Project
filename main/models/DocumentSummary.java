package models;

import java.util.Objects;

/**
 * One row of the document library table (UC3, D7): the document and its precomputed preview.
 *
 * <p>The preview is computed off the EDT by the controller (with
 * {@code models.search.SnippetExtractor#preview}) so that the view never has to touch the
 * full content and never depends on the search module.</p>
 *
 * <p>Invariants: {@code document} and {@code preview} are not {@code null};
 * {@code preview.length() <= AppConstants.PREVIEW_LENGTH}; {@code preview} contains neither
 * {@code '\r'} nor {@code '\n'}.</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param document the document snapshot
 * @param preview  the first up-to-500 characters with line breaks replaced by spaces
 */
public record DocumentSummary(Document document, String preview) {

    /**
     * Validates the invariants.
     *
     * @throws NullPointerException     if an argument is {@code null}
     * @throws IllegalArgumentException if the preview is too long or contains a line break
     */
    public DocumentSummary {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(preview, "preview");
        if (preview.length() > AppConstants.PREVIEW_LENGTH) {
            throw new IllegalArgumentException("preview longer than " + AppConstants.PREVIEW_LENGTH);
        }
        if (preview.indexOf('\n') >= 0 || preview.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("preview must not contain line breaks");
        }
    }

    /**
     * Convenience accessor for the document title.
     *
     * @return {@code document().title()}
     */
    public String title() {
        return document.title();
    }
}
