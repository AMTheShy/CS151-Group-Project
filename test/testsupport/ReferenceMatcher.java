package testsupport;

import java.util.Objects;

import models.AsciiText;
import models.MatchRanges;

/**
 * A deliberately simple, obviously-correct implementation of the S1/S3 match semantics, used as a
 * <em>test oracle</em>: Member B compares the real search engine with it on random inputs, and
 * {@link FakeSearchEngine} uses it to produce realistic results.
 *
 * <p>Algorithm: naive O(n &middot; m) scan; at each position compare the query with the text using
 * ASCII case folding; after a match continue at {@code position + m} (non-overlapping), otherwise
 * at {@code position + 1}. Never use this in production code.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Immutable, thread-safe.</p>
 */
public final class ReferenceMatcher {

    private ReferenceMatcher() {
        throw new AssertionError("ReferenceMatcher is a utility class and must not be instantiated");
    }

    /**
     * Finds all non-overlapping, case-insensitive (ASCII) matches.
     *
     * @param text  the text, not {@code null}
     * @param query the query text, not {@code null}, not empty
     * @return the matches, ascending
     */
    public static MatchRanges findAll(String text, String query) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(query, "query");
        if (query.isEmpty()) {
            throw new IllegalArgumentException("query must not be empty");
        }
        int queryLength = query.length();
        MatchRanges.Builder builder = MatchRanges.builder(queryLength);
        int position = 0;
        while (position + queryLength <= text.length()) {
            if (matchesAt(text, query, position)) {
                builder.add(position);
                position += queryLength;
            } else {
                position++;
            }
        }
        return builder.build();
    }

    private static boolean matchesAt(String text, String query, int position) {
        for (int offset = 0; offset < query.length(); offset++) {
            char textChar = AsciiText.toLowerAscii(text.charAt(position + offset));
            char queryChar = AsciiText.toLowerAscii(query.charAt(offset));
            if (textChar != queryChar) {
                return false;
            }
        }
        return true;
    }
}
