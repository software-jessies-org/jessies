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
        int index = getFirstAnchorIndex(anchor.getIndex());
        anchors.add(index, anchor);
    }
    
    /**
     * Bulk remove.
     * Assumes you're trying to remove "most" of the anchors, perhaps when canceling a find.
     */
    public synchronized void removeAll(SortedSet<PAnchor> deadAnchors) {
        // When we can use java 1.6, we should accept a NavigableSet and iterate
        // backwards through it.  For now, we have to sort it backwards instead.
        // Important note: we can't use normal collections 'contains' methods and
        // the like, because they use '.equals' to check for equality.  Since we
        // may have two anchors which are '.equal' to each other (ie they have the
        // same index) but are not the same, we must use the '==' equality metric
        // in order to get things right.
//        Log.warn("Start of 'removeAll'");
//        dumpAnchorIndices();
        ArrayList<PAnchor> reverseDead = new ArrayList<PAnchor>(deadAnchors);
        Collections.reverse(reverseDead);
        // Uncomment and use the following line when we can accept a NavigableSet.
        //Iterator<PAnchor> deadIterator = deadAnchors.descendingIterator();
        Iterator<PAnchor> deadIterator = reverseDead.iterator();
        PAnchor nextDeadAnchor = deadIterator.hasNext() ? deadIterator.next() : null;
        for (int i = anchors.size() - 1; (i >= 0) && (nextDeadAnchor != null); --i) {
            PAnchor anchor = anchors.get(i);
            while (anchor.getIndex() < nextDeadAnchor.getIndex()) {
                nextDeadAnchor = deadIterator.hasNext() ? deadIterator.next() : null;
                if (nextDeadAnchor == null) {
                    break;
                }
            }
            if (nextDeadAnchor == null) {
                break;
            }
            if (anchor == nextDeadAnchor) {
                anchors.remove(i);
            }
        }
//        Log.warn("End of 'removeAll'");
//        dumpAnchorIndices();
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
        // This is very inefficient, being O(removeCount * (anchors.size() - firstAnchorIndex - deletionLength).
        // It'd be better to do this in the following steps:
        // 1: Copy the to-delete set of anchors into a new list.
        // 2: Move all the elements in 'anchors' towards the start by removeCount positions.
        // 3: Shorten anchors by removeCount.
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
        ArrayList<PAnchor> oldAnchors = anchors;
        anchors = new ArrayList<PAnchor>();
        for (PAnchor anchor : oldAnchors) {
            anchor.anchorDestroyed();
        }
    }
}
