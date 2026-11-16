package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Document}. Owner: Eric. Reference example of the team test style:
 * one behavior per test, Arrange-Act-Assert, method names {@code method_condition_expectedResult}.
 */
class DocumentTest {

    private static final Instant IMPORTED_AT = Instant.parse("2026-11-09T12:00:00Z");

    /** E-01: a valid document exposes its fields and length. */
    @Test
    @DisplayName("E-01 constructor with valid arguments exposes title, content, importedAt and length")
    void constructor_validArguments_exposesFields() {
        // Arrange
        String content = "line one\r\nline two";

        // Act
        Document document = new Document("Notes", content, IMPORTED_AT);

        // Assert
        assertEquals("Notes", document.title());
        assertEquals(content, document.content());
        assertEquals(IMPORTED_AT, document.importedAt());
        assertEquals(content.length(), document.length());
    }

    /** E-02: invalid or un-normalized titles are rejected. */
    @Test
    @DisplayName("E-02 constructor with blank, untrimmed or too long title throws IllegalArgumentException")
    void constructor_invalidTitle_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> new Document("   ", "x", IMPORTED_AT));
        assertThrows(IllegalArgumentException.class, () -> new Document(" Notes", "x", IMPORTED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> new Document("t".repeat(AppConstants.MAX_TITLE_LENGTH + 1), "x", IMPORTED_AT));
    }

    /** E-03: equality uses title and content but ignores importedAt. */
    @Test
    @DisplayName("E-03 equals and hashCode use title and content, ignore importedAt")
    void equals_sameTitleAndContentDifferentInstant_areEqual() {
        // Arrange
        Document first = new Document("Notes", "abc", IMPORTED_AT);
        Document second = new Document("Notes", "abc", IMPORTED_AT.plusSeconds(60));
        Document otherContent = new Document("Notes", "abd", IMPORTED_AT);
        Document otherCase = new Document("notes", "abc", IMPORTED_AT);

        // Act & Assert
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, otherContent);
        assertNotEquals(first, otherCase);
    }

    /** E-04: null arguments are rejected with NullPointerException. */
    @Test
    @DisplayName("E-04 constructor with null argument throws NullPointerException")
    void constructor_nullArgument_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Document(null, "x", IMPORTED_AT));
        assertThrows(NullPointerException.class, () -> new Document("Notes", null, IMPORTED_AT));
        assertThrows(NullPointerException.class, () -> new Document("Notes", "x", null));
    }

    /** E-05: toString never dumps the content. */
    @Test
    @DisplayName("E-05 toString contains title and length but not the content")
    void toString_largeContent_omitsContent() {
        // Arrange
        Document document = new Document("Big", "secret-content", IMPORTED_AT);

        // Act
        String text = document.toString();

        // Assert
        assertTrue(text.contains("Big"));
        assertTrue(text.contains("14"));
        assertFalse(text.contains("secret-content"));
    }
}
