package e.ptextarea;


import java.lang.ref.*;
import java.util.*;
import javax.swing.*;

/**
 * A PAnchorSet contains a sorted list of anchors, referred to weakly so they automatically
 * die when no one holds a reference to them.
 *
 * The methods within this class have to be very careful to ensure they correctly deal
 * with the fact that objects referred to by weak references can go away at any time.
 * 
 * Consider forcing this listener to be the first one called by the PText's event dispatching
 * code.  Other code also listening to text events might assume that its PAnchors are already
 * properly updated.
 * 
 * @author Phil Norman
 */

public class PAnchorSet implements PTextListener {
    private ArrayList<PAnchor> anchors = new ArrayList<PAnchor>();
    
    public synchronized void add(PAnchor anchor) {
        anchors.add(getFirstAnchorIndex(anchor.getIndex()), anchor);
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
    
    // Returns the first position where an anchor for the textIndex could be inserted
    // without violating the ordering.
    // This is what the STL calls "lower_bound".
    private int getFirstAnchorIndex(int textIndex) {
//        checkLinearity();    // Comment this out to improve speed, but remove warnings when our state goes wrong.
        int index = Collections.binarySearch(anchors, new PAnchor(textIndex));
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
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            System.err.println("  Anchor " + i + ": " + anchor);
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
        for (PAnchor anchor : anchorsToRemove) {
            anchor.delete();
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
                anchor.delete();
            }
        }
        anchors.clear();
    }
}
