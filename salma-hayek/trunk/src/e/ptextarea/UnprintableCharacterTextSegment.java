package e.ptextarea;

import e.gui.*;
import java.awt.*;
import java.awt.event.*;

public class UnprintableCharacterTextSegment extends PTextSegment {
    public UnprintableCharacterTextSegment(PTextArea textArea, int start, int end, PStyle style) {
        super(textArea, start, end, style);
    }
    
    @Override
    public PLineSegment subSegment(int start, int end) {
        return new UnprintableCharacterTextSegment(textArea, start + this.start, end + this.start, style);
    }
    
    @Override
    public String getText() {
        String unprintableCharacters = super.getText();
        return e.util.StringUtilities.escapeForJava(unprintableCharacters);
    }
    
    @Override
    public String toString() {
        return "UnprintableCharacterSegment[" + super.toString() + "]";
    }
}
