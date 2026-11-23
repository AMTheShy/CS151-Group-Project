package views;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static views.SwingTestSupport.awaitRender;
import static views.SwingTestSupport.awaitRenderMeasuringLongestRoundTrip;
import static views.SwingTestSupport.onEdt;
import static views.SwingTestSupport.onEdtGet;
import static views.SwingTestSupport.result;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import models.DocumentSearchResult;
import models.MatchRange;
import models.MatchRanges;
import models.SearchQuery;
import testsupport.FakeDocumentLibraryController.ExportRequest;
import testsupport.FakeDocumentLibraryController;
import testsupport.TestDocuments;

/**
 * Tests for {@code DocumentReaderPanel}. Owner: Member D.
 *
 * <p>Cases D-43..D-47 of {@code design/test-plan.md}. Every test is {@code @Disabled} until
 * implemented: remove the annotation, implement Arrange-Act-Assert as described in the method
 * Javadoc, and keep the method name and {@code @DisplayName} unchanged (they are referenced by
 * the test plan and the traceability table).</p>
 */
class DocumentReaderPanelTest {

    /** Size of the D-47 document. */
    private static final int FIVE_MEGABYTES = 5 * 1024 * 1024;

    /** Matches in the D-47 document (above MAX_EAGER_HIGHLIGHTS, so windowed mode is exercised). */
    private static final int BENCHMARK_MATCHES = 20_000;

    /** The D-47 query. */
    private static final String BENCHMARK_NEEDLE = "needle";

    /** Warm-up runs before measuring (Q8). */
    private static final int BENCHMARK_WARM_UPS = 3;

    /** Measured runs (Q8). */
    private static final int BENCHMARK_RUNS = 3;

    /** Target for the whole render (3.4). */
    private static final long MAX_RENDER_MILLIS = 1_000;

    /** Target for a single EDT step (U6). */
    private static final long MAX_EDT_STEP_MILLIS = 100;

    private FakeDocumentLibraryController controller;

    private ScriptedDialogService dialogs;

    private DocumentReaderPanel reader;

    /** Creates fresh fixtures for every test (no shared state between tests). */
    @BeforeEach
    void setUp() {
        controller = new FakeDocumentLibraryController();
        dialogs = new ScriptedDialogService();
        reader = onEdtGet(() -> new DocumentReaderPanel(new DownloadWorkflow(controller, dialogs)));
    }

    /**
     * D-43: offsets stay aligned (N9) [unit].
     *
     * <p>Arrange: 'a\r\nb\rc'.</p>
     * <p>Act: toDisplayText.</p>
     * <p>Assert: 'a \nb c'; same length.</p>
     */
    @Test
    @DisplayName("D-43 offsets stay aligned (N9)")
    void toDisplayText_carriageReturns_replacedOneToOne() {
        // Act
        String display = DocumentReaderPanel.toDisplayText("a\r\nb\rc");

        // Assert
        assertEquals("a \nb c", display);
        assertEquals("a\r\nb\rc".length(), display.length());
    }

    /**
     * D-44: reader content (U5) [unit].
     *
     * <p>Arrange: result with 3 matches; flush chained EDT steps (invokeAndWait loop).</p>
     * <p>Act: showResult.</p>
     * <p>Assert: text equals display text; not editable; query label contains query and '3 matches';
     * position '1 / 3'.</p>
     */
    @Test
    @DisplayName("D-44 reader content (U5)")
    void showResult_document_rendersReadOnlyTextAndLabels() {
        // Arrange
        String content = "computer one\r\ncomputer two\ncomputer three";
        DocumentSearchResult result = result("Doc", content, "computer");

        // Act
        onEdt(() -> reader.showResult(result, SearchQuery.of("computer")));
        awaitRender(reader);

        // Assert
        assertEquals(DocumentReaderPanel.toDisplayText(content), onEdtGet(() -> reader.textArea().getText()));
        assertFalse(onEdtGet(() -> reader.textArea().isEditable()));
        String queryText = onEdtGet(() -> reader.queryLabel().getText());
        assertTrue(queryText.contains("computer") && queryText.contains("3 matches"), queryText);
        assertEquals("1 / 3", onEdtGet(() -> reader.positionLabel().getText()));
    }

    /**
     * D-45: cyclic Previous/Next (U5) [unit].
     *
     * <p>Arrange: 3 matches; next x3; previous.</p>
     * <p>Act: showNextMatch / showPreviousMatch.</p>
     * <p>Assert: caret at starts of match 1, 2, 0, then 2; position label follows.</p>
     */
    @Test
    @DisplayName("D-45 cyclic Previous/Next (U5)")
    void navigation_nextAndPrevious_moveCaretCyclically() {
        // Arrange
        DocumentSearchResult result = result("Doc", "xx needle yy needle zz needle", "needle");
        onEdt(() -> reader.showResult(result, SearchQuery.of("needle")));
        awaitRender(reader);
        List<Integer> starts = result.matches().stream().map(MatchRange::start).toList();

        // Act
        List<Integer> carets = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        onEdt(() -> {
            for (int step = 0; step < 3; step++) {
                reader.showNextMatch();
                carets.add(reader.textArea().getCaretPosition());
                labels.add(reader.positionLabel().getText());
            }
            reader.showPreviousMatch();
            carets.add(reader.textArea().getCaretPosition());
            labels.add(reader.positionLabel().getText());
        });

        // Assert
        assertEquals(List.of(starts.get(1), starts.get(2), starts.get(0), starts.get(2)), carets);
        assertEquals(List.of("2 / 3", "3 / 3", "1 / 3", "3 / 3"), labels);
    }

    /**
     * D-46: Download button (D8) [unit].
     *
     * <p>Arrange: dialogs script target; click download.</p>
     * <p>Act: onDownloadClicked.</p>
     * <p>Assert: controller.exportRequests has the displayed document.</p>
     */
    @Test
    @DisplayName("D-46 Download button (D8)")
    void onDownloadClicked_targetChosen_exportsDisplayedSnapshot() {
        // Arrange
        DocumentSearchResult result = result("Notes", "some needle text", "needle");
        Path target = Path.of("Notes.txt").toAbsolutePath();
        dialogs.answerDownloadTarget(Optional.of(target));
        onEdt(() -> reader.showResult(result, SearchQuery.of("needle")));
        awaitRender(reader);

        // Act
        onEdt(reader::onDownloadClicked);

        // Assert
        assertEquals(List.of(new ExportRequest(result.document(), target)), controller.exportRequests());
    }

    /**
     * D-47: render 5 MB &lt;= 1 s, no EDT step &gt; 100 ms (3.4, U6) [benchmark].
     *
     * <p>Arrange: 5 MB document with 20,000 matches; time each EDT step.</p>
     * <p>Act: showResult.</p>
     * <p>Assert: total &lt;= 1000 ms; max single step &lt;= 100 ms.</p>
     */
    @Test
    @Tag("benchmark")
    @DisplayName("D-47 render 5 MB <= 1 s, no EDT step > 100 ms (3.4, U6)")
    void showResult_fiveMegabyteDocument_rendersWithinOneSecondWithShortEdtSteps() {
        // Arrange
        DocumentSearchResult result = fiveMegabyteResult();
        SearchQuery query = SearchQuery.of(BENCHMARK_NEEDLE);
        List<Long> totalNanos = new ArrayList<>();
        List<Long> longestStepNanos = new ArrayList<>();
        for (int run = 0; run < BENCHMARK_WARM_UPS + BENCHMARK_RUNS; run++) {
            onEdt(reader::clear);

            // Act
            long start = System.nanoTime();
            onEdt(() -> reader.showResult(result, query));
            long longestRoundTrip = awaitRenderMeasuringLongestRoundTrip(reader);
            long elapsed = System.nanoTime() - start;

            if (run >= BENCHMARK_WARM_UPS) {
                totalNanos.add(elapsed);
                longestStepNanos.add(longestRoundTrip);
            }
        }

        // Assert
        assertEquals(result.document().length(), onEdtGet(() -> reader.textArea().getDocument().getLength()));
        long medianTotalMillis = TimeUnit.NANOSECONDS.toMillis(median(totalNanos));
        long medianLongestStepMillis = TimeUnit.NANOSECONDS.toMillis(median(longestStepNanos));
        assertTrue(medianTotalMillis <= MAX_RENDER_MILLIS, "render took " + medianTotalMillis + " ms");
        assertTrue(medianLongestStepMillis <= MAX_EDT_STEP_MILLIS,
                "longest EDT step " + medianLongestStepMillis + " ms");
    }

    /**
     * Builds the D-47 document: 5 MB of generated ASCII text with {@link #BENCHMARK_MATCHES} evenly
     * spaced occurrences of {@link #BENCHMARK_NEEDLE}.
     *
     * @return the search result
     */
    private static DocumentSearchResult fiveMegabyteResult() {
        StringBuilder content = new StringBuilder(TestDocuments.asciiText(FIVE_MEGABYTES, 1));
        int spacing = FIVE_MEGABYTES / BENCHMARK_MATCHES;
        MatchRanges.Builder matches = MatchRanges.builder(BENCHMARK_NEEDLE.length());
        for (int index = 0; index < BENCHMARK_MATCHES; index++) {
            int start = index * spacing;
            content.replace(start, start + BENCHMARK_NEEDLE.length(), BENCHMARK_NEEDLE);
            matches.add(start);
        }
        return result("Big", content.toString(), matches.build());
    }

    /**
     * Returns the median of the values.
     *
     * @param values at least one value
     * @return the median (upper median for even counts)
     */
    private static long median(List<Long> values) {
        List<Long> sorted = values.stream().sorted().toList();
        return sorted.get(sorted.size() / 2);
    }
}
