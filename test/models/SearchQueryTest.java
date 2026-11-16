package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SearchQuery} (S2). Owner: Eric. */
class SearchQueryTest {

    /** E-11: only the ends are trimmed; inner whitespace is preserved exactly. */
    @Test
    @DisplayName("E-11 of trims both ends and preserves inner whitespace verbatim")
    void of_paddedQueryWithDoubleSpace_trimsEndsKeepsInnerSpaces() {
        SearchQuery query = SearchQuery.of("  Computer  Algorithm\t");

        assertEquals("Computer  Algorithm", query.text());
        assertEquals(19, query.length());
    }

    /** E-12: a blank query is rejected. */
    @Test
    @DisplayName("E-12 of with blank input throws IllegalArgumentException with message")
    void of_blankInput_throwsIllegalArgumentException() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> SearchQuery.of("   "));

        assertTrue(error.getMessage().contains("empty"));
        assertTrue(SearchQuery.validate("").isPresent());
    }

    /** E-13: non-ASCII queries are rejected. */
    @Test
    @DisplayName("E-13 of with non-ASCII character throws IllegalArgumentException")
    void of_nonAsciiInput_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> SearchQuery.of("caf\u00e9"));
    }

    /** E-14: folded text is ASCII lower case with identical length and positions. */
    @Test
    @DisplayName("E-14 foldedText is ASCII lower case with the same length as text")
    void foldedText_mixedCase_isLowerCaseSameLength() {
        SearchQuery query = SearchQuery.of("CoMpUtEr_ALGO 42!");

        assertEquals("computer_algo 42!", query.foldedText());
        assertEquals(query.text().length(), query.foldedText().length());
    }

    /** E-15: equality is by (case-sensitive) normalized text. */
    @Test
    @DisplayName("E-15 equals compares normalized text case-sensitively")
    void equals_sameNormalizedText_areEqual() {
        assertEquals(SearchQuery.of(" abc "), SearchQuery.of("abc"));
        assertEquals(SearchQuery.of("abc").hashCode(), SearchQuery.of(" abc").hashCode());
        assertNotEquals(SearchQuery.of("abc"), SearchQuery.of("ABC"));
    }

    /** E-16: null input is a programming error. */
    @Test
    @DisplayName("E-16 of and validate with null throw NullPointerException")
    void of_nullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> SearchQuery.of(null));
        assertThrows(NullPointerException.class, () -> SearchQuery.validate(null));
    }
}
