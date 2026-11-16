package models;

import java.util.List;
import java.util.Objects;

/**
 * The result of searching one document: the document snapshot, all its matches and the snippet
 * of the first match (S3, S4, S5).
 *
 * <p>A result exists only for documents that match at least once; non-matching documents are
 * represented by {@code Optional.empty()} in {@code SearchEngine.search} and are never shown in
 * the drop-down list (U4).</p>
 *
 * <p>Invariants (checked by the constructor in O(number of matches)):</p>
 * <ul>
 *   <li>{@code document}, {@code matches} and {@code snippet} are not {@code null}.</li>
 *   <li>{@code matches} is non-empty, ascending by start, non-overlapping, and every range ends
 *       at or before {@code document.length()}.</li>
 *   <li>{@code matches} is immutable: a {@link MatchRanges} is kept as is, any other list is
 *       copied with {@link List#copyOf(java.util.Collection)}.</li>
 * </ul>
 *
 * <p>The result holds the immutable {@link Document} itself, so the reader and the download keep
 * working even if the document is deleted or replaced after the search (S5, D8).</p>
 *
 * <p>Owner: Eric (contract layer, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 *
 * @param document the searched document snapshot
 * @param matches  every non-overlapping match, ascending
 * @param snippet  the snippet around {@code matches.get(0)}
 */
public record DocumentSearchResult(Document document, List<MatchRange> matches, Snippet snippet) {

    /**
     * Validates and defensively copies the arguments.
     *
     * @throws NullPointerException     if any argument or list element is {@code null}
     * @throws IllegalArgumentException if {@code matches} is empty, unsorted, overlapping or
     *                                  exceeds the document length
     */
    public DocumentSearchResult {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(matches, "matches");
        Objects.requireNonNull(snippet, "snippet");
        matches = matches instanceof MatchRanges ? matches : List.copyOf(matches);
        if (matches.isEmpty()) {
            throw new IllegalArgumentException("a search result needs at least one match");
        }
        int previousEnd = 0;
        for (MatchRange range : matches) {
            if (range.start() < previousEnd) {
                throw new IllegalArgumentException("matches must be ascending and non-overlapping at " + range);
            }
            previousEnd = range.end();
        }
        if (previousEnd > document.length()) {
            throw new IllegalArgumentException("match ends at " + previousEnd
                    + " beyond document length " + document.length());
        }
    }

    /**
     * Returns the number of matches in the document.
     *
     * @return {@code matches().size()}, always {@code >= 1}
     */
    public int matchCount() {
        return matches.size();
    }

    /**
     * Returns the first match, which the snippet is centered on.
     *
     * @return {@code matches().get(0)}
     */
    public MatchRange firstMatch() {
        return matches.get(0);
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
