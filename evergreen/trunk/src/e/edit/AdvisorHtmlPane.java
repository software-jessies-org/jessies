package e.edit;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class AdvisorHtmlPane extends JComponent implements HyperlinkListener {
    private static final DefaultHighlighter.DefaultHighlightPainter FIND_HIGHLIGHT_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(e.ptextarea.PFind.MATCH_COLOR);
    
    private JTextPane textPane;
    private EStatusBar statusBar;
    
    public AdvisorHtmlPane() {
        statusBar = new EStatusBar();
        initTextPane();
        setLayout(new BorderLayout());
        add(new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);
    }
    
    private void initTextPane() {
        this.textPane = new JTextPane();
        textPane.setContentType("text/html");
        textPane.setDragEnabled(false);
        textPane.setEditable(false);
        textPane.addHyperlinkListener(this);
        
        ComponentUtilities.initKeyBinding(textPane, new FindAction(textPane));
        
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        
        Font bodyFont = ChangeFontAction.getConfiguredFont();
        styleSheet.removeStyle("body");
        String body = "body { font-family: \"" + bodyFont.getFamily() + "\", sans-serif; font-size: " + bodyFont.getSize() + "pt; margin: 0px 2px 20px 2px; }";
        styleSheet.addRule(body);
        //System.err.println(body);
        
        Font preFont = ChangeFontAction.getConfiguredFixedFont();
        styleSheet.removeStyle("pre");
        String pre = "pre { font-family: \"" + preFont.getFamily() + "\", monospace; font-size: " + preFont.getSize() + "pt; background-color: #eeeeff; border-style: solid; border-width: thin; border-color: #bbbbff; padding: 5px 5px 5px 5px; }";
        styleSheet.addRule(pre);
        //System.err.println(pre);
    }
    
    public void setText(String text) {
        textPane.setText(text);
        textPane.setCaretPosition(0);
    }
    
    public void hyperlinkUpdate(HyperlinkEvent e) {
        // Welcome to the wonderful world of OOP, featuring nested-if polymorphism.
        if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            statusBar.setText("Open " + e.getDescription());
        } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
            statusBar.clearStatusBar();
        } else if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            if (e instanceof HTMLFrameHyperlinkEvent) {
                ((HTMLDocument) textPane.getDocument()).processHTMLFrameHyperlinkEvent((HTMLFrameHyperlinkEvent) e);
            } else {
                Advisor.getInstance().linkClicked(e.getDescription());
            }
        }
    }
    
    public static class FindAction extends AbstractAction {
        private JTextPane textPane;
        private JTextField findField = new JTextField(40);
        
        public FindAction(JTextPane textPane) {
            super("Find...");
            this.textPane = textPane;
            putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("F", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            String selection = textPane.getSelectedText();
            if (selection != null && selection.length() > 0) {
                findField.setText(StringUtilities.regularExpressionFromLiteral(selection));
            }
            
            AbstractFindDialog findDialog = new AbstractFindDialog() {
                public int updateFindResults(String regularExpression) {
                    return findAllMatches(regularExpression);
                }
                
                public void clearFindResults() {
                    findAllMatches(null);
                }
            };
            findDialog.showFindDialog(textPane, findField);
        }
        
        public int findAllMatches(String regularExpression) {
            Highlighter highlighter = textPane.getHighlighter();
            
            // Remove any existing find highlights.
            Highlighter.Highlight[] highlights = highlighter.getHighlights();
            for (Highlighter.Highlight highlight : highlights) {
                if (highlight.getPainter() == FIND_HIGHLIGHT_PAINTER) {
                    highlighter.removeHighlight(highlight);
                }
            }
            
            if (regularExpression == null || regularExpression.length() == 0) {
                return 0;
            }
            
            String content = textPane.getText();
            if (content == null) {
                return 0;
            }
            
            Pattern pattern = PatternUtilities.smartCaseCompile(regularExpression);
            int matchCount = 0;
            HTMLDocument document = (HTMLDocument) textPane.getDocument();
            for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.CONTENT); it.isValid(); it.next()) {
                try {
                    String fragment = document.getText(it.getStartOffset(), it.getEndOffset() - it.getStartOffset());
                    Matcher matcher = pattern.matcher(fragment);
                    while (matcher.find()) {
                        highlighter.addHighlight(it.getStartOffset() + matcher.start(), it.getStartOffset() + matcher.end(), FIND_HIGHLIGHT_PAINTER);
                        ++matchCount;
                    }
                } catch (BadLocationException ex) {
                }
            }
            return matchCount;
        }
    }
}
