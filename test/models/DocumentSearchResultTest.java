package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.TestDocuments;

/** Unit tests for {@link DocumentSearchResult} (S3, S5). Owner: Eric. */
class DocumentSearchResultTest {

    private static final Snippet SNIPPET = new Snippet("ab", new MatchRange(0, 2), false, false);

    /** E-28: a valid result exposes counts and convenience accessors. */
    @Test
    @DisplayName("E-28 constructor with valid matches exposes matchCount, firstMatch and title")
    void constructor_validMatches_exposesCounts() {
        Document document = TestDocuments.document("Doc", "ab ab ab");
        MatchRanges matches = MatchRanges.of(2, 0, 3, 6);

        DocumentSearchResult result = new DocumentSearchResult(document, matches, SNIPPET);

        assertEquals(3, result.matchCount());
        assertEquals(new MatchRange(0, 2), result.firstMatch());
        assertEquals("Doc", result.title());
        assertSame(matches, result.matches());
    }

    /** E-29: a result needs at least one match. */
    @Test
    @DisplayName("E-29 constructor with empty matches throws IllegalArgumentException")
    void constructor_emptyMatches_throwsIllegalArgumentException() {
        Document document = TestDocuments.document("Doc", "ab");

        assertThrows(IllegalArgumentException.class,
                () -> new DocumentSearchResult(document, List.of(), SNIPPET));
    }

    /** E-30: matches must be sorted, non-overlapping and inside the document. */
    @Test
    @DisplayName("E-30 constructor rejects overlapping, unsorted or out-of-document matches")
    void constructor_invalidMatches_throwsIllegalArgumentException() {
        Document document = TestDocuments.document("Doc", "abab");

        assertThrows(IllegalArgumentException.class, () -> new DocumentSearchResult(document,
                List.of(new MatchRange(0, 3), new MatchRange(2, 4)), SNIPPET));
        assertThrows(IllegalArgumentException.class, () -> new DocumentSearchResult(document,
                List.of(new MatchRange(2, 4), new MatchRange(0, 2)), SNIPPET));
        assertThrows(IllegalArgumentException.class, () -> new DocumentSearchResult(document,
                List.of(new MatchRange(3, 5)), SNIPPET));
    }

    /** E-31: plain lists are defensively copied. */
    @Test
    @DisplayName("E-31 constructor copies a mutable list so later changes do not leak in")
    void constructor_mutableList_isDefensivelyCopied() {
        Document document = TestDocuments.document("Doc", "abab");
        List<MatchRange> mutable = new ArrayList<>(List.of(new MatchRange(0, 2)));

        DocumentSearchResult result = new DocumentSearchResult(document, mutable, SNIPPET);
        mutable.add(new MatchRange(2, 4));

        assertEquals(1, result.matchCount());
        assertThrows(UnsupportedOperationException.class, () -> result.matches().add(new MatchRange(2, 4)));
    }
}
