package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link Snippet} (S4). Owner: Eric. */
class SnippetTest {

    /** E-25: a valid truncated snippet. */
    @Test
    @DisplayName("E-25 constructor accepts a snippet with both ellipses and returns highlighted text")
    void constructor_validTruncatedSnippet_exposesHighlightedText() {
        Snippet snippet = new Snippet("...efgXYZhij...", new MatchRange(6, 9), true, true);

        assertEquals("XYZ", snippet.highlightedText());
    }

    /** E-26: highlight outside text or line breaks are rejected. */
    @Test
    @DisplayName("E-26 constructor rejects highlight beyond text and text containing line breaks")
    void constructor_highlightBeyondText_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Snippet("abc", new MatchRange(1, 4), false, false));
        assertThrows(IllegalArgumentException.class, () -> new Snippet("a\nbc", new MatchRange(0, 1), false, false));
        assertThrows(NullPointerException.class, () -> new Snippet(null, new MatchRange(0, 1), false, false));
    }

    /** E-27: truncation flags require matching ellipses outside the highlight. */
    @Test
    @DisplayName("E-27 constructor rejects truncation flags without ellipsis or with highlight inside ellipsis")
    void constructor_flagWithoutEllipsis_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Snippet("abcdef", new MatchRange(3, 4), true, false));
        assertThrows(IllegalArgumentException.class, () -> new Snippet("...abc", new MatchRange(1, 4), true, false));
        assertThrows(IllegalArgumentException.class, () -> new Snippet("abc...", new MatchRange(2, 5), false, true));
    }
}
