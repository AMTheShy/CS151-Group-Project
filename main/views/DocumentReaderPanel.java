package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.PlainDocument;
import javax.swing.text.View;

import models.DocumentSearchResult;
import models.MatchRange;
import models.SearchQuery;
import views.support.HighlightBatchPlanner;
import views.support.MatchNavigator;
import views.support.TextChunkPlanner;

/**
 * Lower half of a search tab: renders the selected document with all matches highlighted and
 * lets the user step through them (U5 = original UC4, D8 download, U6 never freeze).
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Layout:</b> NORTH = info bar: {@link #queryLabel} {@code Query: "<text>" - <n> matches},
 * {@link #previousButton} "Previous", {@link #positionLabel} ("k / n"), {@link #nextButton}
 * "Next", {@link #downloadButton} "Download"; CENTER = {@code JScrollPane(textArea)}.
 * {@code textArea}: not editable, monospaced font, line wrap off, caret visible
 * ({@code getCaret().setSelectionVisible(true)}).</p>
 *
 * <p><b>Rendering a result - {@link #showResult} (required algorithm):</b></p>
 * <ol>
 *   <li>Increment {@link #renderGeneration}; every chained step below captures the generation and
 *       stops if it changed (the user selected another document meanwhile).</li>
 *   <li>Reset: empty {@code PlainDocument}, remove highlights, disable buttons, label
 *       {@code "Loading..."}.</li>
 *   <li>{@code display = toDisplayText(result.document().content())} (CR &rarr; space, N9).</li>
 *   <li>Insert {@code display} into a <em>new, detached</em> {@code PlainDocument} chunk by chunk
 *       ({@link TextChunkPlanner}), one chunk per {@code SwingUtilities.invokeLater} step; then
 *       {@code textArea.setDocument(doc)} once. (A detached document has no listeners, so each
 *       insert is cheap; attaching once avoids re-layout per chunk.)</li>
 *   <li>Highlights: if {@code batchPlanner.shouldHighlightEagerly(n)}, add all highlights batch by
 *       batch ({@link HighlightBatchPlanner#planBatches}), one batch per {@code invokeLater};
 *       otherwise windowed mode: highlight
 *       {@code matchesIntersecting(matches, visibleStart - margin, visibleEnd + margin)} and redo it
 *       on viewport changes (debounced with a 100 ms {@code javax.swing.Timer}).
 *       Match color: light yellow {@code #FFF59D}; current match: orange {@code #FFB74D}, painted
 *       as a separate highlight on top.</li>
 *   <li>Navigator: {@code new MatchNavigator(n)}; show the first match: move the caret to its
 *       start, {@code textArea.scrollRectToVisible(modelToView2D(start))}, set the current
 *       highlight, update {@link #positionLabel}; enable Previous/Next iff {@code n > 0}.</li>
 * </ol>
 * <p>Every single EDT step must stay well below 100 ms for a 5 MB document (3.4; benchmark D-47).</p>
 *
 * <p><b>Download (D8):</b> {@link #downloadButton} runs {@code downloadWorkflow.run(current.document())}
 * - the snapshot being displayed.</p>
 *
 * <p><b>Threading:</b> EDT only. No I/O and no matching here: matches arrive precomputed in
 * {@link DocumentSearchResult}.</p>
 */
public final class DocumentReaderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Hint shown while no document is selected. */
    static final String HINT_TEXT = "Select a document above";

    /** Position label while no document with matches is shown (same text as {@code MatchNavigator}). */
    private static final String NO_MATCHES_LABEL = "0 / 0";

    /** Background of every match (light yellow). */
    private static final Color MATCH_COLOR = new Color(0xFFF59D);

    /** Background of the current match (orange). */
    private static final Color CURRENT_MATCH_COLOR = new Color(0xFFB74D);

    /** Characters highlighted before and after the visible range in windowed mode (N8). */
    private static final int WINDOW_MARGIN_CHARS = 10_000;

    /** Debounce delay of re-highlighting after scrolling in windowed mode, in milliseconds. */
    private static final int VIEWPORT_DEBOUNCE_MILLIS = 100;

    /** Gap of the info bar in pixels. */
    private static final int INFO_GAP = 6;

    /** Starts downloads of the displayed snapshot. */
    private final transient DownloadWorkflow downloadWorkflow;

    /** Plans highlight batches / windows. */
    private final transient HighlightBatchPlanner batchPlanner = new HighlightBatchPlanner();

    /** Plans text insertion chunks. */
    private final transient TextChunkPlanner chunkPlanner = new TextChunkPlanner();

    /** Read-only document view. */
    private final JTextArea textArea = new JTextArea();

    /** Shows query and match count. */
    private final JLabel queryLabel = new JLabel(" ");

    /** Shows "k / n". */
    private final JLabel positionLabel = new JLabel(NO_MATCHES_LABEL);

    /** Previous match (cyclic). */
    private final JButton previousButton = new JButton("Previous");

    /** Next match (cyclic). */
    private final JButton nextButton = new JButton("Next");

    /** Download the displayed snapshot. */
    private final JButton downloadButton = new JButton("Download");

    /** Painter of all matches. */
    private final transient Highlighter.HighlightPainter matchPainter =
            new DefaultHighlighter.DefaultHighlightPainter(MATCH_COLOR);

    /** Painter of the current match (a layer painter, see {@link #createHighlighter()}). */
    private final transient LayeredHighlighter.LayerPainter currentMatchPainter =
            new DefaultHighlighter.DefaultHighlightPainter(CURRENT_MATCH_COLOR);

    /** Tags of the match highlights currently installed (all matches, or the window). */
    private final transient List<Object> matchHighlightTags = new ArrayList<>();

    /** Debounces re-highlighting on viewport changes in windowed mode; created in the constructor. */
    private final Timer viewportTimer;

    /** Cursor over the matches of the displayed document; replaced per document. */
    private transient MatchNavigator navigator = new MatchNavigator(0);

    /** The displayed result, or {@code null} while nothing is shown (private state only). */
    private transient DocumentSearchResult current;

    /** Incremented per {@link #showResult}; stale chained steps compare and stop. */
    private int renderGeneration;

    /** The current match, painted on top of the match highlights, or {@code null} (private state only). */
    private transient MatchRange currentMatch;

    /** Whether the displayed document uses windowed highlighting (more than MAX_EAGER_HIGHLIGHTS). */
    private boolean windowedMode;

    /** Whether a window of matches is currently highlighted (windowed mode). */
    private boolean windowHighlighted;

    /** Start of the highlighted window (inclusive), valid if {@link #windowHighlighted}. */
    private int highlightedFrom;

    /** End of the highlighted window (exclusive), valid if {@link #windowHighlighted}. */
    private int highlightedTo;

    /** Whether all chained steps of the last render have run. */
    private boolean renderComplete = true;

    /**
     * Creates an empty reader.
     *
     * @param downloadWorkflow handles the Download button, not {@code null}
     * @throws NullPointerException if {@code downloadWorkflow} is {@code null}
     */
    public DocumentReaderPanel(DownloadWorkflow downloadWorkflow) {
        this.downloadWorkflow = Objects.requireNonNull(downloadWorkflow, "downloadWorkflow");
        textArea.setEditable(false);
        textArea.setLineWrap(false);
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, textArea.getFont().getSize()));
        textArea.getCaret().setSelectionVisible(true);
        textArea.setHighlighter(createHighlighter());

        previousButton.addActionListener(event -> showPreviousMatch());
        nextButton.addActionListener(event -> showNextMatch());
        downloadButton.addActionListener(event -> onDownloadClicked());

        JPanel navigationPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, INFO_GAP, 0));
        navigationPanel.add(previousButton);
        navigationPanel.add(positionLabel);
        navigationPanel.add(nextButton);
        navigationPanel.add(downloadButton);
        JPanel infoPanel = new JPanel(new BorderLayout(INFO_GAP, 0));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(INFO_GAP, INFO_GAP, INFO_GAP, INFO_GAP));
        queryLabel.setMinimumSize(new Dimension(0, queryLabel.getPreferredSize().height));
        infoPanel.add(queryLabel, BorderLayout.CENTER);
        infoPanel.add(navigationPanel, BorderLayout.EAST);

        JScrollPane scrollPane = new JScrollPane(textArea);
        viewportTimer = new Timer(VIEWPORT_DEBOUNCE_MILLIS, event -> highlightVisibleWindow());
        viewportTimer.setRepeats(false);
        scrollPane.getViewport().addChangeListener(event -> {
            if (windowedMode && renderComplete) {
                viewportTimer.restart();
            }
        });

        setLayout(new BorderLayout());
        add(infoPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        clear();
    }

    /**
     * Renders a result (see algorithm). Returns immediately; rendering continues in chained EDT steps.
     *
     * @param result the selected search result, not {@code null}
     * @param query  the tab's query (for the label), not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public void showResult(DocumentSearchResult result, SearchQuery query) {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(query, "query");
        int generation = ++renderGeneration;
        resetView();
        current = result;
        renderComplete = false;
        queryLabel.setText("Loading...");
        PlainDocument detachedDocument = new PlainDocument();
        List<MatchRange> chunks = chunkPlanner.chunks(result.document().length());
        SwingUtilities.invokeLater(() -> insertChunk(generation, detachedDocument, chunks, 0, query));
    }

    /** Clears the reader (no document selected) and cancels pending render steps. */
    public void clear() {
        renderGeneration++;
        resetView();
        current = null;
        renderComplete = true;
        queryLabel.setText(HINT_TEXT);
        queryLabel.setToolTipText(null);
    }

    /** Moves to the next match cyclically and scrolls to it (D-45). */
    void showNextMatch() {
        if (!navigator.hasMatches()) {
            return;
        }
        navigator.next();
        showCurrentMatch();
    }

    /** Moves to the previous match cyclically and scrolls to it (D-45). */
    void showPreviousMatch() {
        if (!navigator.hasMatches()) {
            return;
        }
        navigator.previous();
        showCurrentMatch();
    }

    /** Handles the Download button (D-46). */
    void onDownloadClicked() {
        if (current != null) {
            downloadWorkflow.run(current.document());
        }
    }

    /**
     * Converts document content to display text: every {@code '\r'} becomes {@code ' '}, one-to-one,
     * so match offsets stay valid (decision N9; D-43). {@code '\n'} is kept (it is the line break
     * of {@code PlainDocument}).
     *
     * @param content the document content, not {@code null}
     * @return text of the same length
     */
    static String toDisplayText(String content) {
        Objects.requireNonNull(content, "content");
        return content.replace('\r', ' ');
    }

    /**
     * Tells whether the last {@link #showResult} or {@link #clear()} has finished all its chained
     * EDT steps (test accessor, D-39, D-44, D-47).
     *
     * @return {@code true} if nothing is pending
     */
    boolean isRenderComplete() {
        return renderComplete;
    }

    /**
     * Chained step: inserts one chunk into the detached document, then schedules the next chunk or
     * attaches the document.
     *
     * @param generation the render generation this step belongs to
     * @param document   the detached document
     * @param chunks     all chunks
     * @param chunkIndex the chunk to insert now
     * @param query      the tab's query
     */
    private void insertChunk(int generation, PlainDocument document, List<MatchRange> chunks, int chunkIndex,
                             SearchQuery query) {
        if (generation != renderGeneration) {
            return;
        }
        if (chunkIndex == chunks.size()) {
            attachDocument(generation, document, query);
            return;
        }
        MatchRange chunk = chunks.get(chunkIndex);
        String text = toDisplayText(current.document().content().substring(chunk.start(), chunk.end()));
        try {
            document.insertString(chunk.start(), text, null);
        } catch (BadLocationException badLocation) {
            throw new IllegalStateException("Chunk " + chunk + " does not fit the document", badLocation);
        }
        SwingUtilities.invokeLater(() -> insertChunk(generation, document, chunks, chunkIndex + 1, query));
    }

    /**
     * Chained step: shows the fully built document, the labels and the first match, then starts
     * highlighting (eager batches or windowed).
     *
     * @param generation the render generation
     * @param document   the complete document
     * @param query      the tab's query
     */
    private void attachDocument(int generation, PlainDocument document, SearchQuery query) {
        textArea.setDocument(document);
        int matchCount = current.matchCount();
        navigator = new MatchNavigator(matchCount);
        queryLabel.setText("Query: \"" + query.text() + "\" - " + matchCount
                + (matchCount == 1 ? " match" : " matches"));
        queryLabel.setToolTipText(queryLabel.getText());
        previousButton.setEnabled(matchCount > 0);
        nextButton.setEnabled(matchCount > 0);
        downloadButton.setEnabled(true);
        windowedMode = !batchPlanner.shouldHighlightEagerly(matchCount);
        if (navigator.hasMatches()) {
            showCurrentMatch();
        }
        if (windowedMode) {
            renderComplete = true;
            return;
        }
        List<List<MatchRange>> batches = batchPlanner.planBatches(current.matches());
        SwingUtilities.invokeLater(() -> highlightBatch(generation, batches, 0));
    }

    /**
     * Chained step (eager mode): highlights one batch of matches.
     *
     * @param generation the render generation
     * @param batches    all batches
     * @param batchIndex the batch to highlight now
     */
    private void highlightBatch(int generation, List<List<MatchRange>> batches, int batchIndex) {
        if (generation != renderGeneration) {
            return;
        }
        if (batchIndex == batches.size()) {
            paintCurrentHighlight();
            renderComplete = true;
            return;
        }
        for (MatchRange match : batches.get(batchIndex)) {
            matchHighlightTags.add(addHighlight(match, matchPainter));
        }
        SwingUtilities.invokeLater(() -> highlightBatch(generation, batches, batchIndex + 1));
    }

    /**
     * Windowed mode (N8): highlights only the matches around the visible text; does nothing if the
     * highlighted window already covers it.
     */
    private void highlightVisibleWindow() {
        if (!windowedMode || current == null) {
            return;
        }
        int textLength = textArea.getDocument().getLength();
        int[] visible = visibleRange();
        int fromOffset = Math.max(0, visible[0] - WINDOW_MARGIN_CHARS);
        int toOffset = Math.min(textLength, visible[1] + WINDOW_MARGIN_CHARS);
        if (windowHighlighted && fromOffset >= highlightedFrom && toOffset <= highlightedTo) {
            return;
        }
        removeMatchHighlights();
        for (MatchRange match : batchPlanner.matchesIntersecting(current.matches(), fromOffset, toOffset)) {
            matchHighlightTags.add(addHighlight(match, matchPainter));
        }
        windowHighlighted = true;
        highlightedFrom = fromOffset;
        highlightedTo = toOffset;
        paintCurrentHighlight();
    }

    /**
     * Returns the character range currently visible in the viewport, or the caret position when the
     * text area has no visible area (e.g. not on screen).
     *
     * @return {@code [from, to]} with {@code from <= to}
     */
    private int[] visibleRange() {
        Rectangle visibleRect = textArea.getVisibleRect();
        int caret = textArea.getCaretPosition();
        if (visibleRect.isEmpty()) {
            return new int[] {caret, caret};
        }
        int from = textArea.viewToModel2D(new Point(visibleRect.x, visibleRect.y));
        int to = textArea.viewToModel2D(new Point(visibleRect.x + visibleRect.width,
                visibleRect.y + visibleRect.height));
        if (from < 0 || to < from) {
            return new int[] {caret, caret};
        }
        return new int[] {from, to};
    }

    /** Moves the caret to the navigator's current match, scrolls to it and updates the label. */
    private void showCurrentMatch() {
        MatchRange match = current.matches().get(navigator.currentIndex().orElseThrow());
        textArea.setCaretPosition(match.start());
        try {
            Rectangle2D matchBounds = textArea.modelToView2D(match.start());
            if (matchBounds != null) {
                textArea.scrollRectToVisible(matchBounds.getBounds());
            }
        } catch (BadLocationException badLocation) {
            throw new IllegalStateException("Match " + match + " is outside the document", badLocation);
        }
        positionLabel.setText(navigator.positionLabel());
        if (windowedMode) {
            highlightVisibleWindow();
        }
        paintCurrentHighlight();
    }

    /** Marks the navigator's current match as the one painted in the current-match color. */
    private void paintCurrentHighlight() {
        MatchRange previous = currentMatch;
        currentMatch = current != null && navigator.hasMatches()
                ? current.matches().get(navigator.currentIndex().orElseThrow())
                : null;
        repaintRange(previous);
        repaintRange(currentMatch);
    }

    /**
     * Repaints the area of a text range (no-op for {@code null} or while the range is not laid out).
     *
     * @param range the range, may be {@code null}
     */
    private void repaintRange(MatchRange range) {
        if (range == null || range.end() > textArea.getDocument().getLength()) {
            return;
        }
        try {
            Rectangle2D startBounds = textArea.modelToView2D(range.start());
            Rectangle2D endBounds = textArea.modelToView2D(range.end());
            if (startBounds != null && endBounds != null) {
                textArea.repaint(startBounds.createUnion(endBounds).getBounds());
            }
        } catch (BadLocationException badLocation) {
            throw new IllegalStateException("Range " + range + " is outside the document", badLocation);
        }
    }

    /**
     * Creates the highlighter of the text area. Swing paints layered highlights in reverse order of
     * insertion, so "adding the current match last" would put it underneath; instead the current
     * match is painted explicitly after (on top of) all match highlights, still below the text.
     *
     * @return the highlighter
     */
    private Highlighter createHighlighter() {
        return new DefaultHighlighter() {
            @Override
            public void paintLayeredHighlights(Graphics graphics, int fromOffset, int toOffset, Shape viewBounds,
                                               JTextComponent editor, View view) {
                super.paintLayeredHighlights(graphics, fromOffset, toOffset, viewBounds, editor, view);
                MatchRange match = currentMatch;
                if (match != null && match.start() < toOffset && match.end() > fromOffset) {
                    currentMatchPainter.paintLayer(graphics,
                            Math.max(fromOffset, match.start()), Math.min(toOffset, match.end()),
                            viewBounds, editor, view);
                }
            }
        };
    }

    /**
     * Adds one highlight.
     *
     * @param match   the range
     * @param painter the painter
     * @return the highlight tag
     */
    private Object addHighlight(MatchRange match, Highlighter.HighlightPainter painter) {
        try {
            return textArea.getHighlighter().addHighlight(match.start(), match.end(), painter);
        } catch (BadLocationException badLocation) {
            throw new IllegalStateException("Match " + match + " is outside the document", badLocation);
        }
    }

    /** Removes all match highlights (not the current-match highlight). */
    private void removeMatchHighlights() {
        Highlighter highlighter = textArea.getHighlighter();
        for (Object tag : matchHighlightTags) {
            highlighter.removeHighlight(tag);
        }
        matchHighlightTags.clear();
        windowHighlighted = false;
    }

    /** Empties the view: no text, no highlights, buttons disabled, labels reset. */
    private void resetView() {
        viewportTimer.stop();
        removeMatchHighlights();
        currentMatch = null;
        windowHighlighted = false;
        windowedMode = false;
        textArea.setDocument(new PlainDocument());
        navigator = new MatchNavigator(0);
        positionLabel.setText(NO_MATCHES_LABEL);
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
        downloadButton.setEnabled(false);
    }

    /**
     * Test accessor.
     *
     * @return the text area
     */
    JTextArea textArea() {
        return textArea;
    }

    /**
     * Test accessor.
     *
     * @return the position label
     */
    JLabel positionLabel() {
        return positionLabel;
    }

    /**
     * Test accessor.
     *
     * @return the query label
     */
    JLabel queryLabel() {
        return queryLabel;
    }

    /**
     * Test accessor.
     *
     * @return the Next button
     */
    JButton nextButton() {
        return nextButton;
    }
}
