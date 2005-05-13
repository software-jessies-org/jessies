package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
 * The ETextArea action to open a find and replace dialog.
 */
public class FindAndReplaceAction extends ETextAction {
    private static final String ACTION_NAME = "Find/Replace...";
    
    private JTextField patternField = new JTextField(40);
    private JTextField replacementField = new JTextField(40);
    private JLabel statusLabel = new JLabel(" ");
    private JList matchList;
    private JList replacementsList;
    private MatchFinder workerThread;
    
    public FindAndReplaceAction() {
        super(ACTION_NAME);
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("R", false));
        FormDialog.markAsMonitoredField(patternField);
        FormDialog.markAsMonitoredField(replacementField);
    }
    
    private ETextWindow textWindow;
    private ETextArea text;

    private boolean isSelectionMeantAsScope() {
        return (text.getSelectedText().indexOf("\n") != -1);
    }
    
    private void initPatternField() {
        String selection = text.getSelectedText();
        if (selection.length() > 0 && isSelectionMeantAsScope() == false) {
            // Only update the fields if the pattern should change; that way
            // we won't lose the user's replacement from the last time they
            // used this pattern.
            String newPattern = StringUtilities.regularExpressionFromLiteral(selection);
            if (newPattern.equals(patternField.getText()) == false) {
                patternField.setText(newPattern);
                replacementField.setText(StringUtilities.escapeForJava(StringUtilities.regularExpressionFromLiteral(selection)));
            }
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        textWindow = getFocusedTextWindow();
        if (textWindow == null) {
            return;
        }
        text = textWindow.getText();

        initPatternField();
        
        matchList = new JList();
        matchList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = matchList.locationToIndex(e.getPoint());
                    DisplayableMatch match = (DisplayableMatch) matchList.getModel().getElementAt(index);
                    match.doubleClick();
                }
            }
        });
        Font textFont = text.getFont();
        matchList.setFont(textFont);
        matchList.setCellRenderer(new DisplayableMatchRenderer());
        
        replacementsList = new JList();
        replacementsList.setFont(textFont);
        replacementsList.setCellRenderer(new DisplayableMatchRenderer());

        // Make both lists scrollable...
        JScrollPane matchPane = new JScrollPane(matchList);
        JScrollPane replacementsPane = new JScrollPane(replacementsList);
        // ...tie their scroll bars together...
        BoundedRangeModel scrollModel = matchPane.getVerticalScrollBar().getModel();
        replacementsPane.getVerticalScrollBar().setModel(scrollModel);
        // ...and tie the lists' selections together.
        ListSelectionModel selectionModel = matchList.getSelectionModel();
        replacementsList.setSelectionModel(selectionModel);

        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Find:", patternField);
        formPanel.addRow("Replace With:", replacementField);
        formPanel.addRow("", statusLabel);
        formPanel.addRow("Matches:", matchPane);
        formPanel.addRow("Replacements:", replacementsPane);
        formPanel.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        
        // We keep going around the loop of show-dialog/process-text until:
        // (a) the user cancels the dialog.
        // (b) we make it all through the text performing replacements.
        boolean finished = false;
        while (!finished) {
            boolean shouldReplace = FormDialog.show(Edit.getFrame(), "Find/Replace", formPanel, "Replace");
            if (shouldReplace == false) {
                finished = true;
            } else {
                finished = doReplacementsInText();
            }
        }
        
        textWindow = null;
        text = null;
    }

    public boolean doReplacementsInText() {
        try {
            if (isSelectionMeantAsScope()) {
                // There's a suitable selection, so only replace in that.
                int selectionStart = text.getSelectionStart();
                String newText = makeReplacedText(text.getSelectedText());
                text.replaceSelection(newText);
                text.select(selectionStart, selectionStart + newText.length());
            } else {
                // There's no suitable selection, so do the whole text.
                // FIXME - can we try to maintain the selection?
                int caretPosition = text.getUnanchoredSelectionExtreme();
                text.setText(makeReplacedText(text.getText()));
                text.setCaretPosition(caretPosition);
            }
            return true;
        } catch (Exception ex) {
            Edit.showAlert(ACTION_NAME, "Couldn't perform the replacements (" + ex.getMessage() + ").");
            return false;
        }
    }

    public String makeReplacedText(String oldText) {
        String regularExpression = patternField.getText();
        String replacementPattern = StringUtilities.unescapeJava(replacementField.getText());
        Pattern pattern = Pattern.compile(regularExpression, Pattern.MULTILINE);
        return pattern.matcher(oldText).replaceAll(replacementPattern);
    }

    public static class DisplayableMatchRenderer extends EListCellRenderer {
        public DisplayableMatchRenderer() {
            super(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            // If there were captured groups, set the tooltip. (If not, avoid setting an empty
            // tooltip, because that's not the same as no tooltip, and looks rather silly.)
            FindAndReplaceAction.DisplayableMatch match = (FindAndReplaceAction.DisplayableMatch) value;
            String tooltip = match.getToolTipText();
            if (tooltip.length() > 0) {
                setToolTipText(tooltip);
            }
            
            return this;
        }
    }

    public class DisplayableMatch {
        private String html;
        private String toolTip;
        private int lineNumber;

        public DisplayableMatch(final int lineNumber, final Matcher matcher, final String line, final String regularExpression, final String replacement) {
            this.lineNumber = lineNumber;
            this.html = colorize(line, regularExpression, replacement);
            this.toolTip = makeToolTip(matcher);
        }

        public void doubleClick() {
            textWindow.goToLine(lineNumber);
        }
        
        public String getToolTipText() {
            return toolTip;
        }
        
        public String makeToolTip(Matcher matcher) {
            if (matcher.groupCount() == 0) {
                return "";
            }
            
            StringBuffer buffer = new StringBuffer();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (i > 1) buffer.append(", ");
                buffer.append("$" + i + "=\"");
                buffer.append(matcher.group(i));
                buffer.append("\"");
            }
            return buffer.toString();
        }

        public String toString() {
            return html;
        }

        private static final String RED_ON = "<font color=red>";
        private static final String BLUE_ON = "<font color=blue>";

        public String colorize(String line, String regularExpression, String replacement) {
            String colorOn = BLUE_ON;
            if (replacement == null) {
                regularExpression = "(" + regularExpression + ")";
                replacement = "$1";
                colorOn = RED_ON;
            }
            replacement = "\u0000" + replacement + "\u0001";
            Pattern pattern = Pattern.compile(regularExpression);
            String replacedLine;
            try {
                replacedLine = pattern.matcher(line).replaceAll(replacement);
            } catch (IllegalArgumentException ex) {
                return ex.getMessage();
            }
            StringBuffer buffer = new StringBuffer(replacedLine);
            for (int i = 0; i < buffer.length(); ++i) {
                String insert = null;
                if (buffer.charAt(i) == ' ') {
                    insert = "&nbsp;";
                } else if (buffer.charAt(i) == '\t') {
                    insert = "&nbsp;&nbsp;&nbsp;&nbsp;";
                } else if (buffer.charAt(i) == '<') {
                    insert = "&lt;";
                } else if (buffer.charAt(i) == '\u0000') {
                    insert = colorOn;
                } else if (buffer.charAt(i) == '\u0001') {
                    insert = "</font>";
                }
                if (insert != null) {
                    buffer.replace(i, i + 1, insert);
                    i += insert.length() - 1;
                }
            }
            String result = "<html>" + buffer.toString();
            return result;
        }
    }
    
    public void setStatusToGood(int matchCount) {
        patternField.setForeground(UIManager.getColor("TextField.foreground"));
        replacementField.setForeground(UIManager.getColor("TextField.foreground"));
        statusLabel.setText("Matches: " + matchCount);
    }
    
    public void setStatusToBad(String explanation, JTextField badField) {
        badField.setForeground(Color.RED);
        statusLabel.setText(explanation);
    }
    
    private static final int MAX_DISPLAYED_MATCH_COUNT = 100;
    
    public class MatchFinder extends SwingWorker {
        private String regex;
        private String replacement;
        private DefaultListModel matchModel;
        private DefaultListModel replacementsModel;
        
        private PatternSyntaxException patternSyntaxError;
        private IndexOutOfBoundsException replacementSyntaxError;
        
        public void doFindForPattern(String pattern, String replacement) {
            this.matchModel = new DefaultListModel();
            this.replacementsModel = new DefaultListModel();
            this.regex = pattern;
            this.replacement = StringUtilities.unescapeJava(replacement);
            start();
        }
        
        public Object construct() {
            if (regex.length() == 0) {
                return matchModel;
            }

            try {
                Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                String[] lines = text.getText().split("\n");
                int lineStartOffset = 0; // Track our position in the text.
                int matchCount = 0;
                String line;
                for (int i = 0; i < lines.length; ++i, lineStartOffset += line.length() + 1) {
                    line = lines[i];
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        if (matchCount > MAX_DISPLAYED_MATCH_COUNT || Thread.currentThread().isInterrupted()) {
                            return null;
                        }
                        if (isSelectionMeantAsScope()) {
                            // Matches before the selection don't count.
                            if (lineStartOffset + matcher.start() < text.getSelectionStart()) {
                                continue;
                            }
                            // If we're past the end of the selection, we can
                            // stop.
                            if (lineStartOffset + matcher.end() > text.getSelectionEnd()) {
                                break;
                            }
                        }
                        ++matchCount;
                        final int lineNumber = 1 + i;
                        
                        // Record the match.
                        matchModel.addElement(new DisplayableMatch(lineNumber, matcher, line, regex, null));
                        
                        // Record the rewritten line.
                        replacementsModel.addElement(new DisplayableMatch(lineNumber, matcher, line, regex, replacement));
                    }
                }
            } catch (PatternSyntaxException ex) {
                patternSyntaxError = ex;
            } catch (IndexOutOfBoundsException ex) {
                replacementSyntaxError = ex;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return matchModel;
        }
        
        public void finished() {
            Object result = getValue();
            // If the caller has already created a new WorkerThread, then we're obsolete.
            // This happens because of the LIFO order in which Swing seems to execute invokeAndLater callbacks.
            if (workerThread != this) {
                return;
            }
            workerThread = null;
            if (result == null) {
                setStatusToBad("More than " + MAX_DISPLAYED_MATCH_COUNT + " matches. No matches will be shown.", patternField);
            } else if (patternSyntaxError != null) {
                setStatusToBad(patternSyntaxError.getDescription(), patternField);
            } else if (replacementSyntaxError != null) {
                setStatusToBad(replacementSyntaxError.getMessage(), replacementField);
            } else {
                setStatusToGood(matchModel.size());
                matchList.setModel(matchModel);
                replacementsList.setModel(replacementsModel);
            }
        }
    }
    
    public synchronized void showMatches() {
        if (workerThread != null) {
            workerThread.interrupt();
        }
        workerThread = new MatchFinder();
        workerThread.doFindForPattern(patternField.getText(), replacementField.getText());
    }
}
