package e.ptextarea;

import java.util.*;

public class PHighlightManager {
    private Map<String, HighlightSet> highlighterSets = new LinkedHashMap<String, HighlightSet>();
    
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
    
    public synchronized List<PHighlight> getHighlights(int startOffset, int endOffset) {
        List<PHighlight> result = new ArrayList<PHighlight>();
        for (String highlighter : highlighterSets.keySet()) {
            result.addAll(getHighlights(highlighter, startOffset, endOffset));
        }
        return result;
    }
    
    public synchronized List<PHighlight> getHighlights(String highlighter, int startOffset, int endOffset) {
        if (highlighterSets.containsKey(highlighter)) {
            return highlighterSets.get(highlighter).getHighlights(startOffset, endOffset);
        } else {
            return Collections.emptyList();
        }
    }
    
    public synchronized PHighlight getNextOrPreviousHighlight(String highlighter, boolean next, int offset) {
        if (highlighterSets.containsKey(highlighter) == false) {
            return null;
        }
        HighlightSet set = highlighterSets.get(highlighter);
        return next ? set.getHighlightAfter(offset) : set.getHighlightBefore(offset);
    }
    
    private static class HighlightSet {
        private TreeSet<Wrapper> highlights = new TreeSet<Wrapper>();
        
        public void add(PHighlight highlight) {
            highlights.add(new Wrapper(highlight));
        }
        
        public void remove(PHighlight highlight) {
            highlights.remove(new Wrapper(highlight));
        }
        
        public int size() {
            return highlights.size();
        }
        
        public PHighlight getHighlightAfter(int offset) {
            SortedSet<Wrapper> after = highlights.tailSet(new ProbeWrapper(offset));
            return (after.size() == 0) ? null : after.first().get();
        }
        
        public PHighlight getHighlightBefore(int offset) {
            SortedSet<Wrapper> before = highlights.headSet(new ProbeWrapper(offset));
            return (before.size() == 0) ? null : before.last().get();
        }
        
        public List<PHighlight> getHighlights(int startOffset, int endOffset) {
            
            // The 'firstItem' is to be the lowest-indexed highlight wrapper which overlaps the range.
            // We must check the last item of those highlights which start before startOffset to determine
            // if it ends past startOffset.
            Wrapper firstItem = new ProbeWrapper(startOffset);
            SortedSet<Wrapper> highlightsBeforeStart = highlights.headSet(firstItem);
            if (highlightsBeforeStart.size() > 0) {
                Wrapper lastBefore = highlightsBeforeStart.last();
                if (lastBefore.get().getEndIndex() > startOffset) {
                    firstItem = lastBefore;
                }
            }
            
            // Now we have the start, and the end too, so we can simply grab the subset between
            // these two extremes (inclusive of firstItem) and return as a list.
            SortedSet<Wrapper> highlightsInRange = highlights.subSet(firstItem, new ProbeWrapper(endOffset));
            List<PHighlight> result = new ArrayList<PHighlight>(highlightsInRange.size());
            for (Wrapper wrapper : highlightsInRange) {
                result.add(wrapper.get());
            }
            return result;
        }
    }
    
    private static class ProbeWrapper extends Wrapper {
        private int fixedOffset;
        
        public ProbeWrapper(int fixedOffset) {
            super(null);
            this.fixedOffset = fixedOffset;
        }
        
        public int getOffset() {
            return fixedOffset;
        }
    }
    
    private static class Wrapper implements Comparable<Wrapper> {
        private PHighlight highlight;
        
        public Wrapper(PHighlight highlight) {
            this.highlight = highlight;
        }
        
        public boolean equals(Object obj) {
            return (obj instanceof Wrapper) && (((Wrapper) obj).getOffset() == getOffset());
        }
        
        public int compareTo(Wrapper other) {
            return getOffset() - other.getOffset();
        }
        
        public int getOffset() {
            return highlight.getStartIndex();
        }
        
        public PHighlight get() {
            return highlight;
        }
    }
}
