package models.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import models.CancellationToken;
import models.Document;
import models.DocumentSearchResult;
import models.MatchRange;
import models.MatchRanges;
import models.OperationCancelledException;
import models.SearchQuery;
import models.SimpleCancellationToken;
import testsupport.FakeSnippetExtractor;
import testsupport.ReferenceMatcher;
import testsupport.TestDocuments;

/**
 * Tests for {@code LinearScanSearchEngine}. Owner: Member B.
 *
 * <p>Cases B-01..B-18 of {@code design/test-plan.md}.
 * Tests retain fixed method names and display IDs from the test plan.
 * Each test covers the corresponding scenario in its Javadoc.</p>
 */
class LinearScanSearchEngineTest {

    private LinearScanSearchEngine engine;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        engine = new LinearScanSearchEngine(new FakeSnippetExtractor());
    }

    /**
     * B-01: case-insensitive exact match (S1) [unit].
     *
     * <p>Arrange: text 'CoMpUtEr aLgOriThm'; query 'computer algorithm'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: one match [0,18).</p>
     */
    @Test
    @DisplayName("B-01 case-insensitive exact match (S1)")
    void findAllMatches_differentLetterCase_matches() {
        assertEquals(MatchRanges.of(18, 0), engine.findAllMatches("CoMpUtEr aLgOriThm",
                SearchQuery.of("computer algorithm"), CancellationToken.NONE));
    }

    /**
     * B-02: prefix/suffix examples match (S1) [unit].
     *
     * <p>Arrange: each of TestDocuments.S1_POSITIVE_TEXTS; query S1_QUERY.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: exactly one match each.</p>
     */
    @Test
    @DisplayName("B-02 prefix/suffix examples match (S1)")
    void findAllMatches_s1PositiveExamples_eachMatchesOnce() {
        SearchQuery query = SearchQuery.of(TestDocuments.S1_QUERY);
        for (String text : TestDocuments.S1_POSITIVE_TEXTS) {
            assertEquals(1, engine.findAllMatches(text, query, CancellationToken.NONE).size(), text);
        }
    }

    /**
     * B-03: term-wise prefix/suffix do not match (S1) [unit].
     *
     * <p>Arrange: 'Computing algorithms', 'computer randomized algorithms'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: no matches.</p>
     */
    @Test
    @DisplayName("B-03 term-wise prefix/suffix do not match (S1)")
    void findAllMatches_termWisePartialMatches_doNotMatch() {
        SearchQuery query = SearchQuery.of(TestDocuments.S1_QUERY);
        for (String text : List.of("Computing algorithms", "computer randomized algorithms")) {
            assertTrue(engine.findAllMatches(text, query, CancellationToken.NONE).isEmpty(), text);
        }
    }

    /**
     * B-04: whitespace must match exactly (S1) [unit].
     *
     * <p>Arrange: 'Computer_Algorithm', double space, tab, LF, CRLF variants.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: no matches.</p>
     */
    @Test
    @DisplayName("B-04 whitespace must match exactly (S1)")
    void findAllMatches_differentWhitespace_doesNotMatch() {
        SearchQuery query = SearchQuery.of(TestDocuments.S1_QUERY);
        for (String text : TestDocuments.S1_NEGATIVE_TEXTS) {
            assertTrue(engine.findAllMatches(text, query, CancellationToken.NONE).isEmpty(), text);
        }
    }

    /**
     * B-05: non-overlapping scan (S3) [unit].
     *
     * <p>Arrange: text 'aaaa'; query 'aa'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: [0,2) and [2,4).</p>
     */
    @Test
    @DisplayName("B-05 non-overlapping scan (S3)")
    void findAllMatches_overlappingCandidates_returnsNonOverlapping() {
        assertEquals(MatchRanges.of(2, 0, 2),
                engine.findAllMatches("aaaa", SearchQuery.of("aa"), CancellationToken.NONE));
    }

    /**
     * B-06: non-overlapping scan, odd length (S3) [unit].
     *
     * <p>Arrange: text 'aaa'; query 'aa'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: only [0,2).</p>
     */
    @Test
    @DisplayName("B-06 non-overlapping scan, odd length (S3)")
    void findAllMatches_oddOverlap_returnsFirstOnly() {
        assertEquals(MatchRanges.of(2, 0),
                engine.findAllMatches("aaa", SearchQuery.of("aa"), CancellationToken.NONE));
    }

    /**
     * B-07: ascending, index-aligned ranges (S2, S3) [unit].
     *
     * <p>Arrange: resource s1-match-cases.txt; S1_QUERY.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: 5 ranges ascending; each text.substring(range) equalsIgnoreCase the query.</p>
     */
    @Test
    @DisplayName("B-07 ascending, index-aligned ranges (S2, S3)")
    void findAllMatches_multipleMatches_ascendingAndAligned() throws Exception {
        String text = TestDocuments.readResource(TestDocuments.RESOURCE_S1_CASES);
        SearchQuery query = SearchQuery.of(TestDocuments.S1_QUERY);
        List<MatchRange> matches = engine.findAllMatches(text, query, CancellationToken.NONE);
        assertEquals(TestDocuments.S1_CASES_EXPECTED_MATCHES, matches.size());
        int previousEnd = 0;
        for (MatchRange range : matches) {
            assertTrue(range.start() >= previousEnd);
            assertTrue(text.substring(range.start(), range.end()).equalsIgnoreCase(query.text()));
            previousEnd = range.end();
        }
    }

    /**
     * B-08: document without match [unit].
     *
     * <p>Arrange: document 'nothing here'; query 'zzz'.</p>
     * <p>Act: findAllMatches / search.</p>
     * <p>Assert: findAllMatches empty; search returns Optional.empty().</p>
     */
    @Test
    @DisplayName("B-08 document without match")
    void search_noMatch_returnsEmpty() {
        Document document = TestDocuments.document("None", "nothing here");
        SearchQuery query = SearchQuery.of("zzz");
        assertEquals(List.of(), engine.findAllMatches(document.content(), query, CancellationToken.NONE));
        assertTrue(engine.search(document, query, CancellationToken.NONE).isEmpty());
    }

    /**
     * B-09: query longer than text [unit].
     *
     * <p>Arrange: text 'abc'; query 'abcd'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: empty list, no exception.</p>
     */
    @Test
    @DisplayName("B-09 query longer than text")
    void findAllMatches_queryLongerThanText_returnsEmpty() {
        assertEquals(List.of(), engine.findAllMatches("abc", SearchQuery.of("abcd"), CancellationToken.NONE));
    }

    /**
     * B-10: matches at index 0 and at the very end [unit].
     *
     * <p>Arrange: text 'ab xx ab'; query 'ab'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: [0,2) and [6,8).</p>
     */
    @Test
    @DisplayName("B-10 matches at index 0 and at the very end")
    void findAllMatches_matchesAtTextBoundaries_found() {
        assertEquals(MatchRanges.of(2, 0, 6),
                engine.findAllMatches("ab xx ab", SearchQuery.of("ab"), CancellationToken.NONE));
    }

    /**
     * B-11: empty document (D3 allows empty files) [unit].
     *
     * <p>Arrange: text ''; query 'a'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: empty list.</p>
     */
    @Test
    @DisplayName("B-11 empty document (D3 allows empty files)")
    void findAllMatches_emptyText_returnsEmpty() {
        assertEquals(List.of(), engine.findAllMatches("", SearchQuery.of("a"), CancellationToken.NONE));
    }

    /**
     * B-12: result object (S4) [unit].
     *
     * <p>Arrange: document with 3 matches; FakeSnippetExtractor.</p>
     * <p>Act: search.</p>
     * <p>Assert: result.matchCount() 3; snippet text = first match text; result.document() same
     * instance.</p>
     */
    @Test
    @DisplayName("B-12 result object (S4)")
    void search_matchingDocument_returnsResultWithSnippetAndCount() {
        FakeSnippetExtractor extractor = new FakeSnippetExtractor();
        LinearScanSearchEngine localEngine = new LinearScanSearchEngine(extractor);
        Document document = TestDocuments.document("Hits", "aba aba aba");
        DocumentSearchResult result = localEngine.search(document, SearchQuery.of("aba"),
                CancellationToken.NONE).orElseThrow();
        assertEquals(3, result.matchCount());
        assertEquals("aba", result.snippet().text());
        assertSame(document, result.document());
        assertEquals(1, extractor.extractCalls());
    }

    /**
     * B-13: cancellation checks [unit].
     *
     * <p>Arrange: (a) pre-cancelled token; (b) 5 MB text with a token that turns cancelled after 3
     * isCancelled() calls.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: OperationCancelledException in both cases.</p>
     */
    @Test
    @DisplayName("B-13 cancellation checks")
    void findAllMatches_cancelledToken_throwsOperationCancelledException() {
        SimpleCancellationToken cancelled = new SimpleCancellationToken();
        cancelled.cancel();
        assertThrows(OperationCancelledException.class,
                () -> engine.findAllMatches("abc", SearchQuery.of("a"), cancelled));
        CancellationToken afterThreeChecks = new CancellationToken() {
            private int checkCount;
            @Override
            public boolean isCancelled() {
                checkCount++;
                return checkCount >= 3;
            }
        };
        String text = "x".repeat(5 * 1024 * 1024);
        assertThrows(OperationCancelledException.class,
                () -> engine.findAllMatches(text, SearchQuery.of("q"), afterThreeChecks));
    }

    /**
     * B-14: stateless and re-entrant (4.4) [concurrency].
     *
     * <p>Arrange: 8 threads x 50 searches on shared engine and document, start latch, timeout.</p>
     * <p>Act: search.</p>
     * <p>Assert: all results equal the single-threaded result.</p>
     */
    @Test
    @Tag("concurrency")
    @DisplayName("B-14 stateless and re-entrant (4.4)")
    void search_concurrentCallsOnSameInstance_returnIdenticalResults() throws Exception {
        Document document = TestDocuments.document("Shared", "computer algorithm ".repeat(100));
        SearchQuery query = SearchQuery.of("computer algorithm");
        DocumentSearchResult expected = engine.search(document, query, CancellationToken.NONE).orElseThrow();
        int workerCount = 8;
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int worker = 0; worker < workerCount; worker++) {
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    for (int round = 0; round < 50; round++) {
                        assertEquals(expected,
                                engine.search(document, query, CancellationToken.NONE).orElseThrow());
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    /**
     * B-15: oracle comparison [unit].
     *
     * <p>Arrange: 1000 seeded random texts over alphabet {a,A,b,' ','\n'} and random queries.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: result equals ReferenceMatcher.findAll for every case.</p>
     */
    @Test
    @DisplayName("B-15 oracle comparison")
    void findAllMatches_randomInputs_agreesWithReferenceMatcher() {
        Random random = new Random(2026L);
        char[] alphabet = {'a', 'A', 'b', ' ', '\n'};
        for (int testCase = 0; testCase < 1_000; testCase++) {
            int textLength = random.nextInt(100);
            StringBuilder text = new StringBuilder(textLength);
            for (int index = 0; index < textLength; index++) {
                text.append(alphabet[random.nextInt(alphabet.length)]);
            }
            int queryLength = 1 + random.nextInt(8);
            StringBuilder queryText = new StringBuilder(queryLength);
            for (int index = 0; index < queryLength; index++) {
                queryText.append(alphabet[random.nextInt(alphabet.length)]);
            }
            String normalized = queryText.toString().trim();
            if (normalized.isEmpty()) {
                normalized = "a";
            }
            SearchQuery query = SearchQuery.of(normalized);
            assertEquals(ReferenceMatcher.findAll(text.toString(), query.text()),
                    engine.findAllMatches(text.toString(), query, CancellationToken.NONE),
                    "random case " + testCase);
        }
    }

    /**
     * B-16: no regex semantics [unit].
     *
     * <p>Arrange: text 'a.b axb (x) [x]'; queries 'a.b', '(x)', '[x]'.</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: exactly the literal occurrences.</p>
     */
    @Test
    @DisplayName("B-16 no regex semantics")
    void findAllMatches_regexMetacharacters_treatedLiterally() {
        String text = "a.b axb (x) [x]";
        assertEquals(MatchRanges.of(3, 0),
                engine.findAllMatches(text, SearchQuery.of("a.b"), CancellationToken.NONE));
        assertEquals(MatchRanges.of(3, 8),
                engine.findAllMatches(text, SearchQuery.of("(x)"), CancellationToken.NONE));
        assertEquals(MatchRanges.of(3, 12),
                engine.findAllMatches(text, SearchQuery.of("[x]"), CancellationToken.NONE));
    }

    /**
     * B-17: null checks [unit].
     *
     * <p>Arrange: null text/query/token/document.</p>
     * <p>Act: all.</p>
     * <p>Assert: NullPointerException.</p>
     */
    @Test
    @DisplayName("B-17 null checks")
    void publicMethods_nullArguments_throwNullPointerException() {
        SearchQuery query = SearchQuery.of("a");
        Document document = TestDocuments.document("Doc", "abc");
        assertThrows(NullPointerException.class,
                () -> engine.findAllMatches(null, query, CancellationToken.NONE));
        assertThrows(NullPointerException.class,
                () -> engine.findAllMatches("abc", null, CancellationToken.NONE));
        assertThrows(NullPointerException.class, () -> engine.findAllMatches("abc", query, null));
        assertThrows(NullPointerException.class,
                () -> engine.search(null, query, CancellationToken.NONE));
        assertThrows(NullPointerException.class,
                () -> engine.search(document, null, CancellationToken.NONE));
        assertThrows(NullPointerException.class, () -> engine.search(document, query, null));
    }

    /**
     * B-18: locale independence (S2) [unit].
     *
     * <p>Arrange: Locale.setDefault(tr-TR); text 'TITLE'; query 'title' (restore locale in finally).</p>
     * <p>Act: findAllMatches.</p>
     * <p>Assert: one match.</p>
     */
    @Test
    @DisplayName("B-18 locale independence (S2)")
    void findAllMatches_turkishDefaultLocale_stillFoldsAsciiOnly() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(MatchRanges.of(5, 0),
                    engine.findAllMatches("TITLE", SearchQuery.of("title"), CancellationToken.NONE));
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
