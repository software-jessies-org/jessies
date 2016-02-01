package e.ptextarea;

import java.util.*;

public class PHighlightManager {
    private final Map<String, HighlightSet> highlighterSets = new LinkedHashMap<String, HighlightSet>();
    
    public synchronized int countHighlightsOfType(String highlighterName) {
        HighlightSet set = highlighterSets.get(highlighterName);
        return (set != null) ? set.size() : 0;
    }
    
    public synchronized void add(PHighlight highlight) {
        String highlighterName = highlight.getHighlighterName();
        if (highlighterSets.containsKey(highlighterName) == false) {
            highlighterSets.put(highlighterName, new HighlightSet());
        }
        highlighterSets.get(highlighterName).add(highlight);
    }
    
    public synchronized void remove(PHighlight highlight) {
        String highlighterName = highlight.getHighlighterName();
        if (highlighterSets.containsKey(highlighterName)) {
            highlighterSets.get(highlighterName).remove(highlight);
        }
    }
    
    /**
     * Returns all highlighters overlapping the range [beginOffset, endOffset).
     */
    public synchronized List<PHighlight> getHighlightsOverlapping(int beginOffset, int endOffset) {
        List<PHighlight> result = new ArrayList<PHighlight>();
        for (HighlightSet set : highlighterSets.values()) {
            result.addAll(set.getHighlightsOverlapping(beginOffset, endOffset));
        }
        return result;
    }
    
    /**
     * Returns all highlighters matching highlighterName overlapping the range [beginOffset, endOffset).
     */
    public synchronized List<PHighlight> getNamedHighlightsOverlapping(String highlighterName, int beginOffset, int endOffset) {
        HighlightSet set = highlighterSets.get(highlighterName);
        if (set != null) {
            return set.getHighlightsOverlapping(beginOffset, endOffset);
        } else {
            return Collections.emptyList();
        }
    }
    
    public synchronized PHighlight getNextOrPreviousHighlight(String highlighterName, boolean next, int offset) {
        HighlightSet set = highlighterSets.get(highlighterName);
        if (set == null) {
            return null;
        }
        return next ? set.getHighlightAfter(offset) : set.getHighlightBefore(offset);
    }
    
    private static class HighlightSet {
        private TreeSet<PHighlight> highlights = new TreeSet<PHighlight>();
        
        private void add(PHighlight highlight) {
            highlights.add(highlight);
        }
        
        private void remove(PHighlight highlight) {
            highlights.remove(highlight);
        }
        
        private int size() {
            return highlights.size();
        }
        
        private PHighlight getHighlightAfter(int offset) {
            SortedSet<PHighlight> after = highlights.tailSet(new ProbeHighlight(offset));
            return (after.size() == 0) ? null : after.first();
        }
        
        private PHighlight getHighlightBefore(int offset) {
            SortedSet<PHighlight> before = highlights.headSet(new ProbeHighlight(offset));
            return (before.size() == 0) ? null : before.last();
        }
        
        private List<PHighlight> getHighlightsOverlapping(int beginOffset, int endOffset) {
            // The 'firstItem' is to be the lowest-indexed highlight wrapper which *overlaps* the range.
            // We must check highlights which start <= beginOffset to determine if they end > beginOffset.
            PHighlight firstItem = new ProbeHighlight(beginOffset);
            SortedSet<PHighlight> highlightsBeforeStart = highlights.headSet(firstItem, true);
            if (highlightsBeforeStart.size() > 0) {
                PHighlight lastBefore = highlightsBeforeStart.last();
                if (lastBefore.getEndIndex() > beginOffset) {
                    firstItem = lastBefore;
                }
            }
            
            // Now we have the start, and the end too, so we can simply grab the subset between these two extremes (inclusive of firstItem) and return as a list.
            SortedSet<PHighlight> highlightsInRange = highlights.subSet(firstItem, new ProbeHighlight(endOffset));
            List<PHighlight> result = new ArrayList<PHighlight>(highlightsInRange);
            return result;
        }
    }
    
    private static final class ProbeHighlight extends PHighlight {
        private final int fixedOffset;
        
        private ProbeHighlight(int fixedOffset) {
            super();
            this.fixedOffset = fixedOffset;
        }
        
        @Override public int getStartIndex() {
            return fixedOffset;
        }
        
        public String getHighlighterName() {
            throw new UnsupportedOperationException();
        }
        
        protected void paintHighlight(java.awt.Graphics2D g, PCoordinates start, PCoordinates end, java.awt.Insets insets, int lineHeight, int firstLineIndex, int lastLineIndex) {
            throw new UnsupportedOperationException();
        }
    }
}
