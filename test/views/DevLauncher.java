package views;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import controllers.SearchController;
import controllers.SearchHandle;
import controllers.SearchListener;
import models.AppConstants;
import models.Document;
import models.DocumentSearchResult;
import models.DocumentSummary;
import models.ImportProgress;
import models.LoadSummary;
import models.MatchRanges;
import models.SearchQuery;
import testsupport.FakeDocumentLibraryController;
import testsupport.FakeLifecycleController;
import testsupport.FakeSnippetExtractor;
import testsupport.ReferenceMatcher;
import testsupport.TestDocuments;

/**
 * Developer launcher (not a test): opens the real {@link MainFrame} on fakes so the whole UI can be
 * seen and clicked before Member C's controllers exist. Not part of {@code /main}.
 *
 * <p><b>Owner:</b> Member D.</p>
 *
 * <p>What it shows: a startup load, three documents in the table, import progress rows (one running,
 * one failed), and a working search over the fake documents (matches from {@link ReferenceMatcher},
 * delivered after a short delay so the "Searching..." state is visible). The "Big" document has more
 * than {@code MAX_EAGER_HIGHLIGHTS} matches of "computer algorithm" and exercises windowed
 * highlighting. Pass {@code --demo} to open a search tab and select the first result automatically.</p>
 *
 * <p><b>Threading:</b> {@link #main} hops to the EDT; everything else runs on the EDT.</p>
 */
final class DevLauncher {

    /** Delay before the fake search delivers its results. */
    private static final int SEARCH_DELAY_MILLIS = 600;

    /** Length of the generated big document. */
    private static final int BIG_DOCUMENT_LENGTH = 2 * 1024 * 1024;

    /** Every n-th character of the big document starts a match. */
    private static final int BIG_DOCUMENT_MATCH_SPACING = 150;

    /** The query used by {@code --demo}. */
    private static final String DEMO_QUERY = "computer algorithm";

    private DevLauncher() {
        throw new AssertionError("DevLauncher is a launcher and must not be instantiated");
    }

    /**
     * Opens the window.
     *
     * @param args {@code --demo} to open a search tab automatically
     */
    public static void main(String[] args) {
        boolean demo = List.of(args).contains("--demo");
        SwingUtilities.invokeLater(() -> launch(demo));
    }

    /**
     * Builds the frame with fakes and plays the startup script.
     *
     * @param demo whether to open a search tab automatically
     */
    private static void launch(boolean demo) {
        FakeDocumentLibraryController library = new FakeDocumentLibraryController();
        SwingDialogService dialogs = new SwingDialogService();
        MainFrame frame = new MainFrame(library, new FakeSearch(library), new FakeLifecycleController(), dialogs);
        frame.setVisible(true);

        List<Document> documents = List.of(
                TestDocuments.document("Algorithms notes",
                        "Intro to Computer Algorithms.\r\nA computer algorithm is a recipe.\n"
                                + "Computer  Algorithm with two spaces does not match."),
                bigDocument(),
                TestDocuments.document("Shopping list", "milk\neggs\nbread"));
        library.fireStartupLoadProgress(1, documents.size());
        library.fireDocumentsChanged(summaries(documents));
        library.fireStartupLoadCompleted(new LoadSummary(documents.size(), 0, List.of()));
        library.fireImportProgress(ImportProgress.reading(1, "report.txt", 45, 100));
        library.fireImportProgress(ImportProgress.failed(2, "photo.jpg", 18, 400,
                "\"photo.jpg\" contains a non-ASCII byte at offset 18."));

        if (demo) {
            SearchTabPanel tab = frame.searchResultsPane().openSearch(SearchQuery.of(DEMO_QUERY));
            Timer selectTimer = new Timer(SEARCH_DELAY_MILLIS * 2, event -> tab.resultComboBox().setSelectedIndex(0));
            selectTimer.setRepeats(false);
            selectTimer.start();
        }
    }

    /**
     * Builds a large generated document with many matches of the demo query.
     *
     * @return the document
     */
    private static Document bigDocument() {
        StringBuilder content = new StringBuilder(TestDocuments.asciiText(BIG_DOCUMENT_LENGTH, 7));
        for (int start = 0; start + DEMO_QUERY.length() < content.length(); start += BIG_DOCUMENT_MATCH_SPACING) {
            content.replace(start, start + DEMO_QUERY.length(), "Computer Algorithm");
        }
        return TestDocuments.document("Big generated document", content.toString());
    }

    /**
     * Builds table rows.
     *
     * @param documents documents
     * @return rows sorted by title
     */
    private static List<DocumentSummary> summaries(List<Document> documents) {
        FakeSnippetExtractor previews = new FakeSnippetExtractor();
        return documents.stream()
                .sorted(AppConstants.DOCUMENT_TITLE_ORDER)
                .map(document -> new DocumentSummary(document,
                        previews.preview(document.content(), AppConstants.PREVIEW_LENGTH)))
                .toList();
    }

    /**
     * A search controller over the fake library's documents, delivering on the EDT after a delay.
     *
     * @param library the documents to search
     */
    private record FakeSearch(FakeDocumentLibraryController library) implements SearchController {

        @Override
        public SearchHandle startSearch(SearchQuery query, SearchListener listener) {
            Objects.requireNonNull(query, "query");
            Objects.requireNonNull(listener, "listener");
            boolean[] cancelled = {false};
            boolean[] done = {false};
            Timer deliveryTimer = new Timer(SEARCH_DELAY_MILLIS, event -> {
                if (cancelled[0]) {
                    return;
                }
                done[0] = true;
                listener.onSearchCompleted(search(query));
            });
            deliveryTimer.setRepeats(false);
            deliveryTimer.start();
            return new SearchHandle() {
                @Override
                public SearchQuery query() {
                    return query;
                }

                @Override
                public void cancel() {
                    if (!done[0]) {
                        cancelled[0] = true;
                        done[0] = true;
                        deliveryTimer.stop();
                    }
                }

                @Override
                public boolean isCancelled() {
                    return cancelled[0];
                }

                @Override
                public boolean isDone() {
                    return done[0];
                }
            };
        }

        /**
         * Runs the reference matcher over every document.
         *
         * @param query the query
         * @return results sorted by title, only matching documents
         */
        private List<DocumentSearchResult> search(SearchQuery query) {
            FakeSnippetExtractor snippets = new FakeSnippetExtractor();
            List<DocumentSearchResult> results = new ArrayList<>();
            for (DocumentSummary summary : library.currentDocuments()) {
                String content = summary.document().content();
                MatchRanges matches = ReferenceMatcher.findAll(content, query.text());
                if (!matches.isEmpty()) {
                    results.add(new DocumentSearchResult(summary.document(), matches,
                            snippets.extract(content, matches.get(0), AppConstants.SNIPPET_CONTEXT)));
                }
            }
            return List.copyOf(results);
        }
    }
}
