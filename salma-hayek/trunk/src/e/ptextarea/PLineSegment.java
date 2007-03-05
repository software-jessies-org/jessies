package e.ptextarea;

import java.awt.*;

/**
 * A PLineSegment represents a section of text which should be painted using a
 * single style. It knows which style to use, what range of characters in the
 * model this segment represents, what characters will actually be displayed
 * in the view (which need not be the same), how to paint this segment, and
 * how to convert between the model and view coordinate spaces.
 * 
 * @author Phil Norman
 */
public interface PLineSegment {
    /** Returns the style to be used when painting this text. */
    public PStyle getStyle();
    
    /**
     * Returns the model CharSequence represented by this segment. Contrast
     * with getViewText, which what will be rendered, rather than what the
     * rendered text represents. 
     */
    public CharSequence getCharSequence();
    
    /**
     * Returns the text to be drawn. This is overridden by
     * UnprintableCharacterTextSegment to show control characters in
     * their escaped form.
     */
    public String getViewText();
    
    public PLineSegment subSegment(int start);
    
    public PLineSegment subSegment(int start, int end);
    
    /** Returns the text offset of the start of this segment. */
    public int getOffset();
    
    /** Returns the text offset of the character just after the end of this segment. */
    public int getEnd();
    
    /**
     * Returns the number of characters in the model that this segment
     * represents. This may or may not correspond to the number of characters
     * in the view.
     */
    public int getModelTextLength();
    
    /** Returns true if this segment represents any line break, be it caused by line wrap or a newline character. */
    public boolean isNewline();
    
    /** Returns true only if this segment represents a hard newline (one representing a newline character). */
    public boolean isHardNewline();
    
    /** Returns the width of this text in pixels, when painted from the given X position in pixels. */
    public int getDisplayWidth(FontMetrics metrics, int startX);
    
    /**
     * Returns the width between the start of this segment, and the given character offset within
     * the segment, when painted from the given X position, measured in pixels.
     */
    public int getDisplayWidth(FontMetrics metrics, int startX, int charOffset);
    
    /**
     * Returns the character offset from the start of this segment which is nearest to the given
     * 'x' position when this segment is drawn from the given startX position, measured in pixels.
     */
    public int getCharOffset(FontMetrics metrics, int startX, int x);
    
    /**
     * Paints the text into the given location.
     */
    public void paint(Graphics2D g, int x, int yBaseline);
}
