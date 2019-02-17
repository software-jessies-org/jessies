package terminator.model;

import java.util.*;
import terminator.Palettes;

/**
 * Ties together the String containing the characters on a particular line, and the styles to be applied to each character.
 * TextLines are mutable, though it's not possible to change style information without rewriting the corresponding characters (because that's not how terminals work).
 * Actually documentation says that the VT400 has some, but by default support is not compiled into xterm.
 * #define OPT_DEC_RECTOPS 1
 * enables CSI Pt;Pl;Pb;Pr;Ps $[rt] (as well as some other rectangle stuff).
 */
public class TextLine {
    // The text we store internally contains information about tabs.
    // When text is passed back out to the outside world, we either convert the tab information to spaces (for the display), or to tab characters (for the clipboard).
    // Internally, a tab is marked as beginning with TAB_START.
    // Each following display position (assuming *all* characters are the same width) covered by the tab is denoted by TAB_CONTINUE.
    // We have to internally store all this tab position and length information because tab positions can change in the outside world at any time, but each TextLine must retain its integrity once the tabs have been inserted into it.
    private static final char TAB_START = '\t';
    private static final char TAB_CONTINUE = '\r';
    
    // The default background, used beyond the last character of the line.
    private Palettes.Ink background;
    // The index in characters into the containing buffer of the first character of this line.
    private int lineStartIndex;
    
    // The characters on this line.
    // An immutable String may seem like an odd choice, but we've tried StringBuilder too.
    // In terms of space, StringBuilder helps a little, saving on useless String fields (such as the cached hashCode), but we pay extra for each blank line (where the cost is a whole new StringBuilder rather than just sharing the JVM's single empty-string instance), and we pay for unused space in the underlying char[]s.
    // In terms of time, StringBuilder hurts a little, because we need to convert to a String for our callers (especially rendering), and young lines don't change much and old lines never change.
    // In terms of code, there's nothing in it; the StringBuilder delete and insert methods are arguably more readable, but that only affects a handful of lines.
    // All in all, then, String is actually the best choice in our current environment.
    // (If we switched rendering over to AttributedCharacterIterator or something else that didn't require a String, that might change the balance.)
    private String text;
    
    // The styles to be applied to the characters on this line.
    // styles == null => all characters use the default style.
    // Otherwise, styles.length == text.length(), and the style information for text.charAt(i) is styles[i] (never null).
    private Style[] styles;
    
    public TextLine(Palettes.Ink bg) {
        background = bg;
        clear();
    }
    
    public Palettes.Ink getBackground() {
        return background == null ? Palettes.getBackgroundInk() : background;
    }
    
    public void setBackground(Palettes.Ink bg) {
        background = bg;
    }
    
    public int getLineStartIndex() {
        return lineStartIndex;
    }
    
    public void setLineStartIndex(int lineStartIndex) {
        this.lineStartIndex = lineStartIndex;
    }
    
    public Style getStyleAt(int index) {
        return (styles == null) ? Style.getDefaultStyle() : styles[index];
    }
    
    /**
     * Return the first index with a different starting style, given that external style changes at end.
     * ('end' is where the next/current find/url highlight starts or ends, or the end of the text.)
     * Instead of iterating over getStyledTextSegments(), use code like:
     * 
     * for (int start = 0, end = length(); start < end; start = done) {
     *     int done = getRunLimit(start, end);
     *     Style style = getStyleAt(start);
     *     somehowDrawText(getSubstring(start, done), style);
     * }
     */
    public int getRunLimit(int start, int end) {
        if (start < 0 || start >= end || end > length()) {
            throw new AssertionError("start=" + start + " end=" + end + " length()=" + length());
        }
        // If we have no styling, only caller can affect styling of a run.
        if (styles == null) {
            return end;
        }
        Style toMatch = styles[start];
        for (int i = start + 1; i < end; i++) {
            if (!toMatch.equals(styles[i])) {
                return i;
            }
        }
        return end;
    }
    
    /**
     * Returns the text of this line with spaces instead of tabs (or, indeed, instead of the special representation we use internally).
     * 
     * This isn't called toString because you need to come here and think about whether you want this method or getTabbedString instead.
     */
    public String getString() {
        return text.replace(TAB_START, ' ').replace(TAB_CONTINUE, ' ');
    }
    
    public String getSubstring(int beginIndex, int endIndex) {
        return getString().substring(beginIndex, endIndex);
    }

    /** Returns the text, with all the tabs put back in for use with clipboard stuff. */
    public String getTabbedString(int start, int end) {
        StringBuilder buf = new StringBuilder();
        for (int i = start; i < end; i++) {
            char ch = text.charAt(i);
            if (ch != TAB_CONTINUE) {
                buf.append(ch);
            }
        }
        return buf.toString();
    }
    
    public int length() {
        return text.length();
    }
    
    public int lengthIncludingNewline() {
        return length() + 1;
    }
    
    /**
    * Returns the offset of the character specified by charOffset.
    * The returned value will be charOffset for most characters, but may
    * be smaller if the character at charOffset is part of a tab.
    */
    public int getEffectiveCharStartOffset(int charOffset) {
        if (charOffset >= text.length()) {
            return charOffset;
        }
        for (int i = charOffset; i >= 0; i--) {
            if (text.charAt(i) != TAB_CONTINUE) {
                return i;
            }
        }
        return 0;
    }
    
    /**
    * Returns the offset of the character after that specified by charOffset.
    * The returned value will be charOffset + 1 for most characters, but may
    * be larger if the character at charOffset is part of a tab (after the start).
    */
    public int getEffectiveCharEndOffset(int charOffset) {
        if (charOffset >= text.length()) {
            return charOffset;
        }
        for (int i = charOffset; i < text.length(); i++) {
            if (text.charAt(i) != TAB_CONTINUE) {
                return i;
            }
        }
        return text.length();
    }
    
    public void clear() {
        text = "";
        styles = null;
    }
    
    public void killText(int startIndex, int endIndex) {
        if (startIndex >= endIndex || startIndex >= text.length()) {
            return;
        }
        endIndex = Math.min(endIndex, text.length());
        text = text.substring(0, startIndex) + text.substring(endIndex);
        removeStyleData(startIndex, endIndex);
    }
    
    public void insertTabAt(int offset, int tabLength, Style style) {
        insertTextAt(offset, getTabString(tabLength), style);
    }
    
    private static String getTabString(int tabLength) {
        char[] tab = new char[tabLength];
        tab[0] = TAB_START;
        Arrays.fill(tab, 1, tab.length, TAB_CONTINUE);
        return new String(tab);
    }
    
    /** Inserts text at the given position, moving anything already there further to the right. */
    public void insertTextAt(int offset, String newText, Style style) {
        ensureOffsetIsOK(offset);
        text = text.substring(0, offset) + newText + text.substring(offset);
        insertStyleData(offset, newText.length(), style);
    }
    
    /** Writes text at the given position, overwriting anything underneath. */
    public void writeTextAt(int offset, String newText, Style style) {
        ensureOffsetIsOK(offset);
        if (offset + newText.length() < text.length()) {
            text = text.substring(0, offset) + newText + text.substring(offset + newText.length());
        } else {
            text = text.substring(0, offset) + newText;
        }
        overwriteStyleData(offset, newText.length(), style);
    }
    
    private void ensureOffsetIsOK(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("Negative offset " + offset);
        }
        if (offset > text.length()) {
            appendPadding(offset - text.length());
        }
    }
    
    private void appendPadding(int count) {
        char[] pad = new char[count];
        Arrays.fill(pad, ' ');
        int oldTextLength = text.length();
        text += new String(pad);
        // Even an empty line can have a background color; make sure we use it.
        insertStyleData(oldTextLength, count, Style.makeStyle(null, background, 0));
    }
    
    private void overwriteStyleData(int offset, int count, Style value) {
        if (styles == null && value.equals(Style.getDefaultStyle())) {
            return;
        }
        Style[] oldStyleData = maybeResizeStyleData();
        if (oldStyleData != null) {
            System.arraycopy(oldStyleData, 0, styles, 0, oldStyleData.length);
        }
        Arrays.fill(styles, offset, offset + count, value);
    }
    
    private void insertStyleData(int offset, int count, Style value) {
        if (styles == null && value.equals(Style.getDefaultStyle())) {
            return;
        }
        Style[] oldStyleData = maybeResizeStyleData();
        if (oldStyleData != null) {
            System.arraycopy(oldStyleData, 0, styles, 0, offset);
            System.arraycopy(oldStyleData, offset, styles, offset + count, oldStyleData.length - offset);
        }
        Arrays.fill(styles, offset, offset + count, value);
    }
    
    private void removeStyleData(int startIndex, int endIndex) {
        if (styles == null) {
            return;
        }
        Style[] oldStyleData = maybeResizeStyleData();
        // insert and overwrite can get here when oldStyleData is null, but remove can't.
        System.arraycopy(oldStyleData, 0, styles, 0, startIndex);
        System.arraycopy(oldStyleData, endIndex, styles, startIndex, oldStyleData.length - endIndex);
    }
    
    /**
     * Ensures that the "styles" array is the right size for the current "text".
     * You should only call this if you know that the line requires non-default styling.
     */
    private Style[] maybeResizeStyleData() {
        Style[] oldStyleData = styles;
        if (styles == null || styles.length != text.length()) {
            styles = new Style[text.length()];
            Arrays.fill(styles, 0, styles.length, Style.getDefaultStyle());
        }
        return oldStyleData;
    }
}
