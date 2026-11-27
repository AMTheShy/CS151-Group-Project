package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import models.DocumentSearchResult;
import models.Snippet;

/**
 * Two-column cell renderer of the search drop-down (U4, original UC2 "title | snippet of the first
 * match").
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Rules:</b> one reusable {@code JPanel} with a {@code BorderLayout}: west = title label
 * (bold, fixed preferred width {@value #TITLE_COLUMN_WIDTH} px, truncated by Swing with "..."),
 * center = snippet label. The snippet label shows {@link models.Snippet#text()} as HTML with the
 * highlighted range in bold: characters must be HTML-escaped ({@code & < > "}) before inserting
 * {@code <b>} tags. A {@code null} value (the combo box's "nothing selected" display) renders as
 * an empty panel with the text "Select a document". Selection colors come from the {@code JList}.</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class SearchResultCellRenderer implements ListCellRenderer<DocumentSearchResult> {

    /** Preferred width of the title column in pixels. */
    static final int TITLE_COLUMN_WIDTH = 220;

    /** Text shown when no result is selected. */
    static final String NOTHING_SELECTED_TEXT = "Select a document";

    /** Gap between the two columns in pixels. */
    private static final int CELL_GAP = 8;

    /** Padding around a cell in pixels. */
    private static final int CELL_PADDING = 2;

    /** The reused cell component. */
    private final JPanel cellPanel = new JPanel();

    /** Shows the title. */
    private final JLabel titleLabel = new JLabel();

    /** Shows the snippet with the match in bold. */
    private final JLabel snippetLabel = new JLabel();

    /** Creates the renderer and assembles its reusable cell component. */
    public SearchResultCellRenderer() {
        cellPanel.setLayout(new BorderLayout(CELL_GAP, 0));
        cellPanel.setOpaque(true);
        cellPanel.setBorder(BorderFactory.createEmptyBorder(CELL_PADDING, CELL_PADDING, CELL_PADDING, CELL_PADDING));
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        titleLabel.setPreferredSize(new Dimension(TITLE_COLUMN_WIDTH, titleLabel.getPreferredSize().height));
        cellPanel.add(titleLabel, BorderLayout.WEST);
        cellPanel.add(snippetLabel, BorderLayout.CENTER);
    }

    /** {@inheritDoc} Test: D-33. */
    @Override
    public Component getListCellRendererComponent(JList<? extends DocumentSearchResult> list,
                                                  DocumentSearchResult value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        if (value == null) {
            titleLabel.setText(NOTHING_SELECTED_TEXT);
            snippetLabel.setText("");
        } else {
            titleLabel.setText(value.title());
            snippetLabel.setText(snippetHtml(value));
        }
        Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
        Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();
        cellPanel.setBackground(background);
        titleLabel.setForeground(foreground);
        snippetLabel.setForeground(foreground);
        cellPanel.setEnabled(list.isEnabled());
        return cellPanel;
    }

    /**
     * Builds the HTML of the snippet label: escaped text with {@code <b>...</b>} around the highlight.
     *
     * @param result the result
     * @return HTML starting with {@code <html>}
     */
    static String snippetHtml(DocumentSearchResult result) {
        Objects.requireNonNull(result, "result");
        Snippet snippet = result.snippet();
        String text = snippet.text();
        int start = snippet.highlight().start();
        int end = snippet.highlight().end();
        return "<html>" + escapeHtml(text.substring(0, start))
                + "<b>" + escapeHtml(text.substring(start, end)) + "</b>"
                + escapeHtml(text.substring(end)) + "</html>";
    }

    /**
     * Escapes {@code & < > "} so that user text can be embedded in Swing HTML, and turns every space
     * into {@code &nbsp;} because HTML would collapse consecutive spaces, which the search treats as
     * significant (S2).
     *
     * @param text the raw text, not {@code null}
     * @return the escaped text
     */
    static String escapeHtml(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            switch (character) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case ' ':
                    escaped.append("&nbsp;");
                    break;
                default:
                    escaped.append(character);
                    break;
            }
        }
        return escaped.toString();
    }

    /**
     * Test accessor.
     *
     * @return the title label
     */
    JLabel titleLabel() {
        return titleLabel;
    }

    /**
     * Test accessor.
     *
     * @return the snippet label
     */
    JLabel snippetLabel() {
        return snippetLabel;
    }
}
