package models.search;

import java.util.List;
import java.util.Optional;

import models.CancellationToken;
import models.Document;
import models.DocumentSearchResult;
import models.MatchRange;
import models.OperationCancelledException;
import models.SearchQuery;

/**
 * Finds query matches in text (UC2, S1, S3) - a Strategy: the controller depends only on this
 * interface, so the algorithm can be replaced (e.g. by an indexed engine) without touching any
 * other module (OCP).
 *
 * <p><b>Match semantics (S1):</b> a match is a position {@code p} such that for every
 * {@code i < query.length()}, {@code toLowerAscii(text.charAt(p + i)) == query.foldedText().charAt(i)}.
 * That is: case-insensitive with ASCII folding only; every other character, including every
 * whitespace character, must be identical (one space matches exactly one space; it does not match
 * a tab, a newline, two spaces or an underscore). No word-boundary rules: "Computer Algorithms"
 * and "The computer algorithm" contain the query "computer algorithm".</p>
 *
 * <p><b>Non-overlapping, left to right (S3):</b> scanning starts at 0; after a match at
 * {@code p}, scanning continues at {@code p + query.length()}; otherwise at {@code p + 1}.
 * Example: "aa" in "aaaa" yields [0,2) and [2,4); "aa" in "aaa" yields only [0,2).</p>
 *
 * <p>Owner: Eric (interface, frozen v1.0). Implemented by Member B
 * ({@code models.search.LinearScanSearchEngine}). Used by Member C's search controller.</p>
 *
 * <p>Threading: Implementations must not have shared mutable state; any number of threads may
 * call any method concurrently on the same instance.</p>
 */
public interface SearchEngine {

    /**
     * Finds all non-overlapping matches.
     *
     * @param text  the text to scan, not {@code null}
     * @param query the query, not {@code null}
     * @param token checked at least every {@code AppConstants.CANCELLATION_CHECK_INTERVAL}
     *              characters, not {@code null}
     * @return an immutable list of ranges ascending by start, each of length
     *         {@code query.length()}; empty if there is no match. Implementations should return a
     *         {@code models.MatchRanges}.
     * @throws NullPointerException        if an argument is {@code null}
     * @throws OperationCancelledException if the token was cancelled
     */
    List<MatchRange> findAllMatches(String text, SearchQuery query, CancellationToken token);

    /**
     * Searches one document and builds its result, including the snippet of the first match (S4).
     *
     * @param document the document snapshot, not {@code null}
     * @param query    the query, not {@code null}
     * @param token    the cancellation token, not {@code null}
     * @return the result if the document matches at least once, otherwise empty
     * @throws NullPointerException        if an argument is {@code null}
     * @throws OperationCancelledException if the token was cancelled
     */
    Optional<DocumentSearchResult> search(Document document, SearchQuery query, CancellationToken token);
}
