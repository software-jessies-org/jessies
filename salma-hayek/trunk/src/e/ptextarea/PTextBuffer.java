package e.ptextarea;

import e.util.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * The PTextArea's model. It can be used as a CharSequence, for efficient
 * access to the underlying characters. Try to avoid use of 'toString' for
 * maximum efficiency.
 * 
 * The 'replace' method is the only way to change the document. PTextArea
 * offers various other convenience methods. You can be notified of changes
 * via 'addTextListener' (see also 'removeTextListener').
 * 
 * This class is responsible for maintaining an undo buffer, accessible via
 * 'getUndoBuffer'.
 * 
 * In addition to the characters, this class also stores metadata, called
 * properties. There are certain well-known properties, whose names are
 * exported as constants. No caller should cache these properties; a caller
 * that sets a property should expect it to take immediate effect (though
 * 'immediate' may not be meaningful until the text is next written to disk,
 * as in the case of the character encoding). See 'getProperty' and
 * 'setProperty'.
 * 
 * When a file is read from disk with 'readFromFile', we recognize UTF-16BE
 * and UTF-16LE, and -- in the absence of either of their byte-order marks, we
 * fall back to UTF-8. If the file doesn't decode correctly, we try ISO-8859-1.
 * If that also fails, we throw an exception.
 * 
 * A file written to disk with 'writeToFile' will use the current
 * CHARSET_PROPERTY, which will have been initialized to correspond to the
 * encoding the file had when read from disk, or UTF-8 if this is a new file.
 * 
 * @author Elliott Hughes
 * @author Phil Norman
 */
public class PTextBuffer implements CharSequence {
    public static final String CHARSET_PROPERTY = "CharsetProperty";
    public static final String INDENTATION_PROPERTY = "IndentationProperty";
    public static final String LINE_ENDING_PROPERTY = "LineEndingProperty";
    
    private static final int MIN_BUFFER_EXTENSION = 100;
    private static final int MAX_GAP_SIZE = 1024 * 2;
    
    private char[] text = new char[0];
    private int gapPosition;
    private int gapLength;
    private ArrayList<PTextListener> textListeners = new ArrayList<PTextListener>();
    private PAnchorSet anchorSet = new PAnchorSet();
    private Undoer undoBuffer = new Undoer();
    private HashMap<String, Object> properties = new HashMap<String, Object>();
    private PLock lock = new PLock();
    
    public PTextBuffer() {
        // Our anchorSet *must* be the first listener.  It needs to update the anchor locations
        // before anyone else starts messing about with them.
        addTextListener(anchorSet);
        initDefaultProperties();
    }
    
    /**
     * Returns the lock object for this buffer.  This should only be used within this package.
     */
    PLock getLock() {
        return lock;
    }
    
    private void initDefaultProperties() {
        putProperty(CHARSET_PROPERTY, "UTF-8");
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
        synchronized (textListeners) {
            textListeners.add(listener);
        }
    }
    
    /**
     * Remove a previously added listener.
     */
    public void removeTextListener(PTextListener listener) {
        synchronized (textListeners) {
            textListeners.remove(listener);
        }
    }
    
    private void fireTextEvent(PTextEvent event) {
        // Although most Swing listeners are called in reverse order, these events are not
        // consume()able, so I don't really see the point in doing so here.  It is vital that
        // some listeners (the PLineList and PAnchorSet to name two) are called before
        // anyone else, to maintain internal state properly.  Therefore this loop will go
        // forwards through the listener list until someone comes up with a really good
        // reason why backwards is better (at which point I'll add an extra 'internalListeners'
        // list).
        synchronized (textListeners) {
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
    }
    
    /**
     * Replaces the contents of this buffer with the entire contents of 'file'.
     */
    public void readFromFile(File file) {
        getLock().getWriteLock();
        try {
            // Read all the bytes in.
            ByteBuffer byteBuffer = ByteBufferUtilities.readFile(file);
            
            // Turn the raw bytes into a char[].
            final byte[] bytes = byteBuffer.array();
            final int byteCount = bytes.length;
            String encodingName = null;
            char[] chars;
            if (isAllAscii(bytes)) {
                // Most files will be plain ASCII, and we can "decode" them 5x cheaper with just a cast.
                // That speed-up includes the added cost of checking to see if the byte[] only contains ASCII.
                // FIXME: we could make a further slight saving by checking for '\r' at the same time.
                encodingName = "UTF-8";
                chars = new char[byteCount];
                for (int i = 0; i < byteCount; ++i) {
                    chars[i] = (char) bytes[i];
                }
            } else {
                ByteBufferDecoder decoder = new ByteBufferDecoder(byteBuffer, byteCount);
                chars = decoder.getCharArray();
                encodingName = decoder.getEncodingName();
            }
            
            chars = fixLineEndings(chars);
            putProperty(CHARSET_PROPERTY, encodingName);
            setText(chars);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    // Tests whether all the given bytes could be ASCII.
    private static boolean isAllAscii(byte[] bytes) {
        for (int i = 0; i < bytes.length; ++i) {
            byte b = bytes[i];
            // FIXME: this range is a little bit arbitrary, but excluding NUL and DEL and anything with the top bit set seems reasonable.
            if (b < 1 || b > 126) {
                return false;
            }
        }
        return true;
    }
    
    private char[] fixLineEndings(char[] chars) {
        String lineEnding = "\n";
        if (charArrayContains(chars, '\r')) {
            String s = new String(chars);
            if (s.contains("\r\n")) {
                lineEnding = "\r\n";
                s = s.replaceAll("\r\n", "\n");
            } else {
                lineEnding = "\r";
                s = s.replaceAll("\r", "\n");
            }
            chars = s.toCharArray();
        }
        putProperty(LINE_ENDING_PROPERTY, lineEnding);
        return chars;
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
        FileOutputStream openFile = null;
        try {
            openFile = new FileOutputStream(file);
            String charsetName = (String) getProperty(CHARSET_PROPERTY);
            // The CharsetEncoder created here will silently replace characters which cannot
            // be encoded with question marks.
            // This will currently happen if, for example, you have a file "recognized" as ISO-8859-1
            // into which you paste a UTF-8 character which isn't Latin1.
            writeToStream(new OutputStreamWriter(openFile, charsetName));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            FileUtilities.close(openFile);
        }
    }
    
    /**
     * Switch charset encoding if the encoding works.
     */
    public boolean attemptEncoding(String charsetName) {
        try {
            CharsetEncoder charsetEncoder = makeReportingCharsetEncoder(charsetName);
            writeToStream(new OutputStreamWriter(new NullOutputStream(), charsetEncoder));
            putProperty(CHARSET_PROPERTY, charsetName);
            return true;
        } catch (Exception ex) {
            // FIXME: Present encoding errors to the user in a more palatable fashion.
            // Currently, this just says:
            // java.lang.RuntimeException: java.nio.charset.UnmappableCharacterException: Input length = 1
            // UnmappableCharacterException doesn't appear to have stashed any other, more interesting information.
            // One idea would be to maintain a set of unique characters used in a buffer,
            // to discover which of those characters cannot be encoded in the desired encoding
            // and to highlight them, perhaps as Find matches.
            Log.warn("Failed to encode buffer in charset " + charsetName, ex);
            return false;
        }
    }
    
    private static CharsetEncoder makeReportingCharsetEncoder(String charsetName) {
        // CharsetEncoder is reporting by default.
        return Charset.forName(charsetName).newEncoder();
    }
    
    private void writeToStream(OutputStreamWriter outputStreamWriter) {
        getLock().getReadLock();
        try {
            Writer writer = new BufferedWriter(outputStreamWriter);
            
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
                for (String line : lines) {
                    writer.write(line);
                    writer.write(lineEnding);
                }
            }
            writer.flush();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            getLock().relinquishReadLock();
        }
    }
    
    /**
     * Sets the text, replacing anything that was here before.
     * Note that this method does not copy the given char[].
     */
    private void setText(char[] text) {
        getLock().getWriteLock();
        try {
            this.text = text;
            gapPosition = 0;
            gapLength = 0;
            fireTextEvent(new PTextEvent(this, PTextEvent.COMPLETE_REPLACEMENT, 0, new CharArrayCharSequence(text)));
        } finally {
            getLock().relinquishWriteLock();
        }
    }
    
    /**
     * Returns a copy of the specified region of text.  This involves making a
     * complete copy of the specified region of text, and so should only be
     * used if you need to keep the copy unchanged in the face of future edits.
     * If not, use the CharSequence interface instead.
     */
    private CharSequence copyChars(int start, int charCount) {
        return new CharArrayCharSequence(copyCharArray(start, charCount));
    }
    
    /**
     * Like copyChars for users who need a char[] and want to avoid an extra copy.
     */
    private char[] copyCharArray(int start, int charCount) {
        getLock().getReadLock();
        try {
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
                Log.warn("Requested get text from " + start + ", length " + charCount + "; size is " + length() +".", ex);
            }
            return result;
        } finally {
            getLock().relinquishReadLock();
        }
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
        getLock().getWriteLock();
        try {
            if (beforeCaret == null) {
                throw new IllegalArgumentException("beforeCaret must not be null");
            }
            if (afterCaret == null) {
                throw new IllegalArgumentException("afterCaret must not be null");
            }
            CharSequence removeChars = (removeCount == 0) ? null : copyChars(position, removeCount);
            if (add != null && add.length() == 0) {
                add = null;
            }
            undoBuffer.addAndDo(beforeCaret, position, removeChars, add, afterCaret);
        } finally {
            getLock().relinquishWriteLock();
        }
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
            throwIOOBE(index);
        }
        return (index < gapPosition) ? text[index] : text[index + gapLength];
    }
    
    private void throwIOOBE(int index) {
        throw new IndexOutOfBoundsException("index " + index + " not in half-open range [0.." + length() + ")");
    }
    
    /**
     * Returns the index within this buffer of the first occurrence of 'ch', starting the search at 'startIndex'.
     * 
     * If 'startIndex' is negative, it has the same effect as if it were zero: the entire buffer may be searched.
     * If 'startIndex' is greater than the length of the buffer, it has the same effect as if it were equal to the length of the string: -1 is returned.
     */
    public int indexOf(char ch, int startIndex) {
        // This is how String.indexOf behaves.
        if (startIndex < 0) {
            startIndex = 0;
        } else if (startIndex >= length()) {
            return -1;
        }
        
        int gapBufferIndex = (startIndex < gapPosition) ? startIndex : (startIndex + gapLength);
        for (int i = startIndex; i < length(); ++i) {
            if (text[gapBufferIndex++] == ch) {
                return i;
            }
        }
        return -1;
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
        getLock().getReadLock();
        try {
            StringBuilder result = new StringBuilder();
            result.append(text, 0, gapPosition);
            result.append(text, gapPosition + gapLength, text.length - gapPosition - gapLength);
            return result.toString();
        } finally {
            getLock().relinquishReadLock();
        }
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
            return new String(copyCharArray(start, end - start));
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
        }
        
        private void dumpUndoList() {
            Log.warn("Dumping PTextBuffer undo list:");
            int i = 0;
            for (Doable edit : undoList) {
                Log.warn(i++ + ": " + edit);
            }
        }
        
        public void finishCompoundEdit() {
            if (compoundingDepth == 0) {
                dumpUndoList();
                throw new IllegalStateException("can't finish a compound edit when there isn't one active");
            }
            --compoundingDepth;
            ++compoundId;
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
                getLock().getWriteLock();
                try {
                    Doable doable;
                    do {
                        --undoPosition;
                        doable = undoList.get(undoPosition);
                        doable.undo();
                    } while (compoundContinuesAt(doable, undoPosition - 1));
                } finally {
                    getLock().relinquishWriteLock();
                }
                fireChangeListeners();
            }
        }
        
        public void redo() {
            if (canRedo()) {
                getLock().getWriteLock();
                try {
                    Doable doable;
                    do {
                        doable = undoList.get(undoPosition);
                        ++undoPosition;
                        doable.redo();
                    } while (compoundContinuesAt(doable, undoPosition));
                } finally {
                    getLock().relinquishWriteLock();
                }
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
