package terminator.terminal.escape;

import e.util.*;
import java.awt.Color;
import terminator.*;
import terminator.model.*;
import terminator.terminal.*;

/**
 * An XTermEscapeAction performs the action associated with an XTerm OSC (Operating System Command) escape sequence.
 * XTerm escape sequences always start with a ']' character, followed by a number.
 * An optional ';' follows, marking the start of a BEL-terminated string.
 * We strip off the initial ']' and the BEL terminator, if present, in the constructor, since they contain no information.
 * FIXME: this is probably a mistake if we're planning on outputting the sequence in toString, or if we ever support other OSCs.
 */
public class XTermEscapeAction implements TerminalAction {
    private TerminalControl control;
    private String sequence;
    
    public XTermEscapeAction(TerminalControl control, String sequence) {
        this.control = control;
        
        // Trim off the initial ']', and the terminating BEL character (if present).
        if (sequence.charAt(sequence.length() - 1) == '\007') {
            this.sequence = sequence.substring(1, sequence.length() - 1);
        } else {
            this.sequence = sequence.substring(1);
        }
    }

    /**
     * Handles the special escape sequence from xterm, called OSC by ECMA.
     * https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h2-Operating-System-Commands
     *
     * XTerm escape sequences: ESC ] Ps;Pt BEL
     *       0 = change icon name and window title
     *       2 = change window title
     *      10 = get/set text foreground color
     *      11 = get/set text background color
     */
    public void perform(TerminalModel model) {
        int index = sequence.indexOf(';');
        if (index != -1) {
            int ps = Integer.parseInt(sequence.substring(0, index));
            String pt = sequence.substring(index + 1);
            switch (ps) {
                case 0:
                case 2:
                    model.setWindowTitle(pt);
                    return;
                case 10:
                case 11:
                    // We currently only support getting the colors.
                    if (pt.equals("?")) {
                        String key = ps == 10 ? TerminatorPreferences.FOREGROUND_COLOR : TerminatorPreferences.BACKGROUND_COLOR;
                        Color c = Terminator.getPreferences().getColor(key);
                        String r = Integer.toHexString(c.getRed());
                        String g = Integer.toHexString(c.getGreen());
                        String b = Integer.toHexString(c.getBlue());
                        // Experimentation with xterm suggests that it just duplicates the "true" 8 bits into the top 8 bits.
                        control.sendUtf8String(Ascii.ESC + "]" + ps + ";rgb:" + r + r + "/" + g + g + "/" + b + b + Ascii.BEL);
                        return;
                    }
                    break;
            }
        }
        Log.warn("Unsupported XTerm escape sequence \"" + StringUtilities.escapeForJava(sequence) + "\".");
    }
    
    @Override public String toString() {
        return "XTermEscapeAction[" + StringUtilities.escapeForJava(sequence) + "]";
    }
}
