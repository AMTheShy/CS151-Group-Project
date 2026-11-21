package models.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.MatchRange;
import models.Snippet;
import testsupport.TestDocuments;

/**
 * Tests for {@code DefaultSnippetExtractor}. Owner: Member B.
 *
 * <p>Cases B-19..B-29 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class DefaultSnippetExtractorTest {

    private DefaultSnippetExtractor extractor;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        extractor = new DefaultSnippetExtractor();
    }

    /**
     * B-19: context on both sides (S4) [unit].
     *
     * <p>Arrange: resource snippet-boundaries.txt; match [100,106); context 40.</p>
     * <p>Act: extract.</p>
     * <p>Assert: '...' + 40 x + NEEDLE + 40 y + '...'; highlight [43,49); both flags true.</p>
     */
    @Test
    @DisplayName("B-19 context on both sides (S4)")
    void extract_matchInMiddle_hasBothEllipsesAndCorrectHighlight() throws Exception {
        String text = TestDocuments.readResource(TestDocuments.RESOURCE_SNIPPET_BOUNDARIES);
        Snippet snippet = extractor.extract(text, new MatchRange(100, 106), 40);
        assertEquals("..." + "x".repeat(40) + "NEEDLE" + "y".repeat(40) + "...", snippet.text());
        assertEquals(new MatchRange(43, 49), snippet.highlight());
        assertTrue(snippet.truncatedStart());
        assertTrue(snippet.truncatedEnd());
    }

    /**
     * B-20: truncation at document start [unit].
     *
     * <p>Arrange: match starting at index 5; context 40.</p>
     * <p>Act: extract.</p>
     * <p>Assert: text starts with the document start; truncatedStart false.</p>
     */
    @Test
    @DisplayName("B-20 truncation at document start")
    void extract_matchNearStart_noLeadingEllipsis() {
        String text = "helloNEEDLE" + "x".repeat(80);
        Snippet snippet = extractor.extract(text, new MatchRange(5, 11), 40);
        assertTrue(snippet.text().startsWith("hello"));
        assertFalse(snippet.truncatedStart());
    }

    /**
     * B-21: truncation at document end [unit].
     *
     * <p>Arrange: match ending 3 chars before end; context 40.</p>
     * <p>Act: extract.</p>
     * <p>Assert: truncatedEnd false; text ends with document end.</p>
     */
    @Test
    @DisplayName("B-21 truncation at document end")
    void extract_matchNearEnd_noTrailingEllipsis() {
        String text = "x".repeat(80) + "NEEDLEend";
        Snippet snippet = extractor.extract(text, new MatchRange(80, 86), 40);
        assertFalse(snippet.truncatedEnd());
        assertTrue(snippet.text().endsWith("end"));
    }

    /**
     * B-22: short document [unit].
     *
     * <p>Arrange: text 'a NEEDLE b'.</p>
     * <p>Act: extract.</p>
     * <p>Assert: snippet text equals the whole text; no ellipsis.</p>
     */
    @Test
    @DisplayName("B-22 short document")
    void extract_shortDocument_wholeTextWithoutEllipsis() {
        String text = "a NEEDLE b";
        Snippet snippet = extractor.extract(text, new MatchRange(2, 8), 40);
        assertEquals(text, snippet.text());
        assertFalse(snippet.truncatedStart());
        assertFalse(snippet.truncatedEnd());
    }

    /**
     * B-23: boundary: exactly 40 before [unit].
     *
     * <p>Arrange: 40 chars then match.</p>
     * <p>Act: extract.</p>
     * <p>Assert: truncatedStart false.</p>
     */
    @Test
    @DisplayName("B-23 boundary: exactly 40 before")
    void extract_exactlyContextCharsBefore_noLeadingEllipsis() {
        String text = "x".repeat(40) + "NEEDLE";
        Snippet snippet = extractor.extract(text, new MatchRange(40, 46), 40);
        assertFalse(snippet.truncatedStart());
        assertEquals(40, snippet.highlight().start());
    }

    /**
     * B-24: boundary: 41 before [unit].
     *
     * <p>Arrange: 41 chars then match.</p>
     * <p>Act: extract.</p>
     * <p>Assert: truncatedStart true; first context char is text[1].</p>
     */
    @Test
    @DisplayName("B-24 boundary: 41 before")
    void extract_contextPlusOneCharsBefore_leadingEllipsis() {
        String text = "x".repeat(41) + "NEEDLE";
        Snippet snippet = extractor.extract(text, new MatchRange(41, 47), 40);
        assertTrue(snippet.truncatedStart());
        assertEquals("..." + text.substring(1), snippet.text());
        assertEquals(new MatchRange(43, 49), snippet.highlight());
    }

    /**
     * B-25: line breaks replaced for display (S4) [unit].
     *
     * <p>Arrange: 'ab\r\ncd NEEDLE ef\ngh'.</p>
     * <p>Act: extract.</p>
     * <p>Assert: no CR/LF in snippet; same length as source window; highlight still on NEEDLE.</p>
     */
    @Test
    @DisplayName("B-25 line breaks replaced for display (S4)")
    void extract_lineBreaksInContext_replacedOneToOne() {
        String text = "ab\r\ncd NEEDLE ef\ngh";
        Snippet snippet = extractor.extract(text, new MatchRange(7, 13), 40);
        assertEquals(text.length(), snippet.text().length());
        assertFalse(snippet.text().contains("\r"));
        assertFalse(snippet.text().contains("\n"));
        assertEquals("NEEDLE", snippet.highlightedText());
    }

    /**
     * B-26: context 0 [unit].
     *
     * <p>Arrange: middle match; context 0.</p>
     * <p>Act: extract.</p>
     * <p>Assert: '...' + match + '...'; highlight [3, 3+len).</p>
     */
    @Test
    @DisplayName("B-26 context 0")
    void extract_zeroContext_onlyMatchedText() {
        String text = "beforeNEEDLEafter";
        Snippet snippet = extractor.extract(text, new MatchRange(6, 12), 0);
        assertEquals("...NEEDLE...", snippet.text());
        assertEquals(new MatchRange(3, 9), snippet.highlight());
    }

    /**
     * B-27: argument validation [unit].
     *
     * <p>Arrange: match end &gt; text length; context -1.</p>
     * <p>Act: extract.</p>
     * <p>Assert: IllegalArgumentException.</p>
     */
    @Test
    @DisplayName("B-27 argument validation")
    void extract_invalidArguments_throwIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> extractor.extract("abc", new MatchRange(2, 4), 40));
        assertThrows(IllegalArgumentException.class,
                () -> extractor.extract("abc", new MatchRange(0, 1), -1));
        assertThrows(NullPointerException.class,
                () -> extractor.extract(null, new MatchRange(0, 1), 0));
    }

    /**
     * B-28: library preview (D7) [unit].
     *
     * <p>Arrange: 1000-char text containing LF; maxChars 500; also a 10-char text.</p>
     * <p>Act: preview.</p>
     * <p>Assert: 500 chars, no CR/LF; short text returned whole.</p>
     */
    @Test
    @DisplayName("B-28 library preview (D7)")
    void preview_longTextWithLineBreaks_first500CharsWithoutLineBreaks() {
        String text = "a".repeat(200) + "\n" + "b".repeat(799);
        String preview = extractor.preview(text, 500);
        assertEquals(500, preview.length());
        assertFalse(preview.contains("\r"));
        assertFalse(preview.contains("\n"));
        assertEquals("short text", extractor.preview("short text", 500));
    }

    /**
     * B-29: preview limits [unit].
     *
     * <p>Arrange: maxChars 0; maxChars -1.</p>
     * <p>Act: preview.</p>
     * <p>Assert: '' for 0; IllegalArgumentException for -1.</p>
     */
    @Test
    @DisplayName("B-29 preview limits")
    void preview_zeroOrNegativeMax_handledPerContract() {
        assertEquals("", extractor.preview("abc", 0));
        assertThrows(IllegalArgumentException.class, () -> extractor.preview("abc", -1));
        assertThrows(NullPointerException.class, () -> extractor.preview(null, 4));
    }
}
