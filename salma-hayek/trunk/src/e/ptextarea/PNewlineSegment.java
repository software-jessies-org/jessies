package e.ptextarea;

import e.util.*;
import java.awt.*;

public class PNewlineSegment extends PAbstractSegment {
    public static final boolean WRAPPED = false;
    public static final boolean HARD_NEWLINE = true;

    private static final Stroke WRAP_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] { 1.0f, 2.0f }, 0.0f);
    
    private boolean newlineType;
    
    public PNewlineSegment(PTextArea textArea, int start, int end, boolean isHardNewline) {
        super(textArea, start, end, PStyle.NEWLINE);
        this.newlineType = isHardNewline;
    }
    
    @Override
    public PLineSegment subSegment(int start, int end) {
        return new PNewlineSegment(textArea, start + this.start, end + this.start, newlineType);
    }
    
    /** Returns true if this segment represents any line break, be it caused by line wrap or a newline character. */
    public boolean isNewline() {
        return true;
    }
    
    /** Returns true only if this segment represents a hard newline (one representing a newline character). */
    public boolean isHardNewline() {
        return newlineType;
    }
    
    @Override
    public int getDisplayWidth(FontMetrics metrics, int startX) {
        // Might need to cope with being as wide as the available space on screen if we move dotted
        // line painting in here.
        return 0;
    }
    
    @Override
    public int getDisplayWidth(FontMetrics metrics, int startX, int charOffset) {
        return subSegment(0, charOffset).getDisplayWidth(metrics, startX);
    }
    
    @Override
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        return 0;
    }
    
    @Override
    public void paint(Graphics2D g, int x, int yBaseline) {
        if (newlineType == WRAPPED) {
            paintWrapMark(g, x, yBaseline);
        }
    }
    
    private void paintWrapMark(Graphics2D g, int x, int y) {
        // Don't explicitly set a color (we used to always use black).
        // That lets us preserve the "overrideColor" from paintTextLines, which means we render correctly when disabled under the GTK LAF (a drop-shadow effect).
        // If we're rendering a wrapped comment or string literal, we now use the comment or string literal color.
        // To my eye, that looks fine, unlike the way we were butchering the GTK disabled effect, which had been bothering me for months.
        Stroke oldStroke = g.getStroke();
        g.setStroke(WRAP_STROKE);
        int yMiddle = y - g.getFontMetrics().getMaxAscent() / 2;
        g.drawLine(x, yMiddle, textArea.getWidth() - textArea.getInsets().right, yMiddle);
        g.setStroke(oldStroke);
    }
    
    @Override
    public String toString() {
        return "PNewlineSegment[" + style.getName() + ", [" + getOffset() + ", " + getEnd() + "], " + StringUtilities.escapeForJava(getViewText()) + (newlineType ? ", HARD" : ", SOFT") + "]";
    }
}
