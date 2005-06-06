package e.ptextarea;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;

public class UnprintableCharacterTextSegment extends PTextSegment {
    public UnprintableCharacterTextSegment(PTextArea textArea, int start, int end, PStyle style) {
        super(textArea, start, end, style);
    }
    
    public PLineSegment subSegment(int start, int end) {
        return new UnprintableCharacterTextSegment(textArea, start + this.start, end + this.start, style);
    }
    
    public String getText() {
        String unprintableCharacters = super.getText();
        return e.util.StringUtilities.escapeForJava(unprintableCharacters);
    }
    
    public String toString() {
        return "UnprintableCharacterSegment[" + super.toString() + "]";
    }
}
