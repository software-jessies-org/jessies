package terminator.terminal.escape;

import e.util.*;
import java.awt.*;
import java.util.*;
import terminator.*;
import terminator.model.*;
import terminator.terminal.*;

/**
Parses 'CSI' escape sequences.  Such sequences always have '[' as their first character,
and then are sometimes followed by a '?' character, then optionally a list of numbers
separated by ';' characters, followed by the final character which tells us what to do with
all that stuff.

https://invisible-island.net/xterm/ctlseqs/ctlseqs.html#h2-Functions-using-CSI-_-ordered-by-the-final-character_s_

@author Phil Norman
*/

public class CSIEscapeAction implements TerminalAction {
    private TerminalControl control;
    private String sequence;
    
    public CSIEscapeAction(TerminalControl control, String sequence) {
        this.control = control;
        this.sequence = sequence;
    }

    public void perform(TerminalModel model) {
        if (processSequence(model) == false) {
            Log.warn("Unimplemented escape sequence: \"" + StringUtilities.escapeForJava(sequence) + "\"");
        }
    }
    
    private String getSequenceType(char lastChar) {
        switch (lastChar) {
        case 'A': return "Cursor up";
        case 'B': return "Cursor down";
        case 'C': return "Cursor right";
        case 'c': return "Device attributes request";
        case 'D': return "Cursor left";
        case 'd': return "Move cursor to row";
        case 'E': return "Cursor next line";
        case 'F': return "Cursor preceding line";
        case 'G':
        case '`': return "Move cursor column to";
        case 'f':
        case 'H': return "Move cursor to";
        case 'K': return "Kill line contents";
        case 'J': return "Kill lines";
        case 'L': return "Insert lines";
        case 'M': return "Delete lines";
        case 'P': return "Delete characters";
        case 'g': return "Clear tabs";
        case 'h': return "Set DEC private mode";
        case 'l': return "Clear DEC private mode";
        case 'm': return "Set font, color, etc";
        case 'n': return "Device status report";
        case 'r': return "Restore DEC private modes or set scrolling region";
        case 's': return "Save DEC private modes";
        default: return "Unknown:" + lastChar;
        }
    }
    
    @Override public String toString() {
        char lastChar = sequence.charAt(sequence.length() - 1);
        return "CSIEscapeAction[" + getSequenceType(lastChar) + "]";
    }
    
    private boolean processSequence(TerminalModel model) {
        char lastChar = sequence.charAt(sequence.length() - 1);
        String midSequence = sequence.substring(1, sequence.length() - 1);
        switch (lastChar) {
        case 'A':
            return moveCursor(model, midSequence, 0, -1);
        case 'B':
            return moveCursor(model, midSequence, 0, 1);
        case 'C':
            return moveCursor(model, midSequence, 1, 0);
        case 'c':
            return deviceAttributesRequest(midSequence);
        case 'D':
            return moveCursor(model, midSequence, -1, 0);
        case 'd':
            return moveCursorRowTo(model, midSequence);
        case 'E':
        case 'F':
            return movePrevNextLine(model, midSequence, lastChar);
        case 'G':
        case '`':
            return moveCursorColumnTo(model, midSequence);
        case 'f':
        case 'H':
            return moveCursorTo(model, midSequence);
        case 'K':
            return killLineContents(model, midSequence);
        case 'J':
            return eraseInPage(model, midSequence);
        case 'L':
            return insertLines(model, midSequence);
        case 'M':
            return deleteLines(model, midSequence);
        case 'P':
            return deleteCharacters(model, midSequence);
        case 'S':
            return scrollRegion(model, midSequence, true);
        case 'T':
            return scrollRegion(model, midSequence, false);
        case 'g':
            return clearTabs(model, midSequence);
        case 'h':
            return setDecPrivateMode(model, midSequence, true);
        case 'l':
            return setDecPrivateMode(model, midSequence, false);
        case 'm':
            return processFontEscape(model, midSequence);
        case 'n':
            return processDeviceStatusReport(model, midSequence);
        case 'p':
            if (midSequence.equals("!")) {
                control.reset();
                return true;
            }
            break;
        case 'q':
            if (sequence.charAt(sequence.length() - 2) == ' ') {
                int style = Integer.parseInt(sequence.substring(1, sequence.length() - 2));
                model.setCursorStyle(style);
                return true;
            } else {
                Log.warn("unknown CSI q sequence " + StringUtilities.escapeForJava(sequence));
            }
            break;
        case 'r':
            if (midSequence.startsWith("?")) {
                return restoreDecPrivateModes(midSequence);
            } else {
                return setScrollingRegion(model, midSequence);
            }
        case 's':
            return saveDecPrivateModes(midSequence);
        }
        Log.warn("unknown CSI sequence " + StringUtilities.escapeForJava(sequence));
        return false;
    }
    
    private boolean clearTabs(TerminalModel model, String seq) {
        int clearType = seq.isEmpty() ? 0 : Integer.parseInt(seq);
        if (clearType == 0) {
            // Clear horizontal tab at current cursor position.
            model.removeTabAtCursor();
            return true;
        } else if (clearType == 3) {
            // Clear all horizontal tabs.
            model.removeAllTabs();
            return true;
        } else {
            Log.warn("Unknown clear tabs type: " + clearType);
            return false;
        }
    }
    
    private boolean deleteLines(TerminalModel model, String seq) {
        int count = seq.isEmpty() ? 1 : Integer.parseInt(seq);
        for (int i = 0; i < count; i++) {
            model.deleteLine();
        }
        return true;
    }
    
    private boolean insertLines(TerminalModel model, String seq) {
        int count = seq.isEmpty() ? 1 : Integer.parseInt(seq);
        model.insertLines(count);
        return true;
    }
    
    private boolean scrollRegion(TerminalModel model, String seq, boolean up) {
        int count = seq.isEmpty() ? 1 : Integer.parseInt(seq);
        model.scrollRegion(count, up);
        return true;
    }
    
    private boolean setDecPrivateMode(TerminalModel model, String seq, boolean value) {
        boolean isPrivateMode = seq.startsWith("?");
        String[] modes = (isPrivateMode ? seq.substring(1) : seq).split(";");
        for (String modeString : modes) {
            int mode = Integer.parseInt(modeString);
            if (isPrivateMode) {
                switch (mode) {
                case 3:
                    Log.warn("Unsupported private mode: 80 Column Mode (DECCOLM)");
                    break;
                case 4:
                    Log.warn("Unsupported private mode: Jump (Fast) Scroll (DECSCLM)");
                    break;
                case 25:
                    model.setCursorVisible(value);
                    break;
                case 47:
                    model.useAlternateBuffer(value);
                    break;
                case 1000:
                    // Old-style (broken for large windows) xterm mouse tracking.
                    // Unfortunately, xterm ignores 1006 unless you send 1000 ---
                    // even though vim doesn't think that should be necessary.
                    // Let's not support the broken 1000 format, but making 1000
                    // a no-op at least means no warnings in the logs about it.
                    break;
                case 1002:
                    model.setCellMotionMouseTracking(value);
                    break;
                case 1006:
                    model.setMouseTracking(value);
                    break;
                case 1047:
                    model.useAlternateBuffer(value);
                    break;
                case 1048:
                    if (value) {
                        model.saveCursor();
                    } else {
                        model.restoreCursor();
                    }
                    break;
                case 1049:
                    if (value) {
                        model.saveCursor();
                        model.useAlternateBuffer(value);
                    } else {
                        model.useAlternateBuffer(value);
                        model.restoreCursor();                        
                    }
                    break;
                case 2004:
                    model.setBracketedPasteMode(value);
                    break;
                default:
                    Log.warn("Unknown private mode " + mode + " in [" + StringUtilities.escapeForJava(seq) + (value ? 'h' : 'l'));
                }
            } else {
                switch (mode) {
                case 4:
                    model.setInsertMode(value);
                    break;
                case 20:
                    control.setAutomaticNewline(value);
                    break;
                default:
                    Log.warn("Unknown mode " + mode + " in [" + StringUtilities.escapeForJava(seq) + (value ? 'h' : 'l'));
                }
            }
        }
        return true;
    }
    
    private boolean restoreDecPrivateModes(String seq) {
        Log.warn("Restore DEC private mode values not implemented (CSI " + StringUtilities.escapeForJava(seq) + ")");
        return false;
    }
    
    private boolean saveDecPrivateModes(String seq) {
        Log.warn("Save DEC private mode values not implemented (CSI " + StringUtilities.escapeForJava(seq) + ")");
        return false;
    }
    
    private boolean setScrollingRegion(TerminalModel model, String seq) {
        int index = seq.indexOf(';');
        if (index == -1) {
            model.setScrollingRegion(-1, -1);
        } else {
            model.setScrollingRegion(Integer.parseInt(seq.substring(0, index)), Integer.parseInt(seq.substring(index + 1)));
        }
        return true;
    }
    
    private boolean deviceAttributesRequest(String seq) {
        if (seq.equals("") || seq.equals("0")) {
            sendDeviceAttributes(control);
            return true;
        } else {
            return false;
        }
    }
    
    public static void sendDeviceAttributes(TerminalControl control) {
        control.sendUtf8String(Ascii.ESC + "[?1;0c");
    }
    
    private boolean deleteCharacters(TerminalModel model, String seq) {
        int count = seq.isEmpty() ? 1 : Integer.parseInt(seq);
        model.deleteCharacters(count);
        return true;
    }
    
    private boolean killLineContents(TerminalModel model, String seq) {
        int type = seq.isEmpty() ? 0 : Integer.parseInt(seq);
        boolean fromStart = (type >= 1);
        boolean toEnd = (type != 1);
        model.killHorizontally(fromStart, toEnd);
        return true;
    }
    
    private boolean eraseInPage(TerminalModel model, String seq) {
        int type = seq.isEmpty() ? 0 : Integer.parseInt(seq);
        boolean fromTop = (type >= 1);
        boolean toBottom = (type != 1);
        model.eraseInPage(fromTop, toBottom);
        return true;
    }
    
    private boolean movePrevNextLine(TerminalModel model, String seq, char eOrF) {
        int count = seq.isEmpty() ? 1 : Integer.parseInt(seq);
        int line = model.getCursorPosition().getLineIndex();
        line += ((eOrF == 'E') ? 1 : -1) * count;
        model.setCursorPosition(1, line + 1);
        return true;
    }
    
    private boolean moveCursorRowTo(TerminalModel model, String seq) {
        model.setCursorPosition(-1, Integer.parseInt(seq));
        return true;
    }
    
    private boolean moveCursorColumnTo(TerminalModel model, String seq) {
        model.setCursorPosition(Integer.parseInt(seq), -1);
        return true;
    }
    
    private boolean moveCursorTo(TerminalModel model, String seq) {
        int x = 1;
        int y = 1;
        int splitIndex = seq.indexOf(';');
        if (splitIndex != -1) {
            y = Integer.parseInt(seq.substring(0, splitIndex));
            x = Integer.parseInt(seq.substring(splitIndex + 1));
        }
        model.setCursorPosition(x, y);
        return true;
    }
    
    private boolean moveCursor(TerminalModel model, String countString, int xDirection, int yDirection) {
        int count = countString.isEmpty() ? 1 : Integer.parseInt(countString);
        if (xDirection != 0) {
            model.moveCursorHorizontally(xDirection * count);
        }
        if (yDirection != 0) {
            model.moveCursorVertically(yDirection * count);
        }
        return true;
    }
    
    private boolean processDeviceStatusReport(TerminalModel model, String sequence) {
        switch (Integer.parseInt(sequence)) {
        case 5:
            control.sendUtf8String(Ascii.ESC + "[0n");
            return true;
        case 6:
            Location location = model.getCursorPosition();
            int row = location.getLineIndex() - model.getFirstDisplayLine() + 1;
            int column = location.getCharOffset() + 1;
            control.sendUtf8String(Ascii.ESC + "[" + row + ";" + column + "R");
            return true;
        default:
            return false;
        }
    }
    
    // Returns the next chunk from 'chunks' as an int.
    // If the next chunk is empty, returns 0.
    // If there are no more chunks, returns 0.
    private int nextInt(Iterator<String> chunks) {
        final String chunk = chunks.hasNext() ? chunks.next() : "";
        return chunk.isEmpty() ? 0 : Integer.parseInt(chunk);
    }
    
    private boolean processFontEscape(TerminalModel model, String sequence) {
        Style oldStyle = model.getStyle();
        // Grab the *raw* foreground and background colours. This preserves their 'null'ness in the
        // case that a style has no explicit fg/bg, and must use the current default. This is important
        // for when someone changes the fg/bg colours in the preferences panel.
        Palettes.Ink foreground = oldStyle.getRawForeground();
        Palettes.Ink background = oldStyle.getRawBackground();
        int attributes = oldStyle.getAttributes();
        Iterator<String> chunks = Arrays.asList(sequence.split(";")).iterator();
        while (chunks.hasNext()) {
            final int attribute = nextInt(chunks);
            switch (attribute) {
            case 0:
                // Clear all attributes.
                foreground = null;
                background = null;
                attributes = 0;
                break;
            case 1: attributes |= Style.BOLD; break;
            case 2: attributes |= Style.DIM; break;
            case 3: attributes |= Style.ITALIC; break;
            case 4: attributes |= Style.UNDERLINE; break;
            case 5: attributes |= Style.BLINK; break;
            case 7: attributes |= Style.REVERSE; break;
            case 8: attributes |= Style.HIDDEN; break;
            case 9: attributes |= Style.STRIKETHROUGH; break;
            case 21:
                // The xwsh man page suggests this should disable bold.
                // ECMA-048 says it turns on double-underlining.
                // xterm does nothing.
                // libvte 0.52 implements this, along with double and wavy variants of CSI 4 m:
                // https://askubuntu.com/questions/528928/how-to-do-underline-bold-italic-strikethrough-color-background-and-size-i
                attributes &= ~(Style.BOLD | Style.DIM);
                break;
            case 22:
                // Normal: neither BOLD nor DIM.
                attributes &= ~(Style.BOLD | Style.DIM);
                break;
            case 23: attributes &= ~Style.ITALIC; break;
            case 24: attributes &= ~Style.UNDERLINE; break;
            case 25: attributes &= ~Style.BLINK; break;
            case 27: attributes &= ~Style.REVERSE; break;
            case 28: attributes &= ~Style.HIDDEN; break;
            case 29: attributes &= ~Style.STRIKETHROUGH; break;
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37:
                // Set foreground color to one of the original eight colors.
                foreground = Palettes.getInk(attribute - 30);
                break;
            case 38:
                // Set foreground color (256-color or 24-bit).
            case 48:
                // Set background color (256-color or 24-bit).
                Palettes.Ink newColor = null;
                final int colorMode = nextInt(chunks);
                switch (colorMode) {
                case 5:
                    // 256 color mode, as in xterm.
                    newColor = Palettes.getInk(nextInt(chunks));
                    break;
                case 2:
                    // 24 bit color mode, a konsole extension.
                    final int red = nextInt(chunks);
                    final int green = nextInt(chunks);
                    final int blue = nextInt(chunks);
                    newColor = Palettes.getFixedInk(new Color(red, green, blue));
                    break;
                default:
                    Log.warn("Unknown color mode " + colorMode + " for attribute " + attribute + " in [" + StringUtilities.escapeForJava(sequence));
                }
                if (attribute == 38) {
                    foreground = newColor;
                } else {
                    background = newColor;
                }
                break;
            case 39:
                // Use default foreground color.
                foreground = null;
                break;
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47:
                // Set background color to one of the original eight colors.
                background = Palettes.getInk(attribute - 40);
                break;
            case 49:
                // Use default background color.
                background = null;
                break;
            case 90:
            case 91:
            case 92:
            case 93:
            case 94:
            case 95:
            case 96:
            case 97:
                // Set foreground color to one of the eight bright colors.
                foreground = Palettes.getInk(attribute - 82);
                break;
            case 100:
            case 101:
            case 102:
            case 103:
            case 104:
            case 105:
            case 106:
            case 107:
                // Set background color to one of the eight bright colors.
                background = Palettes.getInk(attribute - 92);
                break;
            default:
                Log.warn("Unknown attribute " + attribute + " in [" + StringUtilities.escapeForJava(sequence));
                break;
            }
        }
        model.setStyle(Style.makeStyle(foreground, background, attributes));
        return true;
    }
}
