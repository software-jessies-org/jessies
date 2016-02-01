package e.ptextarea;

import java.awt.*;
import java.awt.event.*;
import e.util.*;

/**
 * FIXME: This should probably be rewritten to use KeyStroke key bindings.
 * 
 * FIXME: This should pay attention to what system we're running on, and try
 * to use the local key bindings. I've looked at the Mac OS key bindings in
 * vim(1) before now, and don't really understand the format. There's some
 * documentation at http://www.cocoadev.com/index.pl?KeyBindings but the
 * Apple documentation (which you can get to from that page) seems mainly
 * about a different format, used to override the StandardKeyBinding.dict
 * bindings. Project Builder/Xcode seem to use that format for their overrides,
 * and anywhere where we say we don't like the Mac OS behavior, we should be
 * writing our own ~/Library/KeyBindings/DefaultKeyBinding.dict file to change
 * all Mac OS applications, not just hacking about with PTextArea which just
 * makes everything more confusing. (So I guess we need to cope both with the
 * system-wide file and any user overrides.)
 * 
 * FIXME: we should find out if there are MS Windows/GNOME equivalents of the
 * Mac OS key bindings files.
 */
public class PKeyHandler implements KeyListener {
    // On Mac OS, native text components support some Emacs keystrokes.
    // Thanks to Apple's "command" key, they don't conflict with the standard keystrokes.
    private static boolean ENABLE_EMACS_KEYS = GuiUtilities.isMacOs();
    
    private PTextArea textArea;
    private PMouseHandler mouseHandler;
    private UpDownMovementHandler movementHandler = new UpDownMovementHandler();
    
    public PKeyHandler(PTextArea textArea, PMouseHandler mouseHandler) {
        this.textArea = textArea;
        this.mouseHandler = mouseHandler;
        textArea.addCaretListener(movementHandler);
    }
    
    public void keyPressed(KeyEvent e) {
        maybeUpdateCursorAndToolTip(e);
        // This is disabled because it's not been useful for years, and steals three key combinations.
        if (false && e.isControlDown() && e.isShiftDown()) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_T:
                textArea.logLineInfo();
                return;
            case KeyEvent.VK_L:
                textArea.getLineList().logLineInfo();
                return;
            case KeyEvent.VK_R:
                textArea.repaint();
                return;
            }
        }
        if (handleInvisibleKeyPressed(e)) {
            e.consume();
        }
    }
    
    public void keyTyped(KeyEvent e) {
        if (isInsertableCharacter(e) && textArea.isEditable()) {
            userIsTyping();
            insertCharacter(e.getKeyChar()); // Only the char is usable in these events anyway.
            e.consume();
        }
    }
    
    public void keyReleased(KeyEvent e) {
        maybeUpdateCursorAndToolTip(e);
    }
    
    private void maybeUpdateCursorAndToolTip(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
            // This isn't as effective as we might like, because we assume the mouse is over the text area receiving key events.
            // FIXME: forward to the PMouseHandler of the text area under the mouse.
            mouseHandler.updateCursorAndToolTip(e.isControlDown());
        }
    }
    
    private void userIsTyping() {
        if (textArea.shouldHideMouseWhenTyping()) {
            textArea.setCursor(GuiUtilities.INVISIBLE_CURSOR);
        }
    }
    
    private boolean isInsertableCharacter(KeyEvent e) {
        // On Mac OS, 'alt' is used to insert special characters (alt-3, for example, gives whichever of # and £ isn't on the key cap).
        // On other systems, we should ignore keys typed with 'alt' down. GNOME and Windows use 'alt' to traverse menus.
        if (GuiUtilities.isMacOs() == false && e.isAltDown()) {
            return false;
        }
        
        // FIXME: is 'alt graph' a special case on some systems? For now, assume it isn't.
        if (e.isAltGraphDown()) {
            return false;
        }
        
        // The 'control' key only inserts characters in terminal emulators.
        // The 'meta' (aka 'command' on Mac OS) key never inserts a character.
        if (e.isControlDown() || e.isMetaDown()) {
            return false;
        }
        
        char ch = e.getKeyChar();
        return (ch != KeyEvent.CHAR_UNDEFINED && ch >= ' ' && ch != Ascii.DEL);
    }
    
    private int extractKeyCode(KeyEvent event) {
        int key = event.getKeyCode();
        if (ENABLE_EMACS_KEYS && event.isControlDown()) {
            key = translateEmacsKeysToNormalKeys(key);
        }
        return key;
    }
    
    private static int translateEmacsKeysToNormalKeys(int key) {
        if (key == KeyEvent.VK_A) {
            return KeyEvent.VK_HOME;
        } else if (key == KeyEvent.VK_B) {
            return KeyEvent.VK_LEFT;
        } else if (key == KeyEvent.VK_D) {
            return KeyEvent.VK_DELETE;
        } else if (key == KeyEvent.VK_E) {
            return KeyEvent.VK_END;
        } else if (key == KeyEvent.VK_F) {
            return KeyEvent.VK_RIGHT;
        } else if (key == KeyEvent.VK_H) {
            return KeyEvent.VK_BACK_SPACE;
        } else if (key == KeyEvent.VK_N) {
            return KeyEvent.VK_DOWN;
        } else if (key == KeyEvent.VK_P) {
            return KeyEvent.VK_UP;
        } else {
            return key;
        }
    }
    
    private boolean handleInvisibleKeyPressed(KeyEvent event) {
        boolean byWord = GuiUtilities.isMacOs() ? event.isAltDown() : event.isControlDown();
        boolean extendingSelection = event.isShiftDown();
        final int key = extractKeyCode(event);
        if (textArea.isEditable() && key == KeyEvent.VK_TAB) {
            if (event.getModifiers() != 0) {
                // If any modifiers are down, pass on this event.
                // Pretty much every modifier+tab combination is used for something by at least one system, and we should keep out of the way.
                return false;
            }
            textArea.replaceSelection(textArea.getIndentationString());
        } else if (textArea.isEditable() && key == KeyEvent.VK_ENTER) {
            new PNewlineInserter(textArea).insertNewline(event.isShiftDown() == false);
        } else if (isStartOfTextKey(event)) {
            moveCaret(extendingSelection, 0);
        } else if (isEndOfTextKey(event)) {
            moveCaret(extendingSelection, textArea.getTextBuffer().length());
        } else if (isStartOfLineKey(event)) {
            moveCaret(extendingSelection, caretToStartOfLine());
        } else if (isEndOfLineKey(event)) {
            moveCaret(extendingSelection, caretToEndOfLine());
        } else if (isMatchingBracketKey(event)) {
            moveCaret(extendingSelection, caretToMatchingBracket());
        } else if (extendingSelection && key == KeyEvent.VK_PAGE_DOWN) {
            moveCaret(extendingSelection, caretPageDown());
        } else if (extendingSelection && key == KeyEvent.VK_PAGE_UP) {
            moveCaret(extendingSelection, caretPageUp());
        } else if (GuiUtilities.isMacOs() && (key == KeyEvent.VK_HOME || key == KeyEvent.VK_END)) {
            textArea.ensureVisibilityOfOffset((key == KeyEvent.VK_HOME) ? 0 : textArea.getTextBuffer().length());
        } else if (key == KeyEvent.VK_LEFT) {
            moveLeft(byWord, extendingSelection);
        } else if (key == KeyEvent.VK_RIGHT) {
            moveRight(byWord, extendingSelection);
        } else if (key == KeyEvent.VK_BACK_SPACE) {
            backspace(event);
        } else if (key == KeyEvent.VK_DELETE) {
            // Legacy CUA-style cut/delete (http://en.wikipedia.org/wiki/Cut,_copy,_and_paste).
            if (event.isShiftDown()) {
                textArea.cut();
            } else {
                // With Ctrl, Word 2003 and Wordpad delete the remainder of the word to the right.
                // Notepad kills the remainder of the line to the right.
                // Word 2003 sometimes also deletes a preceding space, but I've been unable to determine quite when.
                // Wordpad doesn't do that, even with the same corpus.
                if (byWord) {
                    moveRight(true, true);
                }
                delete();
            }
        } else if (key == KeyEvent.VK_INSERT && event.isShiftDown() && event.isControlDown() == false) {
            // Legacy CUA-style paste (http://en.wikipedia.org/wiki/Cut,_copy,_and_paste).
            textArea.paste();
        } else if (key == KeyEvent.VK_INSERT && event.isShiftDown() == false && event.isControlDown()) {
            // Legacy CUA-style copy (http://en.wikipedia.org/wiki/Cut,_copy,_and_paste).
            textArea.copy();
        } else if (ENABLE_EMACS_KEYS && event.isControlDown() && key == KeyEvent.VK_Y) {
            // Legacy Emacs-style paste ("yank", which means "copy" in Vi but "paste" in Emacs).
            // FIXME: this is supposed to paste from the "kill buffer", not the clipboard.
            textArea.paste();
        } else if (key == KeyEvent.VK_UP || key == KeyEvent.VK_DOWN) {
            movementHandler.handleMovementKeys(key, extendingSelection);
        } else if (ENABLE_EMACS_KEYS && event.isControlDown() && key == KeyEvent.VK_K) {
            // Emacs kill-to-end-of-line.
            // FIXME: this is supposed to cut to the "kill buffer", not the clipboard.
            // FIXME: I also think this is wrong in the case where the selection is non-empty.
            moveCaret(true, caretToEndOfLine());
            textArea.cut();
        } else {
            return false;
        }
        // All the keystrokes that did something fall through to here.
        return true;
    }
    
    private boolean isStartOfLineKey(KeyEvent e) {
        if (GuiUtilities.isMacOs()) {
            if (ENABLE_EMACS_KEYS && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
                return true;
            }
            return ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_LEFT);
        } else {
            return (e.getKeyCode() == KeyEvent.VK_HOME);
        }
    }
    
    private boolean isEndOfLineKey(KeyEvent e) {
        if (GuiUtilities.isMacOs()) {
            if (ENABLE_EMACS_KEYS && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_E) {
                return true;
            }
            return ((e.isControlDown() || e.isMetaDown()) && e.getKeyCode() == KeyEvent.VK_RIGHT);
        } else {
            return (e.getKeyCode() == KeyEvent.VK_END);
        }
    }
    
    private boolean isMatchingBracketKey(KeyEvent e) {
        return (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_5);
    }
    
    private boolean isStartOfTextKey(KeyEvent e) {
        if (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_UP) {
            return true;
        }
        return (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_HOME);
    }
    
    private boolean isEndOfTextKey(KeyEvent e) {
        if (e.isMetaDown() && e.getKeyCode() == KeyEvent.VK_DOWN) {
            return true;
        }
        return (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_END);
    }
    
    private void insertCharacter(char ch) {
        CharSequence content = new CharArrayCharSequence(new char[] { ch });
        if (textArea.getIndenter().isElectric(ch)) {
            textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
            try {
                textArea.replaceSelection(content);
                textArea.getIndenter().fixIndentation();
            } finally {
                textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
            }
        } else {
            textArea.replaceSelection(content);
        }
    }
    
    private void backspace(KeyEvent e) {
        if (textArea.isEditable() == false) {
            return;
        }
        
        Range range = determineRangeToRemove(e);
        if (range.isNonEmpty()) {
            textArea.delete(range.getStart(), range.length());
        }
    }
    
    private Range determineRangeToRemove(KeyEvent e) {
        int position = textArea.getSelectionStart();
        
        if (e.isControlDown() && e.isShiftDown()) {
            // control-shift-backspace => delete line, similar to emacs.
            final int lineNumber = textArea.getLineOfOffset(position);
            int start = textArea.getLineStartOffset(lineNumber);
            int end = Math.min(textArea.getLineEndOffsetBeforeTerminator(lineNumber) + 1, textArea.getTextBuffer().length());
            return new Range(start, end);
        }
        
        if (textArea.hasSelection()) {
            // The user's already done our work for us.
            return new Range(textArea.getSelectionStart(), textArea.getSelectionEnd());
        }
        
        if (position == 0) {
            // We can't remove anything before the beginning.
            return Range.NULL_RANGE;
        }
        
        int charactersToDelete = 1;
        final int lineNumber = textArea.getLineOfOffset(position);
        String whitespace = textArea.getIndenter().getCurrentIndentationOfLine(lineNumber);
        int lineOffset = position - textArea.getLineStartOffset(lineNumber);
        CharSequence chars = textArea.getTextBuffer();
        if (e.isControlDown()) {
            // Delete to beginning of previous word.
            int startPosition = position - 1;
            charactersToDelete = position - caretToPreviousWord();
        } else if (e.isAltDown()) {
            // Delete back to beginning of line.
            charactersToDelete = lineOffset;
        } else if (lineOffset > 1 && lineOffset <= whitespace.length()) {
            String tab = textArea.getIndentationString();
            whitespace = whitespace.substring(0, lineOffset);
            while (whitespace.startsWith(tab)) {
                whitespace = whitespace.substring(tab.length());
            }
            charactersToDelete = whitespace.length();
            if (charactersToDelete == 0) {
                charactersToDelete = tab.length();
            }
        }
        
        return new Range(position - charactersToDelete, position);
    }
    
    private void delete() {
        if (textArea.isEditable() == false) {
            return;
        }
        
        int start = textArea.getSelectionStart();
        int end = textArea.getSelectionEnd();
        if (start == end && end < textArea.getTextBuffer().length()) {
            ++end;
        }
        if (start != end) {
            textArea.delete(start, end - start);
        }
    }
    
    private void moveCaret(boolean extendingSelection, int newOffset) {
        if (extendingSelection) {
            textArea.changeUnanchoredSelectionExtreme(newOffset);
        } else {
            textArea.setCaretPosition(newOffset);
        }
    }
    
    private int caretPageDown() {
        int lineIndex = textArea.getLineOfOffset(textArea.getUnanchoredSelectionExtreme());
        int pageLineCount = textArea.getVisibleRect().height / textArea.getLineHeight();
        return textArea.getLineStartOffset(Math.min(lineIndex + pageLineCount, textArea.getLineCount() - 1));
    }
    
    private int caretPageUp() {
        int lineIndex = textArea.getLineOfOffset(textArea.getUnanchoredSelectionExtreme());
        int pageLineCount = textArea.getVisibleRect().height / textArea.getLineHeight();
        return textArea.getLineStartOffset(Math.max(0, lineIndex - pageLineCount));
    }
    
    private int caretToStartOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getUnanchoredSelectionExtreme());
        return textArea.getLineStartOffset(lineIndex);
    }
    
    private int caretToEndOfLine() {
        int lineIndex = textArea.getLineOfOffset(textArea.getUnanchoredSelectionExtreme());
        return textArea.getLineEndOffsetBeforeTerminator(lineIndex);
    }
    
    private int caretToMatchingBracket() {
        int caretOffset = textArea.getUnanchoredSelectionExtreme();
        if (PBracketUtilities.isNextToBracket(textArea.getTextBuffer(), caretOffset) == false) {
            // We're not next to a bracket.
            return caretOffset;
        }
        int bracketOffset = PBracketUtilities.findMatchingBracketInSameStyle(textArea, caretOffset);
        if (bracketOffset == -1) {
            // No matching bracket.
            return caretOffset;
        }
        // We want to land "inside" the brackets, so going backwards we want the offset of the character after the bracket.
        return (bracketOffset < caretOffset) ? (bracketOffset + 1) : bracketOffset;
    }
    
    private void moveLeft(boolean byWord, boolean extendingSelection) {
        int newOffset = byWord ? caretToPreviousWord() : caretLeft(extendingSelection);
        moveCaret(extendingSelection, newOffset);
    }
    
    private void moveRight(boolean byWord, boolean extendingSelection) {
        int newOffset = byWord ? caretToNextWord() : caretRight(extendingSelection);
        moveCaret(extendingSelection, newOffset);
    }
    
    private int caretLeft(boolean extendingSelection) {
        int newOffset = textArea.getSelectionStart();
        if (extendingSelection) {
            newOffset = textArea.getUnanchoredSelectionExtreme() - 1;
        } else if (textArea.hasSelection() == false) {
            --newOffset;
        }
        return Math.max(0, newOffset);
    }
    
    private int caretRight(boolean extendingSelection) {
        int newOffset = textArea.getSelectionEnd();
        if (extendingSelection) {
            newOffset = textArea.getUnanchoredSelectionExtreme() + 1;
        } else if (textArea.hasSelection() == false) {
            ++newOffset;
        }
        return Math.min(newOffset, textArea.getTextBuffer().length());
    }
    
    private int caretToPreviousWord() {
        CharSequence chars = textArea.getTextBuffer();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        int offset = textArea.getUnanchoredSelectionExtreme();
        
        // If we're at the start of the document, we're not going far.
        if (offset == 0) {
            return 0;
        }
        
        // If we're at the start of a word, go to the start of the word before.
        if (PWordUtilities.isInWord(chars, offset - 1, stopChars) == false) {
            return PWordUtilities.getWordStart(chars, PWordUtilities.getNonWordStart(chars, offset - 1, stopChars), stopChars);
        }
        
        // Otherwise go to the start of the current word.
        return PWordUtilities.getWordStart(chars, offset, stopChars);
    }
    
    private int caretToNextWord() {
        CharSequence chars = textArea.getTextBuffer();
        String stopChars = PWordUtilities.DEFAULT_STOP_CHARS;
        int offset = textArea.getUnanchoredSelectionExtreme();
        
        // If we're at the end of the document, we're not going far.
        if (offset == chars.length()) {
            return offset;
        }
        
        // If we're in a word, go to the end of this word.
        if (PWordUtilities.isInWord(chars, offset, stopChars)) {
            return PWordUtilities.getWordEnd(chars, offset, stopChars);
        }
        
        // Otherwise go to the start of the next word.
        return PWordUtilities.getWordEnd(chars, PWordUtilities.getNonWordEnd(chars, PWordUtilities.getWordEnd(chars, offset, stopChars), stopChars), stopChars);
    }
    
    private class UpDownMovementHandler implements PCaretListener {
        private boolean isEntered = false;
        private int xPixelLocation = -1;
        
        public void caretMoved(PTextArea textArea, int selectionStart, int selectionEnd) {
            if (isEntered == false) {
                xPixelLocation = -1;
            }
        }
        
        public void handleMovementKeys(int key, boolean extendingSelection) {
            isEntered = true;
            if (xPixelLocation == -1) {
                xPixelLocation = getCurrentXPixelLocation();
            }
            try {
                moveCaret(extendingSelection, key == KeyEvent.VK_UP ? caretUp() : caretDown());
            } finally {
                isEntered = false;
            }
        }
        
        private int getCurrentXPixelLocation() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            return textArea.getViewCoordinates(coords).x;
        }
        
        private int caretUp() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == 0) {
                return 0;
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex - 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
        
        private int caretDown() {
            PCoordinates coords = textArea.getCoordinates(textArea.getUnanchoredSelectionExtreme());
            int lineIndex = coords.getLineIndex();
            if (lineIndex == textArea.getSplitLineCount() - 1) {
                return textArea.getTextBuffer().length();
            } else {
                int y = textArea.getViewCoordinates(new PCoordinates(lineIndex + 1, 0)).y;
                return textArea.getTextIndex(textArea.getNearestCoordinates(new Point(xPixelLocation, y)));
            }
        }
    }
}
