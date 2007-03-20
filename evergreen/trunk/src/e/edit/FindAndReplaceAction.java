package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import java.util.regex.*;
import javax.swing.*;
import org.jdesktop.swingworker.SwingWorker;

/**
 * The ETextArea action to open a find and replace dialog.
 */
public class FindAndReplaceAction extends ETextAction {
    private JTextField patternField = new JTextField(40);
    private JTextField replacementField = new JTextField(40);
    private JLabel statusLabel = new JLabel(" ");
    private JList matchList;
    private JList replacementsList;
    
    private MatchFinder worker;
    private static final ExecutorService matchFinderExecutor = ThreadUtilities.newSingleThreadExecutor("Find and Replace");
    
    public FindAndReplaceAction() {
        super("Find/Replace...");
        putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("R", false));
        GnomeStockIcon.useStockIcon(this, "gtk-find-and-replace");
    }
    
    private ETextArea currentTextArea;

    private boolean isSelectionMeantAsScope() {
        return currentTextArea.getSelectedText().contains("\n");
    }
    
    private void initPatternField() {
        String selection = currentTextArea.getSelectedText();
        if (selection.length() > 0 && isSelectionMeantAsScope() == false) {
            // Only update the fields if the pattern should change; that way
            // we won't lose the user's replacement from the last time they
            // used this pattern.
            String newPattern = StringUtilities.regularExpressionFromLiteral(selection);
            if (newPattern.equals(patternField.getText()) == false) {
                patternField.setText(newPattern);
                replacementField.setText(StringUtilities.escapeForJava(Matcher.quoteReplacement(selection)));
            }
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        currentTextArea = getFocusedTextArea();
        if (currentTextArea == null) {
            return;
        }

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
        Font textFont = currentTextArea.getFont();
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
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Find/Replace");
        form.setTypingTimeoutActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showMatches();
            }
        });
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("Find:", patternField);
        formPanel.addRow("Replace With:", replacementField);
        formPanel.addRow("", PatternUtilities.addRegularExpressionHelpToComponent(statusLabel));
        formPanel.addRow("Matches:", matchPane);
        formPanel.addRow("Replacements:", replacementsPane);
        
        // We keep going around the loop of show-dialog/process-text until:
        // (a) the user cancels the dialog.
        // (b) we make it all through the text performing replacements.
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                return doReplacementsInText();
            }
        });
        form.show("Replace");
        currentTextArea = null;
    }
    
    public boolean doReplacementsInText() {
        try {
            if (isSelectionMeantAsScope()) {
                // There's a suitable selection, so only replace in that.
                int selectionStart = currentTextArea.getSelectionStart();
                String newText = makeReplacedText(currentTextArea.getSelectedText());
                currentTextArea.replaceSelection(newText);
                currentTextArea.select(selectionStart, selectionStart + newText.length());
            } else {
                // There's no suitable selection, so do the whole text.
                // FIXME - can we try to maintain the selection?
                int caretPosition = currentTextArea.getUnanchoredSelectionExtreme();
                currentTextArea.setText(makeReplacedText(currentTextArea.getText()));
                currentTextArea.setCaretPosition(caretPosition);
            }
            return true;
        } catch (Exception ex) {
            Evergreen.getInstance().showAlert("Couldn't replace", "There was a problem performing the replacements: " + ex.getMessage() + ".");
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
            
            // If there were captured groups, set the tool tip.
            // (If not, avoid setting an empty tool tip, because that's not the same as no tool tip, and looks rather silly.)
            FindAndReplaceAction.DisplayableMatch match = (FindAndReplaceAction.DisplayableMatch) value;
            String toolTip = match.getToolTipText();
            if (toolTip.length() > 0) {
                setToolTipText(toolTip);
            }
            
            return this;
        }
    }

    public class DisplayableMatch {
        private static final String RED_ON = "<font color=red>";
        private static final String BLUE_ON = "<font color=blue>";
        
        private String html;
        private String toolTip;
        private int lineNumber;

        public DisplayableMatch(final int lineNumber, final Matcher matcher, final String line, final String regularExpression, final String replacement) {
            this.lineNumber = lineNumber;
            this.html = colorize(line, regularExpression, replacement);
            this.toolTip = makeToolTip(matcher);
        }

        public void doubleClick() {
            currentTextArea.goToLine(lineNumber);
        }
        
        public String getToolTipText() {
            return toolTip;
        }
        
        public String makeToolTip(Matcher matcher) {
            if (matcher.groupCount() == 0) {
                return "";
            }
            
            StringBuilder buffer = new StringBuilder();
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

        public String colorize(String line, String regularExpression, String replacement) {
            // Work around the Swing misfeature where empty labels have zero height by ensuring that we always have something (even if it's invisible) in our labels.
            if (replacement == null && line.length() == 0) {
                line = " ";
            } else if (replacement != null && replacement.length() == 0) {
                replacement = " ";
            }
            
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
            StringBuilder buffer = new StringBuilder(replacedLine);
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
            String result = "<html><body>" + buffer.toString();
            return result;
        }
    }
    
    private void setStatusToGood(int matchCount) {
        patternField.setForeground(UIManager.getColor("TextField.foreground"));
        replacementField.setForeground(UIManager.getColor("TextField.foreground"));
        setStatus("Matches: " + matchCount);
    }
    
    private void setStatusToBad(String explanation, JTextField badField) {
        badField.setForeground(Color.RED);
        setStatus(explanation);
    }
    
    private void setStatus(String explanation) {
        statusLabel.setText(explanation);
    }
    
    private static final int MAX_DISPLAYED_MATCH_COUNT = 1000;
    
    public class MatchFinder extends SwingWorker<DefaultListModel, Object> {
        private String regex;
        private String replacement;
        
        private DefaultListModel matchModel;
        private DefaultListModel replacementsModel;
        
        private PatternSyntaxException patternSyntaxError;
        private IndexOutOfBoundsException replacementSyntaxError;
        private boolean aborted = false;
        
        public MatchFinder(String pattern, String replacement) {
            this.matchModel = new DefaultListModel();
            this.replacementsModel = new DefaultListModel();
            this.regex = pattern;
            this.replacement = StringUtilities.unescapeJava(replacement);
        }
        
        @Override
        protected DefaultListModel doInBackground() {
            if (regex.length() == 0) {
                return matchModel;
            }

            try {
                Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                String[] lines = currentTextArea.getText().split("\n");
                int lineStartOffset = 0; // Track our position in the text.
                int matchCount = 0;
                String line;
                for (int i = 0; i < lines.length; ++i, lineStartOffset += line.length() + 1) {
                    line = lines[i];
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        if (matchCount > MAX_DISPLAYED_MATCH_COUNT || isCancelled()) {
                            aborted = true;
                            return null;
                        }
                        if (isSelectionMeantAsScope()) {
                            // Matches before the selection don't count.
                            if (lineStartOffset + matcher.start() < currentTextArea.getSelectionStart()) {
                                continue;
                            }
                            // If we're past the end of the selection, we can
                            // stop.
                            if (lineStartOffset + matcher.end() > currentTextArea.getSelectionEnd()) {
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
            }
            return matchModel;
        }
        
        @Override
        public void done() {
            synchronized (FindAndReplaceAction.this) {
                worker = null;
            }
            
            if (isCancelled()) {
                return;
            }
            
            if (aborted) {
                setStatus("More than " + MAX_DISPLAYED_MATCH_COUNT + " matches. No matches will be shown.");
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
        if (worker != null) {
            worker.cancel(true);
        }
        worker = new MatchFinder(patternField.getText(), replacementField.getText());
        matchFinderExecutor.submit(worker);
    }
}
