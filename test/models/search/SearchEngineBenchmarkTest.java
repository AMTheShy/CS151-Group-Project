package models.search;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@code LinearScanSearchEngine + DefaultSnippetExtractor}. Owner: Member B.
 *
 * <p>Cases B-44..B-46 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class SearchEngineBenchmarkTest {

    private LinearScanSearchEngine engine;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        engine = new LinearScanSearchEngine(new DefaultSnippetExtractor());
    }

    /**
     * B-44: single 5 MB document &lt;= 100 ms (3.4) [benchmark].
     *
     * <p>Arrange: TestDocuments.asciiText(5 MB, seed 1); queries 'computer algorithm' and 'e'; 5 warm-
     * ups.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: median &lt;= 100 ms for both queries.</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member B")
    @DisplayName("B-44 single 5 MB document <= 100 ms (3.4)")
    void findAllMatches_fiveMegabyteDocument_completesWithin100Milliseconds() {
        fail("TODO: Member B - B-44");
    }

    /**
     * B-45: snippet/preview never copy the document [benchmark].
     *
     * <p>Arrange: 5 MB text; extract at middle; preview 500.</p>
     * <p>Act: extract / preview.</p>
     * <p>Assert: each call &lt;= 1 ms (median of 100).</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member B")
    @DisplayName("B-45 snippet/preview never copy the document")
    void snippetAndPreview_fiveMegabyteDocument_areConstantTime() {
        fail("TODO: Member B - B-45");
    }

    /**
     * B-46: throughput basis for 100 MB / 1 s with parallelism [benchmark].
     *
     * <p>Arrange: 20 documents x 5 MB; sequential loop on one thread.</p>
     * <p>Act: search.</p>
     * <p>Assert: average per document &lt;= 100 ms (so 100 MB &lt;= 1 s on &gt;= 2 cores; end-to-end check
     * is I-10).</p>
     */
    @Test
    @Tag("benchmark")
    @Disabled("TODO: Member B")
    @DisplayName("B-46 throughput basis for 100 MB / 1 s with parallelism")
    void search_twentyFiveMegabyteDocumentsSequential_averageWithin100Milliseconds() {
        fail("TODO: Member B - B-46");
    }
}
