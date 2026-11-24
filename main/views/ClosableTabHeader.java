package views;

import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Tab header component of a search tab: the (possibly truncated) query and a small "x" button
 * (U3). Installed with {@code JTabbedPane.setTabComponentAt}.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Rules:</b> transparent panel ({@code setOpaque(false)}) with {@code FlowLayout(LEFT, 4, 0)};
 * label text = {@code TabTitleFormatter.tabTitle(query)}; tooltip of the whole header and the label
 * = {@code TabTitleFormatter.tooltip(query)}; the close button is borderless, not focusable,
 * tooltip "Close tab", and its action runs {@code onClose} exactly once per click. Closing the tab
 * cancels the search running in it (U3) - that is done by {@code onClose}
 * ({@link SearchResultsPane#closeTab}).</p>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class ClosableTabHeader extends JPanel {

    private static final long serialVersionUID = 1L;

    /** Horizontal gap between label and button in pixels. */
    private static final int HEADER_GAP = 4;

    /** Horizontal padding of the close button in pixels. */
    private static final int BUTTON_PADDING = 2;

    /** Shows the tab title. */
    private final JLabel titleLabel;

    /** The "x" button. */
    private final JButton closeButton;

    /** Invoked when the close button is clicked. */
    private final transient Runnable onClose;

    /**
     * Creates the header.
     *
     * @param title   the formatted tab title, not {@code null}
     * @param tooltip the full query text, not {@code null}
     * @param onClose action run on click, not {@code null}
     * @throws NullPointerException if an argument is {@code null}
     */
    public ClosableTabHeader(String title, String tooltip, Runnable onClose) {
        this.titleLabel = new JLabel(Objects.requireNonNull(title, "title"));
        this.closeButton = new JButton("x");
        this.onClose = Objects.requireNonNull(onClose, "onClose");
        Objects.requireNonNull(tooltip, "tooltip");
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, HEADER_GAP, 0));
        setToolTipText(tooltip);
        titleLabel.setToolTipText(tooltip);
        closeButton.setToolTipText("Close tab");
        closeButton.setFocusable(false);
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, BUTTON_PADDING, 0, BUTTON_PADDING));
        closeButton.addActionListener(event -> this.onClose.run());
        add(titleLabel);
        add(closeButton);
    }

    /**
     * Test accessor (D-34).
     *
     * @return the close button
     */
    JButton closeButton() {
        return closeButton;
    }

    /**
     * Test accessor (D-34).
     *
     * @return the title label
     */
    JLabel titleLabel() {
        return titleLabel;
    }
}
