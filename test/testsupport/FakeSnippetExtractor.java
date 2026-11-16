package testsupport;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import models.MatchRange;
import models.Snippet;
import models.search.SnippetExtractor;

/**
 * Minimal, valid {@link SnippetExtractor}: the snippet is just the matched text (no context, no
 * ellipsis); the preview is the first characters with line breaks replaced. Used by Member C (and
 * by {@link FakeSearchEngine}) before Member B's implementation exists.
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe.</p>
 */
public final class FakeSnippetExtractor implements SnippetExtractor {

    private final AtomicInteger extractCalls = new AtomicInteger();
    private final AtomicInteger previewCalls = new AtomicInteger();

    /** Creates the extractor. */
    public FakeSnippetExtractor() {
        // empty
    }

    /**
     * Returns how often {@link #extract} was called.
     *
     * @return the count
     */
    public int extractCalls() {
        return extractCalls.get();
    }

    /**
     * Returns how often {@link #preview} was called.
     *
     * @return the count
     */
    public int previewCalls() {
        return previewCalls.get();
    }

    @Override
    public Snippet extract(String text, MatchRange match, int context) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(match, "match");
        if (context < 0 || match.end() > text.length()) {
            throw new IllegalArgumentException("invalid context or match outside text");
        }
        extractCalls.incrementAndGet();
        String matched = replaceLineBreaks(text.substring(match.start(), match.end()));
        return new Snippet(matched, new MatchRange(0, matched.length()), false, false);
    }

    @Override
    public String preview(String text, int maxChars) {
        Objects.requireNonNull(text, "text");
        if (maxChars < 0) {
            throw new IllegalArgumentException("maxChars must be >= 0");
        }
        previewCalls.incrementAndGet();
        return replaceLineBreaks(text.substring(0, Math.min(maxChars, text.length())));
    }

    private static String replaceLineBreaks(String text) {
        return text.replace('\r', ' ').replace('\n', ' ');
    }
}
