package e.ptextarea;


import java.lang.ref.*;
import java.util.*;

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
    private ArrayList anchors = new ArrayList();
    
    public void add(PAnchor anchor) {
        anchors.add(getFirstAnchorIndex(anchor.getIndex()), anchor);
    }
    
    public void remove(PAnchor anchor) {
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
        checkLinearity();    // Comment this out to improve speed, but remove warnings when our state goes wrong.
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
        return (PAnchor) anchors.get(index);
    }
    
    public void textInserted(PTextEvent event) {
        int start = getFirstAnchorIndex(event.getOffset());
        int insertionLength = event.getLength();
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            anchor.setIndex(anchor.getIndex() + insertionLength);
        }
    }
    
    public void dumpAnchorIndices() {
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            System.err.println("  Anchor " + i + ": " + anchor);
        }
    }
    
    public void textRemoved(PTextEvent event) {
        LinkedList deletionList = new LinkedList();
        int start = getFirstAnchorIndex(event.getOffset());
        int deletionLength = event.getLength();
        int deletionEnd = event.getOffset() + event.getLength();
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor.getIndex() < deletionEnd) {
                deletionList.add(anchor);
                anchors.remove(i--);
            } else {
                anchor.setIndex(anchor.getIndex() - deletionLength);
            }
        }
        for (int i = 0; i < deletionList.size(); i++) {
            ((PAnchor) deletionList.get(i)).delete();
        }
    }
    
    public void textCompletelyReplaced(PTextEvent event) {
        clear();
    }
    
    public void clear() {
        checkLinearity();
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor != null) {
                anchor.delete();
            }
        }
        anchors.clear();
    }
    
    private class AnchorComparator implements Comparator {
        public int compare(Object obj1, Object obj2) {
            PAnchor anchor1 = (PAnchor) obj1;
            PAnchor anchor2 = (PAnchor) obj2;
            return (anchor1.getIndex() - anchor1.getIndex());
        }
    }
}
