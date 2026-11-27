package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static views.SwingTestSupport.onEdtGet;

import java.util.List;

import javax.swing.JList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import models.DocumentSearchResult;
import models.MatchRange;
import models.Snippet;
import testsupport.TestDocuments;

/**
 * Tests for {@code SearchResultCellRenderer}. Owner: Member D.
 *
 * <p>Cases D-33..D-33 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchResultCellRendererTest {

    private SearchResultCellRenderer renderer;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        renderer = onEdtGet(SearchResultCellRenderer::new);
    }

    /**
     * D-33: two columns: title | snippet (UC2, U4) [unit].
     *
     * <p>Arrange: result with snippet 'a&lt;b NEEDLE &amp;c' highlight on NEEDLE.</p>
     * <p>Act: getListCellRendererComponent / snippetHtml.</p>
     * <p>Assert: titleLabel text = title; snippet HTML escapes &lt; and &amp;; NEEDLE wrapped in &lt;b&gt;;
     * null value renders 'Select a document'.</p>
     */
    @Test
    @DisplayName("D-33 two columns: title | snippet (UC2, U4)")
    void render_result_showsTitleAndEscapedHighlightedSnippet() {
        // Arrange
        String text = "a<b NEEDLE &c";
        DocumentSearchResult result = new DocumentSearchResult(TestDocuments.document("Doc \"1\"", text),
                List.of(new MatchRange(4, 10)), new Snippet(text, new MatchRange(4, 10), false, false));
        JList<DocumentSearchResult> list = onEdtGet(JList::new);

        // Act
        List<String> resultTexts = onEdtGet(() -> {
            renderer.getListCellRendererComponent(list, result, 0, false, false);
            return List.of(renderer.titleLabel().getText(), renderer.snippetLabel().getText());
        });
        String nullTitle = onEdtGet(() -> {
            renderer.getListCellRendererComponent(list, null, -1, false, false);
            return renderer.titleLabel().getText();
        });

        // Assert
        assertEquals("Doc \"1\"", resultTexts.get(0));
        assertEquals("<html>a&lt;b&nbsp;<b>NEEDLE</b>&nbsp;&amp;c</html>", resultTexts.get(1));
        assertEquals("Select a document", nullTitle);
    }

    /**
     * D-51: consecutive spaces survive in the snippet HTML (whitespace is significant in a query).
     */
    @Test
    @DisplayName("D-51 snippet keeps consecutive spaces (S2, U4)")
    void snippetHtml_consecutiveSpaces_areKeptAsNonBreakingSpaces() {
        // Arrange
        String text = "a Computer  Algorithm b";
        MatchRange match = new MatchRange(2, 21);
        DocumentSearchResult result = new DocumentSearchResult(TestDocuments.document("Doc", text),
                List.of(match), new Snippet(text, match, false, false));

        // Act
        String html = SearchResultCellRenderer.snippetHtml(result);

        // Assert
        assertEquals("<html>a&nbsp;<b>Computer&nbsp;&nbsp;Algorithm</b>&nbsp;b</html>", html);
    }
}
