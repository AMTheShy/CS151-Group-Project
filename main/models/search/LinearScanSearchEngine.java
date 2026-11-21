package models.search;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import models.AppConstants;
import models.AsciiText;
import models.CancellationToken;
import models.Document;
import models.DocumentSearchResult;
import models.MatchRange;
import models.MatchRanges;
import models.SearchQuery;

/**
 * {@link SearchEngine} that scans each document linearly on demand (no index) - the chosen main
 * solution (design doc, "Search algorithm"; the inverted-index alternative was rejected).
 *
 * <p><b>Owner:</b> Member B (Search Engine).</p>
 *
 * <p><b>Requirements covered:</b> S1 (case-insensitive exact substring, whitespace verbatim),
 * S3 (all non-overlapping matches, ascending), S4 (snippet of the first match via the injected
 * {@link SnippetExtractor}), UC2.</p>
 *
 * <p><b>Algorithm (recommended; any algorithm is acceptable if it is exactly equivalent to
 * {@code testsupport.ReferenceMatcher} and meets the performance targets):</b></p>
 * <ol>
 *   <li>{@code q = query.foldedText()}, {@code m = q.length()}, {@code first = q.charAt(0)};
 *       pre-compute the upper-case variant of {@code first} if it is a letter.</li>
 *   <li>{@code position = 0}; while {@code position <= text.length() - m}: if
 *       {@code text.charAt(position)} equals {@code first} in either case and
 *       {@link #matchesAt} is true, add {@code position} to a {@code MatchRanges.Builder} and set
 *       {@code position += m}; otherwise {@code position++}.</li>
 *   <li>Every {@code AppConstants.CANCELLATION_CHECK_INTERVAL} positions, call
 *       {@code token.throwIfCancelled()} (check a counter; do not call it on every character).</li>
 *   <li>Return the built {@code MatchRanges} (compact; decision N6).</li>
 * </ol>
 * <p>Do <b>not</b> use {@code String.toLowerCase()} without {@code Locale.ROOT} and do not fold
 * the whole document into a new string per query unless the benchmark shows it is faster; use
 * {@code models.AsciiText.toLowerAscii(char)} so indices stay aligned (S2). Do not use regular
 * expressions (queries contain {@code . ( * +} literally, test B-16).</p>
 *
 * <p><b>Performance targets</b> (benchmark tests B-44..B-46, design doc 3.4): all matches in a
 * 5 MB document in &le; 100 ms on one thread; 100 MB in &le; 1 s with one thread per core
 * (the parallelism is provided by Member C's controller, not here).</p>
 *
 * <p><b>What it does NOT do:</b> no threads, no caching, no ranking, no regex, no word boundaries.</p>
 *
 * <p><b>Threading:</b> Immutable, thread-safe: the only field is the (stateless) snippet
 * extractor; all working state lives in local variables, so any number of threads may search
 * concurrently with one instance (test B-14).</p>
 */
public final class LinearScanSearchEngine implements SearchEngine {

    /** Builds the snippet of the first match (S4); injected for testability (DIP). */
    private final SnippetExtractor snippetExtractor;

    /**
     * Creates the engine.
     *
     * @param snippetExtractor used by {@link #search} for the first match, not {@code null}
     * @throws NullPointerException if {@code snippetExtractor} is {@code null}
     */
    public LinearScanSearchEngine(SnippetExtractor snippetExtractor) {
        this.snippetExtractor = Objects.requireNonNull(snippetExtractor, "snippetExtractor");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Edge cases: empty text or {@code query.length() > text.length()} &rarr; empty list (B-09,
     * B-11); match at index 0 and ending at {@code text.length()} must be found (B-10).
     * Tests: B-01..B-11, B-13..B-18.</p>
     */
    @Override
    public List<MatchRange> findAllMatches(String text, SearchQuery query, CancellationToken token) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        String foldedQuery = query.foldedText();
        int matchLength = foldedQuery.length();
        MatchRanges.Builder matches = MatchRanges.builder(matchLength);
        int maxStart = text.length() - matchLength;
        int position = 0;
        int positionsSinceCheck = 0;
        char first = foldedQuery.charAt(0);
        while (position <= maxStart) {
            int step = 1;
            if (AsciiText.toLowerAscii(text.charAt(position)) == first
                    && matchesAt(text, position, foldedQuery)) {
                matches.add(position);
                step = matchLength;
            }
            position += step;
            positionsSinceCheck += step;
            if (positionsSinceCheck >= AppConstants.CANCELLATION_CHECK_INTERVAL) {
                token.throwIfCancelled();
                positionsSinceCheck %= AppConstants.CANCELLATION_CHECK_INTERVAL;
            }
        }
        return matches.build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@link #findAllMatches} on {@code document.content()}; if non-empty returns
     * {@code new DocumentSearchResult(document, matches,
     * snippetExtractor.extract(content, matches.get(0), AppConstants.SNIPPET_CONTEXT))}.
     * Tests: B-08, B-12.</p>
     */
    @Override
    public Optional<DocumentSearchResult> search(Document document, SearchQuery query, CancellationToken token) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(token, "token");
        String content = document.content();
        List<MatchRange> matches = findAllMatches(content, query, token);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DocumentSearchResult(document, matches,
                snippetExtractor.extract(content, matches.get(0), AppConstants.SNIPPET_CONTEXT)));
    }

    /**
     * Tells whether the folded query matches the text at {@code position}, comparing
     * {@code AsciiText.toLowerAscii(text.charAt(position + i)) == foldedQuery.charAt(i)}.
     * Precondition: {@code position + foldedQuery.length() <= text.length()}.
     *
     * @param text        the document text
     * @param position    candidate start
     * @param foldedQuery {@code query.foldedText()}
     * @return {@code true} on a full match
     */
    private static boolean matchesAt(String text, int position, String foldedQuery) {
        for (int index = 0; index < foldedQuery.length(); index++) {
            if (AsciiText.toLowerAscii(text.charAt(position + index)) != foldedQuery.charAt(index)) {
                return false;
            }
        }
        return true;
    }
}
