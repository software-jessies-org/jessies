package e.ptextarea;

import e.util.*;
import java.lang.ref.*;
import java.util.*;
import javax.swing.*;

/**
 * Contains all the PAnchor instances related to a given text buffer.
 * Responsible for ensuring that their offsets are updated when the text changes.
 */
public class PAnchorSet implements PTextListener {
    // This list is sorted so we can binarySearch it.
    private ArrayList<PAnchor> anchors = new ArrayList<PAnchor>();
    
    public synchronized void add(PAnchor anchor) {
        anchors.add(getFirstAnchorIndex(anchor.getIndex()), anchor);
    }
    
    /**
     * Bulk remove.
     * Assumes you're trying to remove "most" of the anchors, perhaps when canceling a find.
     */
    public synchronized void removeAll(Collection<PAnchor> deadAnchors) {
        // Sun 6529800: as of Java 7, this is significantly quicker than ArrayList.removeAll.
        // Sun's fix for that bug could perform even better than simple work-around, so if you're reading this in 2009 or later, think about removing this code.
        for (int i = anchors.size() - 1; i >= 0; --i) {
            if (deadAnchors.contains(anchors.get(i))) {
                anchors.remove(i);
            }
        }
    }
    
    public synchronized void remove(PAnchor anchor) {
        int start = getFirstAnchorIndex(anchor.getIndex());
        for (int i = start; i < anchors.size(); i++) {
            if (anchor == anchors.get(i)) {
                anchors.remove(i);
                return;
            }
            if (get(i).getIndex() > anchor.getIndex()) {
                break;
            }
        }
    }
    
    /**
     * Used as an argument to binarySearch.
     */
    private static class UnownedAnchor extends PAnchor {
        public UnownedAnchor(int index) {
            super(index);
        }
        
        // We don't add this to the anchor set, so it's never removed, so no-one needs to be notified of anything.
        @Override
        public void anchorDestroyed() {
        }
    }
    
    // Returns the first position where an anchor for the textIndex could be inserted
    // without violating the ordering.
    // This is what the STL calls "lower_bound".
    private int getFirstAnchorIndex(int textIndex) {
//        checkLinearity();    // Comment this out to improve speed, but remove warnings when our state goes wrong.
        int index = Collections.binarySearch(anchors, new UnownedAnchor(textIndex));
        if (index < 0) {
            return -index - 1;
        } else {
            while (index > 0) {
                if (get(index - 1).getIndex() < textIndex) {
                    break;
                } else {
                    index--;
                }
            }
            return index;
        }
    }
    
    private void checkLinearity() {
        int lastIndex = -1;
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor.getIndex() < lastIndex) {
                dumpAnchorIndices();
                throw new IllegalStateException("Linearity out of order at index " + i);
            }
            lastIndex = anchor.getIndex();
        }
    }
    
    private PAnchor get(int index) {
        return anchors.get(index);
    }
    
    public synchronized void textInserted(PTextEvent event) {
        int start = getFirstAnchorIndex(event.getOffset());
        int insertionLength = event.getLength();
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            anchor.setIndex(anchor.getIndex() + insertionLength);
        }
    }
    
    public synchronized void dumpAnchorIndices() {
        Log.warn("Dumping anchor indices:");
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            Log.warn("  Anchor " + i + ": " + anchor);
        }
    }
    
    private int countAnchorsInRegion(int firstAnchorIndex, int offset, int length) {
        int result = 0;
        int endOffset = offset + length;
        for (int i = firstAnchorIndex; i < anchors.size(); i++) {
            if (get(i).getIndex() < endOffset) {
                result++;
            } else {
                break;
            }
        }
        return result;
    }
    
    public synchronized void textRemoved(PTextEvent event) {
        int deletionLength = event.getLength();
        int firstAnchorIndex = getFirstAnchorIndex(event.getOffset());
        int removeCount = countAnchorsInRegion(firstAnchorIndex, event.getOffset(), deletionLength);
        List<PAnchor> anchorsToRemove = new LinkedList<PAnchor>();
        for (int i = 0; i < removeCount; i++) {
            anchorsToRemove.add(anchors.remove(firstAnchorIndex));
        }
        // Note that the sub-class of PAnchor in PHighlight relies upon this delete
        // call in order to properly destroy itself when one of its extremes is
        // removed.  If you delete this code, some highlights (notably 'find'
        // highlights) will turn into phantom highlights if you kill one end, such
        // that one extreme of the highlight will drift when changes are made to
        // the preceding text, and the other will be stably attached.
        for (PAnchor anchor : anchorsToRemove) {
            anchor.anchorDestroyed();
        }
        // We must recalculate the first anchor index, because the one we calculated
        // before could be wrong if an anchor's deletion caused the deletion of another.
        int start = getFirstAnchorIndex(event.getOffset());
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            anchor.setIndex(anchor.getIndex() - deletionLength);
        }
    }
    
    public synchronized void textCompletelyReplaced(PTextEvent event) {
        clear();
    }
    
    public synchronized void clear() {
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor != null) {
                anchor.anchorDestroyed();
            }
        }
        anchors.clear();
    }
}
