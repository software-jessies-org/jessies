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
        int addIndex = getAnchorIndex(anchor.getIndex());
        anchors.add(addIndex, new WeakReference(anchor));
    }
    
    // This may remove dead items from the anchors list.
    private int getAnchorIndex(int textIndex) {
        int min = 0;
        int max = anchors.size();
        while (max - min > 1) {
            int mid = (min + max) / 2;
            PAnchor anchor = get(mid);
            if (anchor == null) {
                anchors.remove(mid);
                max -= 1;
            } else {
                if (anchor.getIndex() == textIndex) {
                    return mid;
                } else if (anchor.getIndex() > textIndex) {
                    max = mid;
                } else {
                    min = mid;
                }
            }
        }
        if (min < anchors.size()) {
            PAnchor anchor = get(min);
            if (anchor != null && anchor.getIndex() < textIndex) {
                min++;
            }
        }
        return min;
    }
    
    // May return null!
    private PAnchor get(int index) {
        Reference ref = (Reference) anchors.get(index);
        return (PAnchor) ref.get();
    }
    
    public void textInserted(PTextEvent event) {
        int start = getAnchorIndex(event.getOffset());
        int insertionLength = event.getLength();
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor == null) {
                anchors.remove(i);
                i--;  // Counteract the loop increment.
            } else {
                anchor.setIndex(anchor.getIndex() + insertionLength);
            }
        }
    }
    
    private void dumpAnchorIndices() {
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor == null) {
                System.err.println("Anchor " + i + ": null");
            } else {
                System.err.println("Anchor " + i + ": " + anchor.getIndex());
            }
        }
    }
    
    public void textRemoved(PTextEvent event) {
        int start = getAnchorIndex(event.getOffset());
        int deletionLength = event.getLength();
        int deletionEnd = event.getOffset() + event.getLength();
        for (int i = start; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor == null) {
                anchors.remove(i);
                i--;  // Counteract the loop increment.
            } else {
                if (anchor.getIndex() < deletionEnd) {
                    anchor.delete();
                    anchors.remove(i);
                    i--;  // Counteract the loop increment.
                } else {
                    anchor.setIndex(anchor.getIndex() - deletionLength);
                }
            }
        }
    }
    
    public void textCompletelyReplaced(PTextEvent event) {
        clear();
    }
    
    public void clear() {
        for (int i = 0; i < anchors.size(); i++) {
            PAnchor anchor = get(i);
            if (anchor != null) {
                anchor.delete();
            }
        }
        anchors.clear();
    }
}
