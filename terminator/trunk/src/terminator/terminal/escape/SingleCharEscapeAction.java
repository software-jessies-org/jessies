package terminator.terminal.escape;

import e.util.*;
import terminator.model.*;
import terminator.terminal.*;

/**
 * Recognizes escape sequences consisting of ASCII ESC followed by a single character.
 * Note that most of these are mainly of historical interest, even though some of them look similar to more common sequences.
 */
public class SingleCharEscapeAction implements TerminalAction {
    private TerminalControl control;
    private char escChar;
    
    public SingleCharEscapeAction(TerminalControl control, char escChar) {
        this.control = control;
        this.escChar = escChar;
    }

    public void perform(TerminalModel model) {
        switch (escChar) {
            case '6':  // rxvt: scr_backindex
                unsupported("scr_backindex");
                break;
            case '7':  // Save cursor.  rxvt saves position, current style, and charset.
                model.saveCursor();
                break;
            case '8':  // Restore cursor.
                model.restoreCursor();
                break;
            case '9':  // rxvt: scr_forwardindex
                unsupported("scr_forwardindex");
                break;
            case '=':  // rxvt: set private mode PrivMode_aplKP (application keypad).
                unsupported("set private mode PrivMode_aplKP (application keypad).");
                break;
            case '>':  // rxvt: unset private mode PrivMode_aplKP (application keypad).
                unsupported("unset private mode PrivMode_aplKP (application keypad).");
                break;
            case 'D':  // Move the cursor down one line, scrolling if it reaches the bottom of scroll region.  Effectively NL.
                model.processSpecialCharacter('\n');
                break;
            case 'E':  // Move cursor to start of next line, scrolling if required.  Effectively CR,NL
                model.processSpecialCharacter('\r');
                model.processSpecialCharacter('\n');
                break;
            case 'H':  // rxvt: scr_set_tab(1)  Set a horizontal tab marker at the current cursor position.
                model.setTabAtCursor();
                break;
            case 'M':  // Move cursor up one line, scrolling if it reaches the top of scroll region.  Opposite of NL.
                model.scrollDisplayUp();
                break;
            case 'Z':
                // An obsolete form of ESC [ c (send device attributes).
                CSIEscapeAction.sendDeviceAttributes(control);
                break;
            case 'c':  // Power on (full reset).
                control.reset();
                model.fullReset();
                break;
                
            // Change character set.
            // Note that these are different to the related ^N and ^O sequences, which select character sets 1 and 0 and are handled elsewhere.
            // These sequences ("^[n" and "^[o") are even less common than their relatives.
            case 'n':
                control.invokeCharacterSet(2);
                break;
            case 'o':
                control.invokeCharacterSet(3);
                break;
                
            case '|':
            case '}':
            case '~':
                // Invoke the G3, G2, and G1 character sets as
                // GR. Has no visible effect.
                break;
            default:
                Log.warn("Unrecognized single-character escape \"" + escChar + "\".");
        }
    }
    
    private String getType() {
        switch (escChar) {
        case '6': return "rxvt: scr_backindex (not supported)";
        case '7': return "Save cursor";
        case '8': return "Restore cursor";
        case '9': return "rxvt: scr_forwardindex (not supported)";
        case '=': return "Set private mode application keypad";
        case '>': return "Unset private mode application keypad";
        case 'D': return "Down one line";
        case 'E': return "Move to start of next line";
        case 'H': return "Set tab at cursor";
        case 'M': return "Cursor up one line";
        case 'Z': return "Send device attributes (obsolete)";
        case 'c': return "Full reset";
        case 'n': return "Invoke character set 2";
        case 'o': return "Invoke character set 3";
        case '|':
        case '}':
        case '~': return "Invoke G3, G2, G1 character sets as GR";
        default: return "Unrecognized:" + escChar;
        }
    }
    
    @Override public String toString() {
        return "SingleCharEscapeAction[" + getType() + "]";
    }
    
    private void unsupported(String description) {
        Log.warn("Unsupported single-character escape \"" + escChar + "\" (" + description + ").");
    }
}
