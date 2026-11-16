package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DocumentTitles} (D1, N2). Owner: Eric. */
class DocumentTitlesTest {

    /** E-06: normalization trims leading and trailing whitespace only. */
    @Test
    @DisplayName("E-06 normalize trims leading and trailing whitespace, keeps inner spaces")
    void normalize_paddedTitle_returnsTrimmedTitle() {
        assertEquals("My  Notes", DocumentTitles.normalize("  My  Notes \t"));
    }

    /** E-07: empty or blank input is invalid. */
    @Test
    @DisplayName("E-07 validate with blank input returns an error message")
    void validate_blankInput_returnsError() {
        Optional<String> error = DocumentTitles.validate(" \t ");

        assertTrue(error.isPresent());
        assertTrue(error.get().contains("empty"));
    }

    /** E-08: the length limit is inclusive at 200 characters. */
    @Test
    @DisplayName("E-08 validate accepts exactly 200 characters and rejects 201")
    void validate_lengthBoundary_acceptsMaxRejectsMaxPlusOne() {
        String atLimit = "a".repeat(AppConstants.MAX_TITLE_LENGTH);
        String overLimit = atLimit + "a";

        assertTrue(DocumentTitles.isValid(atLimit));
        assertFalse(DocumentTitles.isValid(overLimit));
    }

    /** E-09: control characters and non-ASCII characters are rejected. */
    @Test
    @DisplayName("E-09 validate rejects inner control characters and non-ASCII characters")
    void validate_nonPrintableCharacters_returnsError() {
        assertFalse(DocumentTitles.isValid("line\nbreak"));
        assertFalse(DocumentTitles.isValid("tab\tinside"));
        assertFalse(DocumentTitles.isValid("caf\u00e9"));
        assertTrue(DocumentTitles.isValid("a/b\\c:d*e?f\"g<h>i|j"));
    }

    /** E-10: requireValid demands an already-normalized title. */
    @Test
    @DisplayName("E-10 requireValid rejects untrimmed title and returns valid title unchanged")
    void requireValid_untrimmedTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> DocumentTitles.requireValid(" Notes"));
        assertThrows(NullPointerException.class, () -> DocumentTitles.requireValid(null));
        assertEquals("Notes", DocumentTitles.requireValid("Notes"));
    }
}
