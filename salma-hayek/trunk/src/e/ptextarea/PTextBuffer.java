package e.ptextarea;

import e.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

/**
 * A PTextBuffer is the Document of the PTextArea.  It implements the CharSequence interface, and is
 * responsible for maintaining undo lists.
 * 
 * @author Phil Norman
 */

public class PTextBuffer implements CharSequence {
    public static final String LINE_ENDING_PROPERTY = "LineEndingProperty";
    
    private static final String CHARSET = "UTF-8";
    
    private char[] text = new char[0];
    private int gapPosition;
    private int gapLength;
    private ArrayList listeners = new ArrayList();
    private Undoer undoBuffer = new Undoer();
    private HashMap properties = new HashMap();
    
    public PTextBuffer() {
        initDefaultProperties();
    }
    
    private void initDefaultProperties() {
        putProperty(LINE_ENDING_PROPERTY, "\n");
    }
    
    /**
     * Returns the value of the named property.
     */
    public Object getProperty(String name) {
        return properties.get(name);
    }
    
    /**
     * Associates 'value' with the property 'name'. You can either use one
     * of the pre-defined property constant names, or your own names. Your
     * own names should include your fully-qualified domain to avoid collision.
     */
    public void putProperty(String name, Object value) {
        properties.put(name, value);
    }
    
    public PUndoBuffer getUndoBuffer() {
        return undoBuffer;
    }
    
    /**
     * Add a listener which will be informed when text is added or removed, or when the text is completely
     * replaced.
     */
    public void addTextListener(PTextListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a previously added listener.
     */
    public void removeTextListener(PTextListener listener) {
        listeners.remove(listener);
    }
    
    private void fireEvent(PTextEvent event) {
        for (int i = 0; i < listeners.size(); i++) {
            PTextListener listener = (PTextListener) listeners.get(i);
            if (event.isInsert()) {
                listener.textInserted(event);
            } else if (event.isRemove()) {
                listener.textRemoved(event);
            } else {
                listener.textCompletelyReplaced(event);
            }
        }
    }
    
    /**
     * Replaces the contents of this buffer with the entire contents of 'file'.
     */
    public void readFromFile(File file) {
        DataInputStream dataInputStream = null;
        try {
            // Read all the bytes in.
            long byteCount = file.length();
            FileInputStream fileInputStream = new FileInputStream(file);
            ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[(int) byteCount]);
            dataInputStream = new DataInputStream(fileInputStream);
            dataInputStream.readFully(byteBuffer.array());
            
            // Convert the bytes to characters.
            CharBuffer charBuffer = Charset.forName(CHARSET).decode(byteBuffer);
            
            // And use the characters as our content.
            char[] chars = charBuffer.array();
            String lineEnding = "\n";
            
            // Cope with weird line-endings.
            if (charArrayContains(chars, '\r')) {
                String s = new String(chars);
                if (s.indexOf("\r\n") != -1) {
                    lineEnding = "\r\n";
                    s = s.replaceAll("\r\n", "\n");
                } else {
                    lineEnding = "\r";
                    s = s.replaceAll("\r", "\n");
                }
                chars = s.toCharArray();
            }
            
            putProperty(LINE_ENDING_PROPERTY, lineEnding);
            setText(chars);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtilities.close(dataInputStream);
        }
    }
    
    private static final boolean charArrayContains(char[] chars, char ch) {
        final int end = chars.length;
        for (int i = 0; i < end; ++i) {
            if (chars[i] == ch) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Writes the contents of this buffer into the given file, replacing
     * whatever's already there.
     */
    public void writeToFile(File file) {
        Writer writer = null;
        try {
            writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), CHARSET));
            
            String lineEnding = (String) getProperty(LINE_ENDING_PROPERTY);
            if (lineEnding.equals("\n")) {
                // Just write out the two halves as they are.
                if (gapPosition != 0) {
                    writer.write(text, 0, gapPosition);
                }
                final int gapEnd = gapPosition + gapLength;
                if (gapEnd < text.length) {
                    writer.write(text, gapEnd, text.length - gapEnd);
                }
            } else {
                // Split our internal content into lines, and write them
                // out individually. Expensive, but why aren't you using
                // Unix line-endings, crazy person?
                String[] lines = toString().split("\n");
                for (int i = 0; i < lines.length; ++i) {
                    writer.write(lines[i]);
                    writer.write(lineEnding);
                }
            }
            writer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtilities.close(writer);
        }
    }
    
    /**
     * Sets the text, replacing anything that was here before.
     * Note that this method does not copy the given char[].
     */
    private void setText(char[] text) {
        this.text = text;
        gapPosition = 0;
        gapLength = 0;
        fireEvent(new PTextEvent(this, PTextEvent.COMPLETE_REPLACEMENT, 0, text));
    }
    
    /**
     * Returns a copy of the specified region of text.  This involves making a
     * complete copy of the specified region of text, and so should only be
     * used if you need to keep the copy unchanged in the face of future edits.
     * If not, use the CharSequence interface instead.
     */
    private char[] copyChars(int start, int charCount) {
        char[] result = new char[charCount];
        try {
            int copyCount = 0;
            if (start < gapPosition) {
                copyCount = Math.min(charCount, gapPosition - start);
                System.arraycopy(text, start, result, 0, copyCount);
            }
            if (start + charCount >= gapPosition) {
                int textPosition = Math.max(start, gapPosition) + gapLength;
                System.arraycopy(text, textPosition, result, copyCount, result.length - copyCount);
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.err.println("Requested get text from " + start + ", length " + charCount + "; size is " + length());
            ex.printStackTrace();
        }
        return result;
    }
    
    /** Moves the gap to the specified position. */
    private void moveGap(int newPosition) {
        if (gapPosition == newPosition) {
            return;
        }
        if (newPosition < gapPosition) {
            System.arraycopy(text, newPosition, text, newPosition + gapLength, gapPosition - newPosition);
        } else {
            System.arraycopy(text, gapPosition + gapLength, text, gapPosition, newPosition - gapPosition);
        }
        gapPosition = newPosition;
    }
    
    /** Expands the buffer. */
    private void expandBuffer() {
        char[] newText = new char[Math.max(64, text.length * 2)];
        int sizeIncrease = newText.length - text.length;
        System.arraycopy(text, 0, newText, 0, gapPosition);
        int endOffset = gapPosition + gapLength;
        System.arraycopy(text, endOffset, newText, endOffset + sizeIncrease, text.length - endOffset);
        gapLength += sizeIncrease;
        text = newText;
    }
    
    /** Deletes the given number of characters, the lowest-indexed of which is at the given position. */
    public void delete(int position, int count) {
        char[] ch = copyChars(position, count);
        undoBuffer.addDeletion(position, ch);
        deleteWithoutUndo(position, ch);
    }
    
    /** Special deletion method used by the undo buffer. */
    private void deleteWithoutUndo(int position, char[] ch) {
        moveGap(position + ch.length);
        gapPosition -= ch.length;
        gapLength += ch.length;
        fireEvent(new PTextEvent(this, PTextEvent.REMOVE, position, ch));
    }
    
    /** Inserts the given characters to the right of the given position. */
    public void insert(int position, char[] ch) {
        undoBuffer.addInsertion(position, ch);
        insertWithoutUndo(position, ch);
    }
    
    /** Special insertion method used by the undo buffer. */
    private void insertWithoutUndo(int position, char[] ch) {
        moveGap(position);
        int textLength = ch.length;
        while (textLength > gapLength) {
            expandBuffer();
        }
        System.arraycopy(ch, 0, text, gapPosition, textLength);
        gapPosition += textLength;
        gapLength -= textLength;
        fireEvent(new PTextEvent(this, PTextEvent.INSERT, position, ch));
    }
    
    /** Returns the character at the given index.  Part of the CharSequence interface. */
    public char charAt(int index) {
        return (index < gapPosition) ? text[index] : text[index + gapLength];
    }
   
    /** Returns the number of characters in the text area.  Part of the CharSequence interface. */
    public int length() {
        return text.length - gapLength;
    }
    
    /**
     * Returns a CharSequence which holds the specified section of the PTextBuffer's text.  Part of the
     * CharSequence interface.
     */
    public CharSequence subSequence(int start, int end) {
        return new SubSequence(start, end);
    }
    
    /**
     * Returns an identical copy of the text held within the buffer.  Use the CharSequence interface
     * for preference.
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        result.append(text, 0, gapPosition);
        result.append(text, gapPosition + gapLength, text.length - gapPosition - gapLength);
        return result.toString();
    }
    
    /**
     * A SubSequence is the implementation of a CharSequence which handles sub-sequences
     * of the PTextBuffer's text.
     * Note that calling 'subSequence' on a SubSequence returns another SubSequence instance
     * which acts directly on the PTextBuffer; they do not chain.
     */
    public class SubSequence implements CharSequence {
        private int start;
        private int end;
        
        public SubSequence(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        public char charAt(int index) {
            return PTextBuffer.this.charAt(start + index);
        }
        
        public int length() {
            return end - start;
        }
        
        public CharSequence subSequence(int subStart, int subEnd) {
            if (subEnd < subStart) {
                throw new IndexOutOfBoundsException("Subsequence end " + subEnd + " less than start " + subStart);
            }
            return PTextBuffer.this.subSequence(start + subStart, start + subEnd);
        }
        
        public String toString() {
            return new String(copyChars(start, end - start));
        }
    }
    
    public class Undoer implements PUndoBuffer {
        private ArrayList undoList = new ArrayList();
        private int undoPosition = 0;
        
        private void addInsertion(int position, char[] ch) {
            addDoable(new Doable(position, ch, false));
        }
        
        private void addDeletion(int position, char[] ch) {
            addDoable(new Doable(position, ch, true));
        }
        
        private void addDoable(Doable doable) {
            while (undoList.size() > undoPosition) {
                undoList.remove(undoPosition);
            }
            undoList.add(doable);
        }
        
        public boolean canUndo() {
            return (undoPosition > 0);
        }
        
        public boolean canRedo() {
            return (undoPosition < undoList.size());
        }
        
        public void undo() {
            if (canUndo()) {
                undoPosition--;
                Doable doable = (Doable) undoList.get(undoPosition);
                doable.undo();
            }
        }
        
        public void redo() {
            if (canRedo()) {
                Doable doable = (Doable) undoList.get(undoPosition);
                undoPosition++;
                doable.redo();
            }
        }
    }
    
    private class Doable {
        private int position;
        private char[] ch;
        private boolean isDeletion;
        
        public Doable(int position, char[] ch, boolean isDeletion) {
            this.position = position;
            this.ch = ch;
            this.isDeletion = isDeletion;
        }
        
        public void undo() {
            if (isDeletion) {
                insertWithoutUndo(position, ch);
            } else {
                deleteWithoutUndo(position, ch);
            }
        }
        
        public void redo() {
            if (isDeletion) {
                deleteWithoutUndo(position, ch);
            } else {
                insertWithoutUndo(position, ch);
            }
        }
    }
}
