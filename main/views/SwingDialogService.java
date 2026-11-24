package views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

/**
 * Production {@link DialogService} based on {@code JOptionPane} and {@code JFileChooser}.
 *
 * <p><b>Owner:</b> Member D (Views).</p>
 *
 * <p><b>Requirements covered:</b> D1 (title prompt with default), D2 (overwrite confirmation),
 * D7 (delete confirmation with a scroll pane of bounded size), D8 (save chooser + overwrite),
 * UC5 (error dialog with the specific message).</p>
 *
 * <p><b>Rules:</b></p>
 * <ul>
 *   <li>Every dialog uses {@link #parent} as owner so it is centered on, and modal to, the main frame.</li>
 *   <li>The delete confirmation embeds {@link #buildDeletionList(List)}; the dialog therefore never
 *       grows beyond {@link #DELETION_LIST_MAX_SIZE}, whatever the number of titles (D7).</li>
 *   <li>Long error messages are shown in a non-editable, line-wrapping {@code JTextArea} inside a
 *       {@code JScrollPane} limited to 500 x 200 pixels.</li>
 *   <li>The file chooser for downloads pre-selects {@code suggestedFileName} in the last used directory.</li>
 * </ul>
 *
 * <p><b>Threading:</b> EDT only.</p>
 */
public final class SwingDialogService implements DialogService {

    /** Maximum preferred size of the scrollable list in the delete confirmation (D7). */
    static final Dimension DELETION_LIST_MAX_SIZE = new Dimension(420, 240);

    /** Maximum number of visible rows of the deletion list before it scrolls. */
    private static final int DELETION_LIST_VISIBLE_ROWS = 12;

    /** Size of the scrollable area used for long error / info messages. */
    private static final Dimension MESSAGE_AREA_SIZE = new Dimension(500, 200);

    /** Messages up to this length (without line breaks) are shown as plain text. */
    private static final int SHORT_MESSAGE_LENGTH = 80;

    /** Width of the text input fields in columns. */
    private static final int INPUT_COLUMNS = 30;

    /** Vertical gap inside dialog content panels, in pixels. */
    private static final int DIALOG_GAP = 6;

    /** Label of the cancel button of confirmations. */
    private static final String CANCEL_LABEL = "Cancel";

    /** Owner of all dialogs; set once the main frame exists ({@code null} = no owner yet, Swing allows it). */
    private Component parent;

    /** Directory the file choosers open in; the last one used ({@code null} = user's default). */
    private File lastDirectory;

    /** Creates a service without a parent; call {@link #setParent(Component)} once the frame exists. */
    public SwingDialogService() {
        // parent is set later
    }

    /**
     * Sets the owner component of all dialogs (the main frame).
     *
     * @param parent the owner, not {@code null}
     */
    public void setParent(Component parent) {
        this.parent = Objects.requireNonNull(parent, "parent");
    }

    /**
     * Builds the content of the delete confirmation: a {@code JScrollPane} containing a
     * {@code JList<String>} with all titles, whose preferred size is at most
     * {@link #DELETION_LIST_MAX_SIZE}. Static and Swing-model-only so it can be tested headless
     * (D-14, D-15).
     *
     * @param titles the titles, not {@code null}
     * @return the scroll pane
     */
    static JComponent buildDeletionList(List<String> titles) {
        Objects.requireNonNull(titles, "titles");
        JList<String> titleList = new JList<>(titles.toArray(new String[0]));
        titleList.setVisibleRowCount(Math.min(titles.size(), DELETION_LIST_VISIBLE_ROWS));
        JScrollPane scrollPane = new JScrollPane(titleList);
        Dimension preferred = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(
                Math.min(preferred.width, DELETION_LIST_MAX_SIZE.width),
                Math.min(preferred.height, DELETION_LIST_MAX_SIZE.height)));
        return scrollPane;
    }

    @Override
    public Optional<Path> chooseFileToImport() {
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("Import document");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        lastDirectory = chooser.getCurrentDirectory();
        return Optional.ofNullable(chooser.getSelectedFile()).map(File::toPath);
    }

    @Override
    public Optional<String> askTitle(String suggestedTitle, Optional<String> errorMessage) {
        Objects.requireNonNull(suggestedTitle, "suggestedTitle");
        Objects.requireNonNull(errorMessage, "errorMessage");
        return askText("Import document", "Title:", suggestedTitle, errorMessage);
    }

    @Override
    public boolean confirmOverwriteDocument(String title) {
        Objects.requireNonNull(title, "title");
        return confirm("Document exists",
                "A document titled \"" + title + "\" already exists. Overwrite it?", "Overwrite");
    }

    @Override
    public boolean confirmDelete(List<String> titles) {
        Objects.requireNonNull(titles, "titles");
        JPanel contentPanel = new JPanel(new BorderLayout(0, DIALOG_GAP));
        String question = titles.size() == 1
                ? "Delete this document?"
                : "Delete these " + titles.size() + " documents?";
        contentPanel.add(new JLabel(question), BorderLayout.NORTH);
        contentPanel.add(buildDeletionList(titles), BorderLayout.CENTER);
        return confirm("Delete documents", contentPanel, "Delete");
    }

    @Override
    public Optional<String> askSearchQuery(String initialText, Optional<String> errorMessage) {
        Objects.requireNonNull(initialText, "initialText");
        Objects.requireNonNull(errorMessage, "errorMessage");
        return askText("Search", "Search for:", initialText, errorMessage);
    }

    @Override
    public Optional<Path> chooseDownloadTarget(String suggestedFileName) {
        Objects.requireNonNull(suggestedFileName, "suggestedFileName");
        JFileChooser chooser = new JFileChooser(lastDirectory);
        chooser.setDialogTitle("Download document");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), suggestedFileName));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return Optional.empty();
        }
        lastDirectory = chooser.getCurrentDirectory();
        return Optional.ofNullable(chooser.getSelectedFile()).map(File::toPath);
    }

    @Override
    public boolean confirmOverwriteFile(Path target) {
        Objects.requireNonNull(target, "target");
        return confirm("File exists", "\"" + target + "\" already exists. Overwrite it?", "Overwrite");
    }

    @Override
    public void showError(String title, String message) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        JOptionPane.showMessageDialog(parent, messageComponent(message), title, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public void showInfo(String title, String message) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        JOptionPane.showMessageDialog(parent, messageComponent(message), title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a modal text input with an optional red error line above the field.
     *
     * @param dialogTitle  the dialog title
     * @param prompt       the label in front of the field
     * @param initialText  the pre-filled text
     * @param errorMessage the optional error from the previous attempt
     * @return the raw text, or empty if cancelled
     */
    private Optional<String> askText(String dialogTitle, String prompt, String initialText,
                                     Optional<String> errorMessage) {
        JTextField inputField = new JTextField(initialText, INPUT_COLUMNS);
        inputField.selectAll();
        JPanel contentPanel = new JPanel(new BorderLayout(0, DIALOG_GAP));
        errorMessage.ifPresent(error -> {
            JLabel errorLabel = new JLabel(error);
            errorLabel.setForeground(Color.RED);
            contentPanel.add(errorLabel, BorderLayout.NORTH);
        });
        contentPanel.add(new JLabel(prompt), BorderLayout.WEST);
        contentPanel.add(inputField, BorderLayout.CENTER);
        inputField.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                inputField.requestFocusInWindow();
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                // nothing to do
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // nothing to do
            }
        });
        int answer = JOptionPane.showConfirmDialog(parent, contentPanel, dialogTitle,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        return answer == JOptionPane.OK_OPTION ? Optional.of(inputField.getText()) : Optional.empty();
    }

    /**
     * Shows a modal question with a confirm button and "Cancel" (the default).
     *
     * @param dialogTitle  the dialog title
     * @param message      a string or a component
     * @param confirmLabel the label of the confirm button
     * @return {@code true} if the confirm button was chosen
     */
    private boolean confirm(String dialogTitle, Object message, String confirmLabel) {
        Object[] options = {confirmLabel, CANCEL_LABEL};
        int answer = JOptionPane.showOptionDialog(parent, message, dialogTitle, JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, CANCEL_LABEL);
        return answer == 0;
    }

    /**
     * Returns the message itself if it is short, otherwise a wrapping, scrollable, read-only text area.
     *
     * @param message the message
     * @return a string or a component for {@code JOptionPane}
     */
    private static Object messageComponent(String message) {
        if (message.length() <= SHORT_MESSAGE_LENGTH && message.indexOf('\n') < 0) {
            return message;
        }
        JTextArea messageTextArea = new JTextArea(message);
        messageTextArea.setEditable(false);
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        messageTextArea.setOpaque(false);
        JScrollPane scrollPane = new JScrollPane(messageTextArea);
        scrollPane.setPreferredSize(MESSAGE_AREA_SIZE);
        scrollPane.setBorder(null);
        return scrollPane;
    }
}
