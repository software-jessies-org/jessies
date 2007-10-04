package e.ptextarea;

import java.awt.*;

public class UnprintableCharacterTextSegment extends PTextSegment {
    public UnprintableCharacterTextSegment(PTextArea textArea, int start, int end, PStyle style) {
        super(textArea, start, end, style);
    }
    
    @Override
    public PLineSegment subSegment(int start, int end) {
        return new UnprintableCharacterTextSegment(textArea, start + this.start, end + this.start, style);
    }
    
    /**
     * Escapes the control characters represented by this segment.
     */
    @Override
    public String getViewText() {
        String unprintableCharacters = super.getViewText();
        return e.util.StringUtilities.escapeForJava(unprintableCharacters);
    }
    
    /**
     * Returns a whole number of escaped control characters. If you're more
     * than half-way through an escape, you get the whole escaped character.
     * This relies on all escapes being like "\u0000" (specifically, six
     * characters in length).
     */
    @Override
    public int getCharOffset(FontMetrics metrics, int startX, int x) {
        final int charOffset = super.getCharOffset(metrics, startX, x);
        final int extra = (charOffset % 6 >= 3) ? 1 : 0;
        return (charOffset / 6) + extra;
    }
    
    @Override
    public String toString() {
        return "UnprintableCharacterSegment[" + super.toString() + "]";
    }
}
