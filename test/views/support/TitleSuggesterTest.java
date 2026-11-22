package views.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.DocumentTitles;

/**
 * Tests for {@code TitleSuggester}. Owner: Member B.
 *
 * <p>Cases B-52..B-56 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class TitleSuggesterTest {

    /**
     * B-52: default title = file name without extension (D1) [unit].
     *
     * <p>Arrange: Path.of('dir', 'notes.txt').</p>
     * <p>Act: suggestTitle.</p>
     * <p>Assert: 'notes'.</p>
     */
    @Test
    @DisplayName("B-52 default title = file name without extension (D1)")
    void suggestTitle_txtFile_dropsExtension() {
        assertEquals("notes", TitleSuggester.suggestTitle(Path.of("dir", "notes.txt")));
    }

    /**
     * B-53: extension rules [unit].
     *
     * <p>Arrange: 'archive.tar.gz', 'README', 'notes.'.</p>
     * <p>Act: suggestTitle.</p>
     * <p>Assert: 'archive.tar', 'README', 'notes'.</p>
     */
    @Test
    @DisplayName("B-53 extension rules")
    void suggestTitle_multipleOrNoExtensions_dropsLastOnly() {
        assertEquals("archive.tar", TitleSuggester.suggestTitle(Path.of("archive.tar.gz")));
        assertEquals("README", TitleSuggester.suggestTitle(Path.of("README")));
        assertEquals("notes", TitleSuggester.suggestTitle(Path.of("notes.")));
    }

    /**
     * B-54: dot files [unit].
     *
     * <p>Arrange: '.bashrc'.</p>
     * <p>Act: suggestTitle.</p>
     * <p>Assert: '.bashrc'.</p>
     */
    @Test
    @DisplayName("B-54 dot files")
    void suggestTitle_hiddenFile_keepsLeadingDot() {
        assertEquals(".bashrc", TitleSuggester.suggestTitle(Path.of(".bashrc")));
    }

    /**
     * B-55: length and whitespace (D1) [unit].
     *
     * <p>Arrange: '  x...x(250 chars).txt'.</p>
     * <p>Act: suggestTitle.</p>
     * <p>Assert: 200 chars, trimmed, DocumentTitles.isValid true.</p>
     */
    @Test
    @DisplayName("B-55 length and whitespace (D1)")
    void suggestTitle_longOrPaddedName_truncatedAndTrimmed() {
        String title = TitleSuggester.suggestTitle(Path.of("  " + "x".repeat(250) + ".txt"));
        assertEquals(200, title.length());
        assertEquals(title.trim(), title);
        assertTrue(DocumentTitles.isValid(title));
    }

    /**
     * B-56: always a valid suggestion [unit].
     *
     * <p>Arrange: 'caf\u00e9.txt'.</p>
     * <p>Act: suggestTitle.</p>
     * <p>Assert: 'caf_'.</p>
     */
    @Test
    @DisplayName("B-56 always a valid suggestion")
    void suggestTitle_nonAsciiCharacters_replacedByUnderscore() {
        assertEquals("caf_", TitleSuggester.suggestTitle(Path.of("caf\u00e9.txt")));
        assertThrows(NullPointerException.class, () -> TitleSuggester.suggestTitle(null));
    }
}
