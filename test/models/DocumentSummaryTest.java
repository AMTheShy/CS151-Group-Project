package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.TestDocuments;

/** Unit tests for {@link DocumentSummary} (D7). Owner: Eric. */
class DocumentSummaryTest {

    /** E-32: preview length and line-break rules are enforced. */
    @Test
    @DisplayName("E-32 constructor rejects preview longer than 500 or containing line breaks")
    void constructor_invalidPreview_throwsIllegalArgumentException() {
        Document document = TestDocuments.document("Doc");

        assertEquals("Doc", new DocumentSummary(document, "a".repeat(AppConstants.PREVIEW_LENGTH)).title());
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentSummary(document, "a".repeat(AppConstants.PREVIEW_LENGTH + 1)));
        assertThrows(IllegalArgumentException.class, () -> new DocumentSummary(document, "a\nb"));
        assertThrows(IllegalArgumentException.class, () -> new DocumentSummary(document, "a\rb"));
    }
}
