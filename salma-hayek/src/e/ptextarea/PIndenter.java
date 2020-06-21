package e.ptextarea;

import e.util.*;
import java.util.*;
import java.util.regex.*;

/*
A quick summary of the class hierarchy of the PIndenter family:

  PIndenter                      Base class.
    PNoOpIndenter                Bare bones  - does nothing special.
    PSimpleIndenter              Does some language-specific stuff it shouldn't be doing (eg java comments).
      PGenericIndenter           Provides implementation for regexp-based indentation.
        PBashIndenter            Regexp-based indentation for bash
        PRubyIndenter
      PCFamilyIndenter
        PCppIndenter             Handles C++, proto, rust (rust just because we have nothing better)
        PJavaIndenter
        PPerlIndenter
      PGoIndenter
      PPythonIndenter
*/

/**
 * Defines the interface provided by every indenter.
 */
public abstract class PIndenter {
    private static final Pattern INDENTATION_PATTERN = Pattern.compile("(^[ \\t]*(?:\\*(?: |$))?).*$");
    
    protected PTextArea textArea;
    protected PrefixedPreferences preferences;
    
    public PIndenter(PTextArea textArea) {
        this.textArea = textArea;
    }
    
    public void setPreferences(PrefixedPreferences preferences) {
        this.preferences = preferences;
    }
    
    /**
     * Override this function to return the preferences that your indenter supports.
     * Important: *ALWAYS* first retrieve super.getPreferences() and add to that list.
     */
    public ArrayList<Preference> getPreferences() {
        return new ArrayList<Preference>();
    }
    
    public static class Preference {
        private final String key;
        private final Object value;
        private final String description;
        
        public Preference(String key, Object value, String description) {
            this.key = key;
            this.value = value;
            this.description = description;
        }
        
        public String getKey() {
            return key;
        }
        
        public Object getValue() {
            return value;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * Returns true if c is an 'electric' character.
     * This is Emacs terminology for a character that, when typed, causes the line's indentation to be reassessed.
     * Typically, this signifies the end of a block.
     * 
     * Note that Vim uses a significantly more sophisticated system:
     * http://vimdoc.sourceforge.net/htmldoc/indent.html#indentkeys-format
     */
    public abstract boolean isElectric(char c);
    
    /**
     * Replaces the given line with a correctly-indented version.
     */
    public abstract void fixIndentationOnLine(int lineIndex);
    
    /**
     * Does whatever needs doing when the user hits Return. The basic action of the PIndenter is to
     * insert a newline, and copy the indentation string from the last non-blank line.
     * This handles the undo buffer compound editing support - override the insertNewlineImpl to do clever stuff.
     */
    public final void insertNewline(boolean fixIndentation) {
        textArea.getTextBuffer().getUndoBuffer().startCompoundEdit();
        try {
            insertNewlineImpl(fixIndentation);
        } finally {
            textArea.getTextBuffer().getUndoBuffer().finishCompoundEdit();
        }
    }
    
    // Inserts a newline (and does whatever indentation fixing).
    // This is always called with the undo buffer in compound edit mode.
    public void insertNewlineImpl(boolean fixIndentation) {
        int startLineIndex = textArea.getLineOfOffset(textArea.getSelectionStart());
        textArea.replaceSelection("\n" + textArea.getIndenter().getCurrentIndentationOfLine(startLineIndex));
    }
    
    /**
     * Returns a copy of just the leading part of the given line.
     * Usually that just contains whitespace but the asterisks at the start
     * of the body lines of a doc-comment are also considered as indentation
     * because they end in (a single character of) whitespace.
     */
    public final String getCurrentIndentationOfLine(int lineNumber) {
        return indentationOf(textArea.getLineText(lineNumber));
    }
    
    public static final String indentationOf(String line) {
        Matcher matcher = INDENTATION_PATTERN.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("line \"" + line + "\" has impossible indentation");
        }
    }
    
    /**
     * Corrects the indentation of the lines touched by the selection.
     */
    public final void fixIndentation() {
        fixIndentationBetween(textArea.getSelectionStart(), textArea.getSelectionEnd());        
    }
    
    /**
     * Corrects the indentation of the lines touched by the range of characters from startOffset to endOffset.
     */
    public final void fixIndentationBetween(final int startOffset, final int endOffset) {
        final int startLine = textArea.getLineOfOffset(startOffset);
        // I've thought about (and experimented with) the +-1 issue here.
        final int finishLine = textArea.getLineOfOffset(endOffset);
        for (int lineIndex = startLine; lineIndex <= finishLine; ++lineIndex) {
            fixIndentationOnLine(lineIndex);
        }
    }
}
