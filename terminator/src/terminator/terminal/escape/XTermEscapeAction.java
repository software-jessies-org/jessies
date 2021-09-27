package terminator.terminal.escape;

import e.util.*;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.*;
import java.util.Base64;

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
     * 
     * libvte supports arbitrary hyperlinks.
     * See https://gist.github.com/egmontkob/eb114294efbcd5adb1944c9f3cb5feda.
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
                case 52:
                    if (!Terminator.getPreferences().getBoolean(TerminatorPreferences.ENABLE_TERMINAL_CLIPBOARD_ACCESS)) {
                        Log.warn("XTerm escape sequence OSC " + ps + " is disabled by preference " + TerminatorPreferences.ENABLE_TERMINAL_CLIPBOARD_ACCESS + ".");
                        return;
                    }
                    String[] parts = pt.split(";", 3);
                    if (parts.length != 2) {
                        break;
                    }
                    // [cpqs0-7]* controls which clipboard/selection we should use.
                    String pc = parts[0];
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    String clipboardName = "c";
                    if (pc.equals("p")) {
                        clipboard = Toolkit.getDefaultToolkit().getSystemSelection();
                        clipboardName = "p";
                    }
                    String pd = parts[1];
                    if (pd.equals("?")) {
                        Object unusedRequestor = this;
                        Transferable transferable = clipboard.getContents(unusedRequestor);
                        try {
                            String string = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                            String base64 = Base64.getEncoder().encodeToString(string.getBytes(TerminalControl.CHARSET_NAME));
                            control.sendUtf8String(Ascii.ESC + "[52;" + clipboardName + ";" + base64 + Ascii.BEL);
                        } catch (Exception ex) {
                            Log.warn("Couldn't get clipboard contents.", ex);
                        }
                    } else {
                        try {
                            String contents = new String(Base64.getDecoder().decode(pd.getBytes(TerminalControl.CHARSET_NAME)));
                            StringSelection selection = new StringSelection(contents);
                            clipboard.setContents(selection, selection);
                        } catch (Exception ex) {
                            Log.warn("Couldn't set clipboard contents.", ex);
                        }
                    }
                    return;
            }
        }
        Log.warn("Unsupported XTerm escape sequence \"" + StringUtilities.escapeForJava(sequence) + "\".");
    }
    
    @Override public String toString() {
        return "XTermEscapeAction[" + StringUtilities.escapeForJava(sequence) + "]";
    }
}
