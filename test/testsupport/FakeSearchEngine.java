package testsupport;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import models.AppConstants;
import models.CancellationToken;
import models.Document;
import models.DocumentSearchResult;
import models.MatchRange;
import models.MatchRanges;
import models.SearchQuery;
import models.search.SearchEngine;

/**
 * Scriptable {@link SearchEngine} for Member C's controller tests.
 *
 * <p>By default it behaves realistically: matches come from {@link ReferenceMatcher}, snippets from
 * {@link FakeSnippetExtractor}. Per title, a test can instead preset a result
 * ({@link #presetResult}) or a failure ({@link #failOn}). A hook ({@link #setBeforeSearchHook})
 * runs at the start of every {@link #search} call - use it with a {@code CountDownLatch} to hold
 * workers, or to cancel a handle mid-search.</p>
 *
 * <p>Owner: Eric (contract layer test support, frozen v1.0).</p>
 *
 * <p>Threading: Thread-safe; has no shared mutable state apart from the recording lists.</p>
 */
public final class FakeSearchEngine implements SearchEngine {

    private final FakeSnippetExtractor snippetExtractor = new FakeSnippetExtractor();
    private final Map<String, Optional<DocumentSearchResult>> presetResults = new ConcurrentHashMap<>();
    private final Map<String, RuntimeException> failures = new ConcurrentHashMap<>();
    private final List<String> searchedTitles = new CopyOnWriteArrayList<>();
    private final List<CancellationToken> tokensSeen = new CopyOnWriteArrayList<>();
    private volatile Consumer<Document> beforeSearchHook = document -> { };

    /** Creates an engine with realistic default behavior. */
    public FakeSearchEngine() {
        // empty
    }

    /**
     * Presets the result for a title, bypassing real matching.
     *
     * @param title  the document title
     * @param result the result to return (empty = no match)
     */
    public void presetResult(String title, Optional<DocumentSearchResult> result) {
        presetResults.put(title, result);
    }

    /**
     * Makes searching the given title throw.
     *
     * @param title   the document title
     * @param failure the exception to throw
     */
    public void failOn(String title, RuntimeException failure) {
        failures.put(title, failure);
    }

    /**
     * Sets a hook run at the start of each {@link #search}.
     *
     * @param hook the hook, not {@code null}
     */
    public void setBeforeSearchHook(Consumer<Document> hook) {
        beforeSearchHook = Objects.requireNonNull(hook, "hook");
    }

    /**
     * Returns the titles passed to {@link #search}, in call order (order is nondeterministic when
     * called from several threads).
     *
     * @return immutable copy
     */
    public List<String> searchedTitles() {
        return List.copyOf(searchedTitles);
    }

    /**
     * Returns the tokens received by {@link #search}.
     *
     * @return immutable copy
     */
    public List<CancellationToken> tokensSeen() {
        return List.copyOf(tokensSeen);
    }

    @Override
    public List<MatchRange> findAllMatches(String text, SearchQuery query, CancellationToken token) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        return ReferenceMatcher.findAll(text, query.text());
    }

    @Override
    public Optional<DocumentSearchResult> search(Document document, SearchQuery query, CancellationToken token) {
        Objects.requireNonNull(document, "document");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(token, "token");
        searchedTitles.add(document.title());
        tokensSeen.add(token);
        beforeSearchHook.accept(document);
        token.throwIfCancelled();
        RuntimeException failure = failures.get(document.title());
        if (failure != null) {
            throw failure;
        }
        Optional<DocumentSearchResult> preset = presetResults.get(document.title());
        if (preset != null) {
            return preset;
        }
        MatchRanges matches = ReferenceMatcher.findAll(document.content(), query.text());
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new DocumentSearchResult(document, matches,
                snippetExtractor.extract(document.content(), matches.get(0), AppConstants.SNIPPET_CONTEXT)));
    }
}
