package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link InvalidDocumentException}. Owner: Eric. */
class InvalidDocumentExceptionTest {

    /** E-47: the reason is exposed and the exception is a checked DocumentException. */
    @Test
    @DisplayName("E-47 exposes reason and message and is a checked DocumentException")
    void constructor_reasonAndMessage_exposesBoth() {
        InvalidDocumentException exception = new InvalidDocumentException(
                InvalidDocumentException.Reason.FILE_TOO_LARGE, "The file is larger than 20 MB.");

        assertEquals(InvalidDocumentException.Reason.FILE_TOO_LARGE, exception.reason());
        assertEquals("The file is larger than 20 MB.", exception.getMessage());
        assertTrue(exception instanceof DocumentException);
        assertThrows(NullPointerException.class, () -> new InvalidDocumentException(null, "x"));
    }
}
