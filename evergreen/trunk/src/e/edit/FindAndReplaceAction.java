package e.edit;

import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.undo.*;
import e.forms.*;
import e.gui.*;
import e.util.*;

/**
The ETextArea action to open a find and replace dialog.
*/
public class FindAndReplaceAction extends ETextAction {
    public static final String ACTION_NAME = "Find/Replace...";
    
    public class LiveTextField extends EMonitoredTextField {
        public void timerExpired() {
            showMatches();
        }
    }
    
    private LiveTextField patternField = new LiveTextField();
    private LiveTextField replacementField = new LiveTextField();
    private JLabel statusLabel = new JLabel(" ");
    private JList matchList;
    private JList replacementsList;
    private MatchFinder workerThread;
    
    public FindAndReplaceAction() {
        super(ACTION_NAME);
    }
    
    private ETextWindow textWindow;
    private ETextArea text;

    private boolean isSelectionMeantAsScope() {
        return (text.getSelectedText().indexOf("\n") != -1);
    }
    
    private void initPatternField() {
        String selection = text.getSelectedText();
        if (selection.length() > 0 && isSelectionMeantAsScope() == false) {
            patternField.setText(StringUtilities.regularExpressionFromLiteral(selection));
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
        matchList.setCellRenderer(new DisplayableMatchRenderer());
        
        replacementsList = new JList();
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
        if (patternField.getText().length() > 0) {
            showMatches();
        }
        
        // We keep going around the loop of show-dialog/process-text until:
        // (a) the user cancels the dialog.
        // (b) we make it all through the text performing replacements.
        boolean finished = false;
        while (!finished) {
            boolean shouldReplace = FormDialog.show(Edit.getFrame(), "Find/Replace", formPanel);
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
        // Introduce a CompoundEdit to the UndoManager so that all our replacements are treated as a single UndoableEdit.
        CompoundEdit entireEdit = new CompoundEdit();
        UndoManager undoManager = text.getUndoManager();
        undoManager.addEdit(entireEdit);
        try {
            if (isSelectionMeantAsScope()) {
                // There's a selection, so only replace in that.
                int selectionStart = text.getSelectionStart();
                String newText = makeReplacedText(text.getSelectedText());
                text.replaceSelection(newText);
                text.select(selectionStart, selectionStart + newText.length());
            } else {
                // There's no selection, so do the whole text.
                int caretPosition = text.getCaretPosition();
                text.setText(makeReplacedText(text.getText()));
                text.setCaretPosition(caretPosition);
            }
            return true;
        } catch (Exception ex) {
            Edit.showAlert("Find And Replace", "Couldn't perform the replacements (" + ex.getMessage() + ").");
            return false;
        } finally {
            // Ensure that, no matter what happens, we finish the CompoundEdit so life can go back to normal.
            // FIXME: We should probably undo the CompoundEdit if an exception was thrown, but as it is we're likely to want to fix what went wrong.
            entireEdit.end();
        }
    }

    public String makeReplacedText(String oldText) {
        String regularExpression = patternField.getText();
        String replacementPattern = replacementField.getText();
        Pattern pattern = Pattern.compile(regularExpression, Pattern.MULTILINE);
        return pattern.matcher(oldText).replaceAll(replacementPattern);
    }

    public static class DisplayableMatchRenderer extends EListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            // If there were captured groups, set the tooltip. (If not, avoid setting an empty
            // tooltip, because that's not the same as no tooltip, and looks rather silly.)
            DisplayableMatch match = (DisplayableMatch) value;
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
            html = pattern.matcher(line).replaceAll(replacement);
            StringBuffer buffer = new StringBuffer(html);
            for (int i = 0; i < buffer.length(); ++i) {
                String insert = null;
                if (buffer.charAt(i) == ' ') {
                    insert = "&nbsp;";
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
    
    public void setStatusToGood() {
        patternField.setForeground(UIManager.getColor("TextField.foreground"));
        replacementField.setForeground(UIManager.getColor("TextField.foreground"));
        statusLabel.setText(" ");
    }
    
    public void setStatusToBad(String explanation, LiveTextField badField) {
        badField.setForeground(Color.RED);
        statusLabel.setText(explanation);
    }
    
    private static final int MAX_DISPLAYED_MATCH_COUNT = 100;
    
    public class MatchFinder extends SwingWorker {
        private String regex;
        private String replacement;
        private DefaultListModel matchModel;
        private DefaultListModel replacementsModel;
        
        private Exception syntaxError;
        
        public void doFindForPattern(String pattern, String replacement) {
            this.matchModel = new DefaultListModel();
            this.replacementsModel = new DefaultListModel();
            this.regex = pattern;
            this.replacement = replacement;
            setStatusToGood();
            start();
        }
        
        public Object construct() {
            if (regex.length() == 0) {
                return matchModel;
            }

            try {
                Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
                String[] lines = text.getText().split("\n");
                int matchCount = 0;
                for (int i = 0; i < lines.length; ++i) {
                    String line = lines[i];
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        if (matchCount > MAX_DISPLAYED_MATCH_COUNT || Thread.currentThread().isInterrupted()) {
                            return null;
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
                syntaxError = ex;
            } catch (IndexOutOfBoundsException ex) {
                syntaxError = ex;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return matchModel;
        }
        
        public void finished() {
            Object result = get();
            workerThread = null;
            if (result == null) {
                setStatusToBad("More than " + MAX_DISPLAYED_MATCH_COUNT + " matches. No matches will be shown.", patternField);
            } else if (syntaxError != null) {
                if (syntaxError instanceof PatternSyntaxException) {
                    setStatusToBad(((PatternSyntaxException) syntaxError).getDescription(), patternField);
                } else {
                    setStatusToBad(syntaxError.getMessage(), replacementField);
                }
            } else {
                matchList.setModel(matchModel);
                replacementsList.setModel(replacementsModel);
            }
        }
    }
    
    public synchronized void showMatches() {
        while (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
        workerThread = new MatchFinder();
        workerThread.doFindForPattern(patternField.getText(), replacementField.getText());
    }
}
