package models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import testsupport.TestDocuments;

/** Unit tests for {@link AppConstants#TITLE_ORDER} and {@link AppConstants#DOCUMENT_TITLE_ORDER} (D6). Owner: Eric. */
class TitleOrderTest {

    /** E-39: primary key is case-insensitive. */
    @Test
    @DisplayName("E-39 TITLE_ORDER sorts case-insensitively first")
    void titleOrder_mixedCaseTitles_sortsCaseInsensitively() {
        List<String> titles = new ArrayList<>(List.of("banana", "Apple", "cherry", "apricot"));

        titles.sort(AppConstants.TITLE_ORDER);

        assertEquals(List.of("Apple", "apricot", "banana", "cherry"), titles);
    }

    /** E-40: ties are broken case-sensitively, making the order total and consistent with equals. */
    @Test
    @DisplayName("E-40 TITLE_ORDER breaks ties case-sensitively and is zero only for equal strings")
    void titleOrder_caseVariants_tieBrokenCaseSensitively() {
        List<String> titles = new ArrayList<>(List.of("notes", "Notes", "NOTES"));

        titles.sort(AppConstants.TITLE_ORDER);

        assertEquals(List.of("NOTES", "Notes", "notes"), titles);
        assertTrue(AppConstants.TITLE_ORDER.compare("Notes", "notes") != 0);
        assertEquals(0, AppConstants.TITLE_ORDER.compare("Notes", "Notes"));
    }

    /** E-41: usable as a sorted-set comparator keeping case variants distinct (D1). */
    @Test
    @DisplayName("E-41 TITLE_ORDER keeps case variants as distinct keys in a sorted set")
    void titleOrder_inTreeSet_keepsCaseVariantsDistinct() {
        TreeSet<String> set = new TreeSet<>(AppConstants.TITLE_ORDER);

        set.addAll(List.of("Foo", "foo", "FOO", "foo"));

        assertEquals(3, set.size());
    }

    /** E-42: the document comparator delegates to the title order. */
    @Test
    @DisplayName("E-42 DOCUMENT_TITLE_ORDER sorts documents by title using TITLE_ORDER")
    void documentTitleOrder_documents_sortedByTitle() {
        List<Document> documents = new ArrayList<>(List.of(
                TestDocuments.document("b"), TestDocuments.document("A"), TestDocuments.document("a")));

        documents.sort(AppConstants.DOCUMENT_TITLE_ORDER);

        assertEquals(List.of("A", "a", "b"), documents.stream().map(Document::title).toList());
    }
}
