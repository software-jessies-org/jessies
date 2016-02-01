package e.edit;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.undo.*;

/**
 * DocumentUndoFilter aggregates the UndoableEditEvents from a Document.
 * The UndoableEditEvents that are generated from this class can contain
 * any number of key strokes that originate at the same position. A time
 * delay can also be set so that key strokes are aggregated until a certain
 * timeout has been reached.
 * <p>This works by creating a new UndoableEdit, KeyStrokeUndo. It will
 * contain the DocumentEvents that are generated from the Document. Every time
 * a new UndoableEdit is received by the UndoManager the UndoManager will
 * message the last UndoableEdit (in this case a KeyStrokeUndo) if it can
 * add the edit. KeyStrokeUndo will then add the edit to itself given certain
 * constraints (such as max number of key strokes, time outs, character 
 * position...). In this manner KeyStrokeUndo will aggregate a number of edits
 * and make undo behave on muliple key strokes.
 * 
 * @author Scott Violet <scott.violet@sun.com>
 */
public class DocumentUndoFilter {
    /** Document listening on undo events from. */
    protected Document document;

    /** TextComponent we've created a key listener for. */
    protected JTextComponent textComponent;

    /** Actual listener on the document. */
    protected UndoableEditListener listener;

    /** Amount of time, in miliseconds, between keys. */
    protected int keyTimeout;

    /** The maximum number of edits that can be merged into one. */
    protected int maxEditCount;

    /** Whether to ignore UndoableEdits corresponding to style changes. */
    private boolean ignoreStyleChanges;

    /**
     * Maintains the list of UndoableEditListeners that are interested we
     * forward edits to.
     */
    protected EventListenerList listenerList;

    /** Set to true when a KeyStrokeUndo is undo()ing or redo()ing. */
    private boolean isUndoing;
 
    public DocumentUndoFilter() {
        setKeyTimeout(0);
        setIgnoreStyleChanges(true);
        setMaxEditCount(-1);
        listener = new UL();
        listenerList = new EventListenerList();
    }

    /**
     * Creates a DocumentUndoFilter listening to the specified component.
     * Be sure and invoke <code>setTextComponent</code> if the Document
     * changes.
     */
    public DocumentUndoFilter(JTextComponent component) {
        this();
        setTextComponent(component);
    }

    /**
     * Sets whether or not style changes are ignored. It's useful to be
     * able to ignore style changes in editors where styles are under
     * computer control (a syntax-coloring programmer's editor, for example).
     * A rich text editor where the user can choose styles should invoke
     * this method with the parameter false. The default is true.
     */
    public void setIgnoreStyleChanges(boolean ignore) {
        ignoreStyleChanges = ignore;
    }

    /** Returns true if style changes are being ignored. */
    public boolean getIgnoreStyleChanges() {
        return ignoreStyleChanges;
    }

    /**
     * Sets the text component that will be used to get edits from.
     * If you change the Document of the text component you will need
     * to invoke this method again!
     */
    public void setTextComponent(JTextComponent component) {
        // remove current listeners
        if (document != null) {
            document.removeUndoableEditListener(listener);
        }

        this.textComponent = component;

        // add new listeners
        if(component != null) {
            document = component.getDocument();
            document.addUndoableEditListener(listener);
        } else {
            document = null;
        }
    }

    /**
     * Returns the JTextComponent that events are filtered from.
     */
    public JTextComponent getTextComponent() {
        return textComponent;
    }

    /**
     * Sets the amount of time between keystrokes until an edit is
     * considered significant to time, which is in miliseconds. A value
     * 0 implies the time is not considered between edits.
     */
    public void setKeyTimeout(int time) {
        keyTimeout = time;
    }

    /**
     * Returns the amount of time, in miliseconds between keystrokes
     * until an edit is considered significant.
     */
    public int getKeyTimeout() {
        return keyTimeout;
    }

    /**
     * Set the number of individual edits that will be grouped together
     * as a single undo. If count is less than one there is no limit.
     * The default is unlimited.
     */
    public void setMaxEditCount(int count) {
        if (count < 0) {
            count = Integer.MAX_VALUE;
        }
        maxEditCount = count;
    }

    /**
     * Returns the number of individual edits that will be grouped together
     * as a single undo.
     */
    public int getMaxEditCount() {
        return maxEditCount;
    }

    /**
     * Adds l to the list of UndoableEditListeners that are
     * to be notified when an UndoableEditEvent is generated.
     */
    public void addUndoableEditListener(UndoableEditListener l) {
        listenerList.add(UndoableEditListener.class, l);
    }

    /**
     * Removes l from the list of UndoableEditListeners that are
     * to be notified when an UndoableEditEvent is generated.
     */
    public void removeUndoableEditListener(UndoableEditListener l) {
        listenerList.remove(UndoableEditListener.class, l);
    }

    /**
     * Messages the listeners that an undo event happened.
     */
    protected void fireUndoableEditEvent(UndoableEditEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length-2; i>=0; i-=2) {
            if (listeners[i] == UndoableEditListener.class) {
                ((UndoableEditListener)listeners[i+1]).undoableEditHappened(e);
            }
        }
    }

    /**
     * Messaged when a new undoable event is generated from the Document.
     */
    protected void handleEvent(UndoableEditEvent e) {
        UndoableEdit edit = e.getEdit();
        
        if (ignoreStyleChanges && edit instanceof DocumentEvent) {
            DocumentEvent docEvent = (DocumentEvent) edit;
            if (docEvent.getType() == DocumentEvent.EventType.CHANGE) {
                return;
            }
        }
        
        if (edit instanceof DocumentEvent) {
            fireUndoableEditEvent(new UndoableEditEvent(e.getSource(), new KeyStrokeUndo(e.getEdit())));
        } else {
            fireUndoableEditEvent(e);
        }
    }


    /**
     * Class to implement the listening on the document. When an
     * event is received handleEvent is invoked.
     */
    private class UL implements UndoableEditListener {

        /**
         * Invoked when an event from the document has been received,
         * messages handleEvent.
         */
        public void undoableEditHappened(UndoableEditEvent e) {
            handleEvent(e);
        }
    }

    /**
     * A KeyStrokeUndo is used to group similiar edits together to be
     * undone as a single unit.<p>
     * A KeyStrokeUndo is initialy created from an undo event generated
     * from a Document. The information contained in the intial edit, as
     * well as information contained in the DocumentUndoFilter this
     * was created from will be used to determine if an edit can be added.
     * An edit can be added to the receiver if it is of the same type,
     * a timeout as determined by the DocumentUndoFilter has not
     * elapsed, the max number of characters has not been reached, and the edit
     * occured at the same location in the document.<p>
     * NOTE: this subclasses AbstractUndoableEdit instead of CompoundEdit
     * because it needs to be able to be undone, but also able to add edits
     * at a certain point (inProgress is package private).
     */
    private class KeyStrokeUndo extends AbstractUndoableEdit {
        /**
         * Children edits, first one will always be the event from
         * the Document, and the remaining will be KeyStrokeUndos.
         */
        protected Vector edits;

        /** true if new edits can be added. */
        protected boolean accepting;

        /**
         * Type of event the receiver will accept. This is set when first
         * edit is added.
         */
        protected DocumentEvent.EventType eventType;

        /**
         * True if this undo is a DocumentEvent.EventType.REMOVE and
         * the type of deletion is forward.
         */
        protected boolean isForwardDelete;

        /** Time this event was created. */
        protected long eventTime;

        public KeyStrokeUndo(UndoableEdit edit) {
            super();
            isForwardDelete = false;
            edits = new Vector();
            accepting = true;
            eventTime = System.currentTimeMillis();
            addEdit(edit);
        }

        /**
         * Undo the edit that was made.
         */
        public void undo() throws CannotUndoException {
            boolean wasUndoing = isUndoing;

            super.undo();
            try {
                isUndoing = true;
                accepting = false;
                for (int counter = getEditCount() - 1; counter >= 0; counter--) {
                    getEdit(counter).undo();
                }
                if (!wasUndoing) {
                    resetSelection(true);
                    isUndoing = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new CannotUndoException();
            }
        }

        /**
         * Re-apply the edit, assuming that it has been undone.
         */
        public void redo() throws CannotRedoException {
            boolean wasUndoing = isUndoing;

            super.redo();
            try {
                isUndoing = true;
                accepting = false;
                for (int counter = 0, maxCounter = getEditCount(); counter < maxCounter; counter++) {
                    getEdit(counter).redo();
                }
                if (!wasUndoing) {
                    resetSelection(false);
                    isUndoing = false;
                }
            } catch (Exception e) {
                throw new CannotRedoException();
            }
        }

        /**
         * May be sent to inform an edit that it should no longer be
         * used. This is a useful hook for cleaning up state no longer
         * needed once undoing or redoing is impossible--for example,
         * deleting file resources used by objects that can no longer be
         * undeleted. UndoManager calls this before it dequeues edits.
         *
         * Note that this is a one-way operation. There is no "undie"
         * method.
         *
         * @see CompoundEdit#die
         */
        public void die() {
            super.die();
            for (int counter = getEditCount() - 1; counter >= 0; counter--) {
                getEdit(counter).die();
            }
            // Dump the edits.
            edits = null;
        }

        /**
         * Returns true if <code>anEdit</code> can be grouped with the
         * receiver. Grouping is done if the other edit is an KeyStrokeUndo
         * and represents the same type of edit (that is they are both
         * inserts, or both removes) a time delay has not expired, and a max
         * number of keys has not been exceeded. If false is returned, the
         * receiver is marked as not allowing any similiar edits to be added.
         */
        public boolean addEdit(UndoableEdit anEdit) {
            if (accepting) {
                if (getEditCount() == 0) {
                    JTextComponent text = getTextComponent();

                    reallyAddEdit(anEdit);
                    eventType = ((DocumentEvent)anEdit).getType();
                    // If this is more than one character (probably a
                    // remove), or the event type is a change
                    // make it not accept anything else.
                    if(getCharCount() > 1 || eventType == DocumentEvent.EventType.CHANGE) {
                        accepting = false;
                    }
                    return true;
                } else if (anEdit instanceof KeyStrokeUndo) {
                    KeyStrokeUndo keyUndo = (KeyStrokeUndo)anEdit;

                    // make sure the edit is similiar.
                    if (keyUndo.getDocument() == getDocument() && keyUndo.getType() == getLastType() && keyUndo.getCharCount() == 1 && (getKeyTimeout() == 0 || (keyUndo.getEventTime() - getEventTime()) < (long)getKeyTimeout()) && offsetsMatch(keyUndo)) {
                        boolean isNewLine;

                        // Make sure the new new one isn't a newline, if it
                        // is, don't let it through.
                        try {
                            isNewLine = ((getType() == DocumentEvent.EventType.INSERT) && (getTextComponent().getText(keyUndo.getOffset(), 1).charAt(0) == '\n'));
                        } catch (BadLocationException ble) {
                            isNewLine = false;
                        }
                        if (!isNewLine) {
                            reallyAddEdit(anEdit);
                            // Mark it so that the next one isn't accepted
                            // if the max number of keys have been received.
                            if (getEditCount() >= getMaxEditCount() || keyUndo.getCharCount() > 1) {
                                accepting = false;
                            }
                            return true;
                        }
                    }
                }
                accepting = false;
            }
            return false;
        }

        /**
         * Always returns true.
         */
        public boolean isSignificant() {
            return true;
        }

        /**
         * Provide a localized, human readable description of this edit
         * suitable for use in, say, a change log.
         */
        public String getPresentationName() {
            if (getEditCount() > 0) {
                return getEdit(0).getPresentationName();
            }
            return "";
        }

        /**
         * Provide a localized, human readable description of the undoable
         * form of this edit, e.g. for use as an Undo menu item. Typically
         * derived from getDescription();
         */
        public String getUndoPresentationName() {
            if (getEditCount() > 0) {
                return getEdit(0).getUndoPresentationName();
            }
            return "";
        }

        /**
         * Provide a localized, human readable description of the redoable
         * form of this edit, e.g. for use as a Redo menu item. Typically
         * derived from getPresentationName();
         */
        public String getRedoPresentationName() {
            if (getEditCount() > 0) {
                return getEdit(0).getRedoPresentationName();
            }
            return "";
        }


        //
        // Local methods
        //

        /**
         * Returns true if the offset in <code>undo</code> matches
         * the next logical place the character should be placed at.
         */
        protected boolean offsetsMatch(KeyStrokeUndo undo) {
            if (isForwardDelete) {
                return (getOffset() == undo.getOffset());
            }
            if (getEditCount() == 1 && getType() == DocumentEvent.EventType.REMOVE && getOffset() == undo.getOffset()) {
                isForwardDelete = true;
                return true;
            }
            return (getNextOffset() == undo.getOffset());
        }

        /**
         * Resets the selection of the text component based on the type
         * event. <code>didUndo</code> is true if the receiver has been
         * undone.
         */
        protected void resetSelection(boolean didUndo) {
            JTextComponent text = getTextComponent();

            if (text != null && getOffset() != -1) {
                DocumentEvent.EventType type = getType();
                int selStart;

                if (type == DocumentEvent.EventType.INSERT) {
                    if (didUndo) {
                        selStart = getOffset();
                    } else {
                        selStart = getOffset() + getCharCount();
                    }
                } else if (type == DocumentEvent.EventType.REMOVE) {
                    if (isForwardDelete) {
                        selStart = getOffset();
                    } else if (didUndo) {
                        selStart = getOffset() + ((DocumentEvent)getEdit(0)).getLength();
                    } else {
                        selStart = getOffset() - getEditCount() + 1;
                    }
                } else {
                    selStart = getOffset();
                }
                text.select(selStart, selStart);
            }
        }

        /**
         * Returns the offset of the first edit.
         */
        protected int getOffset() {
            if (getEditCount() > 0) {
                return ((DocumentEvent)getEdit(0)).getOffset();
            }
            return -1;
        }

        /**
         * Returns where the next matching edits offset should be. For
         * inserts this is the current offset plus the number of edits,
         * for removal this is the current offset minus the number of
         * edits.
         */
        protected int getNextOffset() {
            if (getType() == DocumentEvent.EventType.INSERT) {
                return getOffset() + getEditCount();
            } else if (getType() == DocumentEvent.EventType.REMOVE) {
                return getOffset() - getEditCount();
            }
            return -2;
        }

        /**
         * Returns the number of characters in the first undo, which is
         * an instance of DocumentEvent.
         */
        protected int getCharCount() {
            int retValue = 0;
            int numEdits = getEditCount();

            if (numEdits > 0) {
                retValue += ((DocumentEvent)getEdit(0)).getLength();
                for (int counter = 1; counter < numEdits; counter++) {
                    retValue += ((KeyStrokeUndo)getEdit(counter)).getCharCount();
                }
            }
            return retValue;
        }

        /**
         * Returns the type of the last edit. This will only differ from
         * getType if this edit was created for a replace.
         */
        protected DocumentEvent.EventType getLastType() {
            int eCount = getEditCount();

            if (eCount <= 1) {
                return getType();
            }
            return ((KeyStrokeUndo)getEdit(eCount - 1)).getType();
        }

        /**
         * Type of event.
         */
        protected DocumentEvent.EventType getType() {
            return eventType;
        }

        /**
         * Returns the document the receiver was created for. This can
         * differ from the Document of the enclosing DocumentUndoFilter.
         */
        protected Document getDocument() {
            if (getEditCount() > 0) {
                return ((DocumentEvent)getEdit(0)).getDocument();
            }
            return null;
        }

        /**
         * Time the event occured at.
         */
        protected long getEventTime() {
            int eCount = getEditCount();

            if (eCount > 1) {
                return ((KeyStrokeUndo)getEdit(eCount - 1)).getEventTime();
            }
            return eventTime;
        }

        /**
         * Adds the edit to the receiver, regardless.
         */
        protected void reallyAddEdit(UndoableEdit edit) {
            edits.addElement(edit);
        }

        /**
         * Returns the number of edits the receiver contains.
         */
        protected int getEditCount() {
            return edits.size();
        }

        /**
         * Returns the UndoableEdit at the particular index
         * <code>index</code>.
         */
        protected UndoableEdit getEdit(int index) {
            return (UndoableEdit) edits.elementAt(index);
        }

        public String toString() {
            return super.toString() + " type " + getType() + " edit count " + getEditCount() + " char count " + getCharCount() + " acc " + accepting;
        }
    }
}
