package e.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;

public class JTextComponentUtilities {
    public static void goToSelection(final JTextComponent textComponent, final int startOffset, final int endOffset) {
        if (EventQueue.isDispatchThread()) {
            doGoToSelection(textComponent, startOffset, endOffset);
        } else {
            try {
                SwingUtilities.invokeLater(new Runnable() {
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
        Highlighter.Highlight[] highlights = highlighter.getHighlights();
        for (int i = 0; i < highlights.length; i++) {
            if (highlights[i].getPainter() == painter) {
                highlighter.removeHighlight(highlights[i]);
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
    
    private JTextComponentUtilities() {
        // This class cannot be instantiated.
    }
}
