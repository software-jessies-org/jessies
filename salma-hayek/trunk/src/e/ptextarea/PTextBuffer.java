package e.ptextarea;

import e.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A PTextBuffer is the Document of the PTextArea.  It implements the CharSequence interface, and is
 * responsible for maintaining undo lists.
 * 
 * @author Phil Norman
 */

public class PTextBuffer implements CharSequence {
    public static final String LINE_ENDING_PROPERTY = "LineEndingProperty";
    public static final String INDENTATION_PROPERTY = "IndentationProperty";
    
    private static final String CHARSET = "UTF-8";
    private static final int MIN_BUFFER_EXTENSION = 100;
    private static final int MAX_GAP_SIZE = 1024 * 2;
    
    private char[] text = new char[0];
    private int gapPosition;
    private int gapLength;
    private ArrayList<PTextListener> textListeners = new ArrayList<PTextListener>();
    private PAnchorSet anchorSet = new PAnchorSet();
    private Undoer undoBuffer = new Undoer();
    private HashMap<String, Object> properties = new HashMap<String, Object>();
    
    public PTextBuffer() {
        // Our anchorSet *must* be the first listener.  It needs to update the anchor locations
        // before anyone else starts messing about with them.
        addTextListener(anchorSet);
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
    
    public PAnchorSet getAnchorSet() {
        return anchorSet;
    }
    
    public PUndoBuffer getUndoBuffer() {
        return undoBuffer;
    }
    
    /**
     * Add a listener which will be informed when text is added or removed, or when the text is completely
     * replaced.
     */
    public void addTextListener(PTextListener listener) {
        textListeners.add(listener);
    }
    
    /**
     * Remove a previously added listener.
     */
    public void removeTextListener(PTextListener listener) {
        textListeners.remove(listener);
    }
    
    private void fireTextEvent(PTextEvent event) {
        // Although most Swing listeners are called in reverse order, these events are not
        // consume()able, so I don't really see the point in doing so here.  It is vital that
        // some listeners (the PLineList and PAnchorSet to name two) are called before
        // anyone else, to maintain internal state properly.  Therefore this loop will go
        // forwards through the listener list until someone comes up with a really good
        // reason why backwards is better (at which point I'll add an extra 'internalListeners'
        // list).
        for (PTextListener listener : textListeners) {
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
        fireTextEvent(new PTextEvent(this, PTextEvent.COMPLETE_REPLACEMENT, 0, new CharArrayCharSequence(text)));
    }
    
    /**
     * Returns a copy of the specified region of text.  This involves making a
     * complete copy of the specified region of text, and so should only be
     * used if you need to keep the copy unchanged in the face of future edits.
     * If not, use the CharSequence interface instead.
     */
    private CharSequence copyChars(int start, int charCount) {
        if (start < 0 || charCount < 0 || start + charCount > length()) {
            throw new IllegalArgumentException("start=" + start + " charCount=" + charCount + " length()=" + length());
        }
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
        return new CharArrayCharSequence(result);
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
    
    private void changeBufferLength(int lengthChange) {
        char[] newText = new char[text.length + lengthChange];
        System.arraycopy(text, 0, newText, 0, gapPosition);
        int endOffset = gapPosition + gapLength;
        System.arraycopy(text, endOffset, newText, endOffset + lengthChange, text.length - endOffset);
        gapLength += lengthChange;
        text = newText;
    }
    
    /** Expands the buffer. */
    private void expandBuffer(int requiredGapLength) {
        int desiredGapIncrease = requiredGapLength + Math.min(MAX_GAP_SIZE, requiredGapLength);
        changeBufferLength(Math.max(MIN_BUFFER_EXTENSION, desiredGapIncrease));
    }
    
    /** Shrinks the buffer. */
    private void shrinkBuffer() {
        if (gapLength > MAX_GAP_SIZE) {
            int desiredGapLength = Math.max(MIN_BUFFER_EXTENSION, gapLength - MAX_GAP_SIZE);
            changeBufferLength(desiredGapLength - gapLength);
        }
    }
    
    public void replace(SelectionSetter beforeCaret, int position, int removeCount, CharSequence add, SelectionSetter afterCaret) {
        if (beforeCaret == null) {
            throw new IllegalArgumentException("beforeCaret must not be null");
        }
        if (afterCaret == null) {
            throw new IllegalArgumentException("afterCaret must not be null");
        }
        CharSequence removeChars = copyChars(position, removeCount);
        undoBuffer.addAndDo(beforeCaret, position, removeChars, add, afterCaret);
    }
    
    /** Special remove method used by the undo buffer. */
    private void removeWithoutUndo(int position, CharSequence chars) {
        moveGap(position + chars.length());
        gapPosition -= chars.length();
        gapLength += chars.length();
        shrinkBuffer();
        fireTextEvent(new PTextEvent(this, PTextEvent.REMOVE, position, chars));
    }
    
    /** Special insertion method used by the undo buffer. */
    private void insertWithoutUndo(int position, CharSequence chars) {
        moveGap(position);
        int textLength = chars.length();
        while (textLength > gapLength) {
            expandBuffer(textLength);
        }
        if (chars instanceof CharArrayCharSequence) {
            ((CharArrayCharSequence) chars).copyTo(text, gapPosition);
        } else {
            // This is the price you pay for giving us a String.
            for (int i = 0; i < chars.length(); ++i) {
                text[gapPosition + i] = chars.charAt(i);
            }
        }
        gapPosition += textLength;
        gapLength -= textLength;
        fireTextEvent(new PTextEvent(this, PTextEvent.INSERT, position, chars));
    }
    
    /** Returns the character at the given index.  Part of the CharSequence interface. */
    public char charAt(int index) {
        if (index < 0 || index >= length()) {
            throw new IndexOutOfBoundsException("index " + index + " not in range [0.." + length() + ")");
        }
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
            // FIXME: in 1.5, use "new StringBuilder(copyChars(start, end - start)).toString()".
            StringBuffer result = new StringBuffer(end - start);
            CharSequence chars = copyChars(start, end - start);
            for (int i = 0; i < chars.length(); ++i) {
                result.append(chars.charAt(i));
            }
            return result.toString();
        }
    }
    
    public class Undoer implements PUndoBuffer {
        private ArrayList<Doable> undoList;
        private int undoPosition;
        
        private int cleanPosition = -1;
        
        // How many levels of nested compound edits we have.
        private int compoundingDepth;
        
        // A sequence number for compound edits, so we can tell adjacent
        // compound edits apart in the undo history, where the nesting is all
        // flattened out.
        private int compoundId;
        
        // A special compoundId to make edits that aren't part of any compound
        // edit easily recognizable.
        private static final int NOT_COMPOUND = -1;
        
        private ArrayList<ChangeListener> changeListeners = new ArrayList<ChangeListener>();
        
        public Undoer() {
            resetUndoBuffer();
        }
        
        public void resetUndoBuffer() {
            this.undoList = new ArrayList<Doable>();
            this.undoPosition = 0;
            this.compoundingDepth = 0;
            this.compoundId = 0;
            fireChangeListeners();
        }
        
        private void addAndDo(SelectionSetter beforeCaret, int position, CharSequence removeChars, CharSequence insertChars, SelectionSetter afterCaret) {
            // FIXME: use ArrayList.removeRange.
            while (undoList.size() > undoPosition) {
                cleanPosition = -1;  // We can never be clean again until we save.
                undoList.remove(undoPosition);
            }
            
            int id = (compoundingDepth == 0) ? NOT_COMPOUND : compoundId;
            Doable newEdit = new Doable(id, beforeCaret, position, removeChars, insertChars, afterCaret);
            undoList.add(newEdit);
            redo();
        }
        
        public void startCompoundEdit() {
            ++compoundingDepth;
            //System.out.println("started compound edit");
        }
        
        public void dump() {
            int i = 0;
            for (Doable edit : undoList) {
                System.out.println(i++ + ": " + edit);
            }
        }
        
        public void finishCompoundEdit() {
            if (compoundingDepth == 0) {
                dump();
                throw new IllegalStateException("can't finish a compound edit when there isn't one active");
            }
            --compoundingDepth;
            ++compoundId;
            //System.out.println("finished compound edit");
            //dump();
        }
        
        public void setCurrentStateClean() {
            cleanPosition = undoPosition;
        }
        
        public boolean isClean() {
            return (cleanPosition == undoPosition);
        }
        
        public boolean canUndo() {
            return (undoPosition > 0);
        }
        
        public boolean canRedo() {
            return (undoPosition < undoList.size());
        }
        
        /**
         * Tests whether the Doable at 'index' in the undo list is a
         * continuation of 'doable'.
         */
        private boolean compoundContinuesAt(Doable doable, int index) {
            if (doable.isNotCompound()) {
                return false;
            }
            if (index < 0 || index >= undoList.size()) {
                return false;
            }
            Doable indexedDoable = undoList.get(index);
            return (indexedDoable.getCompoundId() == doable.getCompoundId());
        }
        
        public void undo() {
            if (canUndo()) {
                Doable doable;
                do {
                    --undoPosition;
                    doable = undoList.get(undoPosition);
                    //System.out.println("undo: " + doable);
                    doable.undo();
                } while (compoundContinuesAt(doable, undoPosition - 1));
                fireChangeListeners();
            }
        }
        
        public void redo() {
            if (canRedo()) {
                Doable doable;
                do {
                    doable = undoList.get(undoPosition);
                    ++undoPosition;
                    //System.out.println("redo: " + doable);
                    doable.redo();
                } while (compoundContinuesAt(doable, undoPosition));
                fireChangeListeners();
            }
        }
        
        public void addChangeListener(ChangeListener listener) {
            changeListeners.add(listener);
        }
        
        private void fireChangeListeners() {
            for (int i = changeListeners.size() - 1; i >= 0; --i) {
                changeListeners.get(i).stateChanged(new ChangeEvent(this));
            }
        }
    }
    
    public interface SelectionSetter {
        public void modifySelection();
    }
    
    private class Doable {
        private int compoundId;
        private SelectionSetter beforeCaret;
        private int position;
        private CharSequence removeChars;
        private CharSequence insertChars;
        private SelectionSetter afterCaret;
        
        public Doable(int compoundId, SelectionSetter beforeCaret, int position, CharSequence removeChars, CharSequence insertChars, SelectionSetter afterCaret) {
            this.compoundId = compoundId;
            this.beforeCaret = beforeCaret;
            this.position = position;
            this.removeChars = removeChars;
            this.insertChars = insertChars;
            this.afterCaret = afterCaret;
        }
        
        public boolean isNotCompound() {
            return (compoundId == Undoer.NOT_COMPOUND);
        }
        
        public int getCompoundId() {
            return compoundId;
        }
        
        public String toString() {
            return "Doable[compoundId=" + compoundId + ",position=" + position + ",removeChars=\"" + removeChars + "\",insertChars=\"" + insertChars + "\"]";
        }
        
        public void undo() {
            removeAndInsert(insertChars, removeChars);
            beforeCaret.modifySelection();
        }
        
        public void redo() {
            removeAndInsert(removeChars, insertChars);
            afterCaret.modifySelection();
        }
        
        private void removeAndInsert(CharSequence remove, CharSequence insert) {
            if (remove != null) {
                removeWithoutUndo(position, remove);
            }
            if (insert != null) {
                insertWithoutUndo(position, insert);
            }
        }
    }
}
