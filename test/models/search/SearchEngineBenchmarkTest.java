package models.search;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import models.CancellationToken;
import models.Document;
import models.MatchRange;
import models.SearchQuery;
import testsupport.TestDocuments;

/**
 * Tests for {@code LinearScanSearchEngine + DefaultSnippetExtractor}. Owner: Member B.
 *
 * <p>Cases B-44..B-46 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
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
    @DisplayName("B-44 single 5 MB document <= 100 ms (3.4)")
    void findAllMatches_fiveMegabyteDocument_completesWithin100Milliseconds() {
        String text = TestDocuments.asciiText(5 * 1024 * 1024, 1L);
        for (String rawQuery : List.of("computer algorithm", "e")) {
            SearchQuery query = SearchQuery.of(rawQuery);
            for (int warmup = 0; warmup < 5; warmup++) {
                engine.findAllMatches(text, query, CancellationToken.NONE);
            }
            long[] times = new long[5];
            for (int round = 0; round < times.length; round++) {
                long start = System.nanoTime();
                engine.findAllMatches(text, query, CancellationToken.NONE);
                times[round] = System.nanoTime() - start;
            }
            Arrays.sort(times);
            double medianMillis = times[times.length / 2] / 1_000_000.0;
            assertTrue(medianMillis <= 100, rawQuery + " median: " + medianMillis + " ms");
        }
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
    @DisplayName("B-45 snippet/preview never copy the document")
    void snippetAndPreview_fiveMegabyteDocument_areConstantTime() {
        String text = TestDocuments.asciiText(5 * 1024 * 1024, 1L);
        DefaultSnippetExtractor extractor = new DefaultSnippetExtractor();
        MatchRange middle = new MatchRange(text.length() / 2, text.length() / 2 + 6);
        for (int warmup = 0; warmup < 5; warmup++) {
            extractor.extract(text, middle, 40);
            extractor.preview(text, 500);
        }
        long[] extractTimes = new long[100];
        long[] previewTimes = new long[100];
        for (int round = 0; round < 100; round++) {
            long start = System.nanoTime();
            extractor.extract(text, middle, 40);
            extractTimes[round] = System.nanoTime() - start;
            start = System.nanoTime();
            extractor.preview(text, 500);
            previewTimes[round] = System.nanoTime() - start;
        }
        Arrays.sort(extractTimes);
        Arrays.sort(previewTimes);
        assertTrue(extractTimes[50] <= 1_000_000, "extract median ns: " + extractTimes[50]);
        assertTrue(previewTimes[50] <= 1_000_000, "preview median ns: " + previewTimes[50]);
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
    @DisplayName("B-46 throughput basis for 100 MB / 1 s with parallelism")
    void search_twentyFiveMegabyteDocumentsSequential_averageWithin100Milliseconds() {
        String text = TestDocuments.asciiText(5 * 1024 * 1024, 1L);
        SearchQuery query = SearchQuery.of("computer algorithm");
        List<Document> documents = new ArrayList<>();
        for (int index = 0; index < 20; index++) {
            documents.add(TestDocuments.document("Doc" + index, text));
        }
        for (int warmup = 0; warmup < 3; warmup++) {
            for (Document document : documents) {
                engine.search(document, query, CancellationToken.NONE);
            }
        }
        long[] times = new long[3];
        for (int round = 0; round < times.length; round++) {
            long start = System.nanoTime();
            for (Document document : documents) {
                engine.search(document, query, CancellationToken.NONE);
            }
            times[round] = System.nanoTime() - start;
        }
        Arrays.sort(times);
        double averageMillis = times[1] / 1_000_000.0 / documents.size();
        assertTrue(averageMillis <= 100, "Average per 5 MB document: " + averageMillis + " ms");
    }
}
