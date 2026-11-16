package models;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DuplicateTitleException} and {@link DocumentNotFoundException}. Owner: Eric. */
class DuplicateTitleExceptionTest {

    /** E-48: standard user-presentable messages. */
    @Test
    @DisplayName("E-48 builds the standard user-presentable message and exposes the title")
    void constructor_title_buildsStandardMessage() {
        DuplicateTitleException duplicate = new DuplicateTitleException("Notes");
        DocumentNotFoundException notFound = new DocumentNotFoundException("Notes");

        assertEquals("A document titled \"Notes\" already exists.", duplicate.getMessage());
        assertEquals("Notes", duplicate.title());
        assertEquals("No document titled \"Notes\" was found.", notFound.getMessage());
    }
}
