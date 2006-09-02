package e.gui;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class JTextComponentUtilities {
    private static final DefaultHighlighter.DefaultHighlightPainter FIND_HIGHLIGHT_PAINTER = new DefaultHighlighter.DefaultHighlightPainter(e.ptextarea.PFind.MATCH_COLOR);
    
    private JTextComponentUtilities() {
        // This class cannot be instantiated.
    }
    
    public static void goToSelection(final JTextComponent textComponent, final int startOffset, final int endOffset) {
        if (EventQueue.isDispatchThread()) {
            doGoToSelection(textComponent, startOffset, endOffset);
        } else {
            try {
                EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        doGoToSelection(textComponent, startOffset, endOffset);
                    }
                });
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
    
    /**
     * Use goToSelection instead. This method is not thread-safe.
     */
    private static void doGoToSelection(JTextComponent textComponent, int startOffset, int endOffset) {
        ensureVisibilityOfOffset(textComponent, startOffset);
        textComponent.select(startOffset, endOffset);
    }
    
    public static void ensureVisibilityOfOffset(JTextComponent textComponent, int offset) {
        JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, textComponent);
        if (viewport == null) {
            return;
        }
        int height = viewport.getExtentSize().height;
        try {
            Rectangle viewRectangle = textComponent.modelToView(offset);
            if (viewRectangle == null) {
                return;
            }
            int y = viewRectangle.y - height/2;
            y = Math.max(0, y);
            y = Math.min(y, textComponent.getHeight() - height);
            viewport.setViewPosition(new Point(0, y));
        } catch (javax.swing.text.BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    
    public static void removeAllHighlightsUsingPainter(final JTextComponent textComponent, final Highlighter.HighlightPainter painter) {
        Highlighter highlighter = textComponent.getHighlighter();
        for (Highlighter.Highlight highlight : highlighter.getHighlights()) {
            if (highlight.getPainter() == painter) {
                highlighter.removeHighlight(highlight);
            }
        }
    }
    
    public static void findNextHighlight(final JTextComponent textComponent, final Position.Bias bias, final Highlighter.HighlightPainter painter) {
        Highlighter highlighter = textComponent.getHighlighter();
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        final boolean forwards = (bias == Position.Bias.Forward);
        int start = forwards ? 0 : highlights.length - 1;
        int stop = forwards ? highlights.length : -1;
        int step = forwards ? 1 : -1;
        for (int i = start; i != stop; i += step) {
            if (highlights[i].getPainter() == painter) {
                if (highlighterIsNext(textComponent, forwards, highlights[i])) {
                    goToSelection(textComponent, highlights[i].getStartOffset(), highlights[i].getEndOffset());
                    return;
                }
            }
        }
    }
    
    private static boolean highlighterIsNext(JTextComponent textComponent, boolean forwards, Highlighter.Highlight highlight) {
        final int minOffset = Math.min(highlight.getStartOffset(), highlight.getEndOffset());
        final int maxOffset = Math.max(highlight.getStartOffset(), highlight.getEndOffset());
        if (forwards) {
            return minOffset > textComponent.getSelectionEnd();
        } else {
            return maxOffset < textComponent.getSelectionStart();
        }
    }
    
    /**
     * Makes C-F bring up a regular-expression find dialog that highlights all matches, like Firefox.
     * C-D and C-G are "Find Next" and "Find Previous" respectively.
     * 
     * FIXME: currently only works when the document is an instance of HTMLDocument.
     */
    public static void addFindFunctionalityTo(JTextPane textPane) {
        ComponentUtilities.initKeyBinding(textPane, new JTextComponentUtilities.FindAction(textPane));
        ComponentUtilities.initKeyBinding(textPane, new JTextComponentUtilities.FindAgainAction(textPane, true));
        ComponentUtilities.initKeyBinding(textPane, new JTextComponentUtilities.FindAgainAction(textPane, false));
    }
    
    public static class FindAction extends AbstractAction {
        private JTextPane textPane;
        private JTextField findField = new JTextField(40);
        
        public FindAction(JTextPane textPane) {
            super("Find...");
            this.textPane = textPane;
            putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke("F", false));
            // Make C-D and C-G work even while our text field has the focus.
            ComponentUtilities.initKeyBinding(findField, new JTextComponentUtilities.FindAgainAction(textPane, true));
            ComponentUtilities.initKeyBinding(findField, new JTextComponentUtilities.FindAgainAction(textPane, false));
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
            JTextComponentUtilities.removeAllHighlightsUsingPainter(textPane, FIND_HIGHLIGHT_PAINTER);
            
            if (regularExpression == null || regularExpression.length() == 0) {
                return 0;
            }
            
            String content = textPane.getText();
            if (content == null) {
                return 0;
            }
            
            Pattern pattern = PatternUtilities.smartCaseCompile(regularExpression);
            int matchCount = 0;
            Highlighter highlighter = textPane.getHighlighter();
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
    
    public static class FindAgainAction extends AbstractAction {
        private JTextPane textPane;
        private boolean next;
        
        public FindAgainAction(JTextPane textPane, boolean next) {
            super(next ? "Find Next" : "Find Previous");
            this.textPane = textPane;
            this.next = next;
            putValue(ACCELERATOR_KEY, e.util.GuiUtilities.makeKeyStroke(next ? "G" : "D", false));
        }
        
        public void actionPerformed(ActionEvent e) {
            JTextComponentUtilities.findNextHighlight(textPane, next ? Position.Bias.Forward : Position.Bias.Backward, FIND_HIGHLIGHT_PAINTER);
        }
    }
}
