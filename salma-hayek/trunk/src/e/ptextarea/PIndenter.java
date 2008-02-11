package e.ptextarea;

import java.util.regex.*;

/**
 * Defines the interface provided by every indenter.
 */
public abstract class PIndenter {
    private static final Pattern INDENTATION_PATTERN = Pattern.compile("(^[ \\t]*(?:\\*(?: |$))?).*$");
    
    protected PTextArea textArea;
    
    public PIndenter(PTextArea textArea) {
        this.textArea = textArea;
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
    
    // FIXME: this is a hack for the benefit of PNewlineInserter, which does its own indentation fixing when inserting matching brackets, rather than deferring to the indenter. (Why is that the case?)
    public boolean isInNeedOfClosingSemicolon(String line) {
        return false;
    }
    
    /**
     * Returns true if this indenter isn't capable of fixing the current line, and is only usable for computing auto-indent (i.e. indentation for the next line).
     */
    public boolean canOnlyAutoIndent() {
        return false;
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
