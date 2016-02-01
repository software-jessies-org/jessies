package e.ptextarea;

import e.util.*;
import java.awt.*;
import java.util.*;
import javax.swing.*;

final class PTextAreaRenderer {
    private static final Stopwatch paintStopwatch = Stopwatch.get("PTextAreaRenderer.render");
    
    private static final Color MARGIN_BOUNDARY_COLOR = new Color(0.93f, 0.93f, 0.93f);
    private static final Color MARGIN_OUTSIDE_COLOR = new Color(0.97f, 0.97f, 0.97f);
    
    // Used to get help us match the native UI when we're disabled.
    private static final JLabel disabledLabel;
    static {
        disabledLabel = new JLabel("x");
        disabledLabel.setEnabled(false);
    }
    
    private PTextArea textArea;
    private Graphics2D g;
    private FontMetrics plainFontMetrics;
    
    PTextAreaRenderer(PTextArea textArea, Graphics2D g) {
        this.textArea = textArea;
        this.g = g;
        this.plainFontMetrics = textArea.getFontMetrics(Font.PLAIN);
    }
    
    void render() {
        Stopwatch.Timer timer = paintStopwatch.start();
        try {
            GuiUtilities.setTextAntiAliasing(g, true);
            
            Rectangle bounds = g.getClipBounds();
            
            // Paint what parts of the background we need to paint.
            int paintableBackgroundWidth;
            boolean disabledGtk = (textArea.isEnabled() == false && GuiUtilities.isGtk());
            if (disabledGtk) {
                // The whole background should appear disabled.
                paintableBackgroundWidth = bounds.width;
                g.setColor(disabledLabel.getBackground());
            } else {
                // Only paint that part of the background that isn't part of the right-hand margin.
                paintableBackgroundWidth = paintRightHandMargin(bounds);
                g.setColor(textArea.getBackground());
            }
            // Paint the uninteresting white background, if we're opaque.
            // Otherwise leave that part alone for any JViewport watermark to show through.
            if (textArea.isOpaque()) {
                g.fillRect(bounds.x, bounds.y, paintableBackgroundWidth, bounds.height);
            }
            
            final Font plainFont = textArea.getFont();
            
            // Work out which lines we need to paint.
            final int maxSplitLine = textArea.getSplitLineCount() - 1;
            Insets insets = textArea.getInsets();
            int minLine = (bounds.y - insets.top) / plainFontMetrics.getHeight();
            int maxLine = (bounds.y - insets.top + bounds.height) / plainFontMetrics.getHeight();
            minLine = Math.max(0, Math.min(maxSplitLine, minLine));
            maxLine = Math.max(0, Math.min(maxSplitLine, maxLine));
            
            // Paint the highlights on those lines.
            paintHighlights(minLine, maxLine);
            
            // Paint the text.
            g.setFont(plainFont);
            int startX = insets.left;
            int startY = textArea.getBaseline(minLine);
            if (disabledGtk) {
                // The GNOME "Clearlooks" and Ubuntu "Human" themes both render text in a "shadowed" style when the component is disabled.
                paintTextLines(minLine, maxLine, startX + 1, startY + 1, Color.WHITE);
                paintTextLines(minLine, maxLine, startX, startY, UIManager.getColor("EditorPane.inactiveForeground"));
            } else {
                paintTextLines(minLine, maxLine, startX, startY, textArea.isEnabled() ? null : UIManager.getColor("EditorPane.inactiveForeground"));
            }
        } finally {
            timer.stop();
        }
    }
    
    private void paintTextLines(int minLine, int maxLine, int startX, int startY, Color overrideColor) {
        int baseline = startY;
        int paintCharOffset = textArea.getSplitLine(minLine).getTextIndex(textArea);
        int x = startX;
        int line = minLine;
        int caretOffset = textArea.hasSelection() ? -1 : textArea.getSelectionStart();
        Iterator<PLineSegment> it = textArea.getWrappedSegmentIterator(paintCharOffset);
        while (it.hasNext()) {
            PLineSegment segment = it.next();
            paintCharOffset = segment.getEnd();
            g.setColor(overrideColor != null ? overrideColor : segment.getStyle().getColor());
            segment.paint(g, x, baseline);
            if (segment.getOffset() == caretOffset && segment.isNewline() == false) {
                paintCaret(x, baseline);
            } else if (segment.getOffset() <= caretOffset && segment.getEnd() > caretOffset) {
                int caretX = x + segment.getDisplayWidth(x, caretOffset - segment.getOffset());
                paintCaret(caretX, baseline);
            }
            x += segment.getDisplayWidth(x);
            if (segment.isNewline()) {
                x = startX;
                baseline += plainFontMetrics.getHeight();
                line++;
                if (line > maxLine) {
                    break;
                }
            }
        }
        if (caretOffset == paintCharOffset) {
            paintCaret(x, baseline);
        }
    }
    
    /**
     * Draws the right-hand margin, and returns the width of the rectangle from bounds.x that should be filled with the non-margin background color.
     * Using this in "render" when we paint the whole component's background lets us avoid unnecessary flicker caused by filling the area twice.
     */
    private int paintRightHandMargin(Rectangle bounds) {
        int whiteBackgroundWidth = bounds.width;
        int rightHandMarginColumn = textArea.getRightHandMarginColumn();
        if (rightHandMarginColumn != PTextArea.NO_MARGIN) {
            int offset = plainFontMetrics.stringWidth("n") * rightHandMarginColumn + textArea.getInsets().left;
            g.setColor(MARGIN_BOUNDARY_COLOR);
            g.drawLine(offset, bounds.y, offset, bounds.y + bounds.height);
            g.setColor(MARGIN_OUTSIDE_COLOR);
            g.fillRect(offset + 1, bounds.y, bounds.x + bounds.width - offset - 1, bounds.height);
            whiteBackgroundWidth = (offset - bounds.x);
        }
        return whiteBackgroundWidth;
    }
    
    private void paintHighlights(int minLine, int maxLine) {
        if (textArea.isEnabled() == false) {
            // A disabled component shouldn't render highlights (especially not the selection).
            return;
        }
        // Find the highlights that overlap the area we're redrawing.
        int beginOffset = textArea.getSplitLine(minLine).getTextIndex(textArea);
        SplitLine max = textArea.getSplitLine(maxLine);
        int endOffset = max.getTextIndex(textArea) + max.getLength();
        Collection<PHighlight> highlightList = textArea.getHighlightManager().getHighlightsOverlapping(beginOffset, endOffset);
        // Paint the highlights first...
        for (PHighlight highlight : highlightList) {
            highlight.paint(g);
        }
        // ...and then draw the selection on top.
        // This ensures that even fully opaque highlights don't obscure the selection.
        textArea.getSelection().paint(g);
    }
    
    private void paintCaret(int x, int y) {
        if (textArea.isFocusOwner() == false || textArea.isEnabled() == false) {
            // An unfocused component shouldn't render a caret. There should be at most one caret on the display.
            // A disabled component shouldn't render a caret either.
            return;
        }
        g.setColor(Color.RED);
        int yTop = y - plainFontMetrics.getMaxAscent();
        int yBottom = y + plainFontMetrics.getMaxDescent() - 1;
        g.drawLine(x, yTop + 1, x, yBottom - 1);
        g.drawLine(x, yTop + 1, x + 1, yTop);
        g.drawLine(x, yTop + 1, x - 1, yTop);
        g.drawLine(x, yBottom - 1, x + 1, yBottom);
        g.drawLine(x, yBottom - 1, x - 1, yBottom);
    }
}
