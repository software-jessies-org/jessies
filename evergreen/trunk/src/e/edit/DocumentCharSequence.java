package e.edit;

import javax.swing.text.*;

/**
 * Wraps the legacy javax.swing.text.Document interface as a modern
 * CharSequence.
 */
public class DocumentCharSequence implements CharSequence {
    private Document document;
    
    public DocumentCharSequence(Document document) {
        this.document = document;
    }
    
    public int length() {
        return document.getLength();
    }
    
    public char charAt(int index) {
        try {
            return document.getText(index, 1).charAt(0);
        } catch (BadLocationException ex) {
            throw new IndexOutOfBoundsException(Integer.toString(index) + " not in [0.." + length() + ")");
        }
    }
    
    public CharSequence subSequence(int start, int end) {
        throw new UnsupportedOperationException("FIXME: this could be implemented, but wasn't needed at the time");
    }
    
    public String toString() {
        try {
            return document.getText(0, length());
        } catch (BadLocationException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
