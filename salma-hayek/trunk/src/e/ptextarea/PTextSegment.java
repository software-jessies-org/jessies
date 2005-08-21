package e.ptextarea;

import e.gui.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A PTextSegment is a PLineSegment which knows how to deal with styled characters.
 * 
 * @author Phil Norman
 */
public class PTextSegment extends PAbstractSegment {
    private String toolTip;
    private ActionListener linkAction;
    
    public PTextSegment(PTextArea textArea, int start, int end, PStyle style) {
        super(textArea, start, end, style);
    }
    
    @Override
    public PLineSegment subSegment(int start, int end) {
        PTextSegment subSegment = new PTextSegment(textArea, start + this.start, end + this.start, style);
        subSegment.toolTip = toolTip;
        subSegment.linkAction = linkAction;
        return subSegment;
    }
    
    @Override
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        return GuiUtilities.getCharOffset(metrics, startX, x, getViewText().toCharArray());
    }
    
    @Override
    public void paint(Graphics2D g, int x, int yBaseline) {
        g.drawString(getViewText(), x, yBaseline);
        if (style.isUnderlined()) {
            paintUnderline(g, x, yBaseline);
        }
    }
    
    private void paintUnderline(Graphics2D g, int x, int yBaseline) {
        FontMetrics metrics = g.getFontMetrics();
        yBaseline += Math.min(2, metrics.getMaxDescent());
        int width = getDisplayWidth(metrics, x);
        g.drawLine(x, yBaseline, x + width, yBaseline);
    }
    
    public String getToolTip() {
        return toolTip;
    }
    
    public void setToolTip(String toolTip) {
        this.toolTip = toolTip;
    }
    
    public void setLinkAction(ActionListener actionListener) {
        this.linkAction = actionListener;
    }
    
    public void linkClicked() {
        linkAction.actionPerformed(null);
    }
    
    @Override
    public String toString() {
        String result = "PTextSegment[" + super.toString();
        if (toolTip != null) {
            result += ",toolTip=" + toolTip;
        }
        if (linkAction != NoOpAction.INSTANCE) {
            result += ",linkAction=" + linkAction;
        }
        result += "]";
        return result;
    }
}
