package terminator.terminal.escape;

import e.util.*;
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
    private String sequence;
    
    public XTermEscapeAction(String sequence) {
        // Trim off the initial ']', and the terminating BEL character (if present).
        if (sequence.charAt(sequence.length() - 1) == '\007') {
            this.sequence = sequence.substring(1, sequence.length() - 1);
        } else {
            this.sequence = sequence.substring(1);
        }
    }

    /**
     * Handles the special escape sequence from xterm, called OSC by ECMA.
     * From rxvt:
     *
     * XTerm escape sequences: ESC ] Ps;Pt BEL
     *       0 = change iconName/title
     *       1 = change iconName
     *       2 = change title
     *      46 = change log file (not implemented)
     *      50 = change font
     *
     * rxvt extensions:
     *      10 = menu
     *      20 = bg pixmap
     *      39 = change default fg color
     *      49 = change default bg color
     */
    public void perform(TerminalModel model) {
        if (isNewWindowTitle()) {
            model.setWindowTitle(getNewWindowTitle());
        } else {
            Log.warn("Unsupported XTerm escape sequence \"" + StringUtilities.escapeForJava(sequence) + "\".");
        }
    }
    
    private boolean isNewWindowTitle() {
        return (sequence.startsWith("2;") || sequence.startsWith("0;"));
    }
    
    private String getNewWindowTitle() {
        return sequence.substring(2);
    }
    
    @Override public String toString() {
        if (isNewWindowTitle()) {
            return "XTermEscapeAction[New window title:\"" + StringUtilities.escapeForJava(getNewWindowTitle()) + "\"]";
        } else {
            return "XTermEscapeAction[Unsupported:" + StringUtilities.escapeForJava(sequence) + "]";
        }
    }
}
