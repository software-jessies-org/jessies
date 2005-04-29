package e.ptextarea;


/**
 * A PAnchorRegion is a region defined between two anchors.  It is automatically deleted
 * when either of its PAnchor end points is destroyed.
 * 
 * @author Phil Norman
 */

public class PAnchorRegion {
    private PAnchor start;
    private PAnchor end;
    
    public PAnchorRegion(PAnchorSet anchorSet, int startIndex, int endIndex) {
        if (endIndex < startIndex) {
            throw new IndexOutOfBoundsException("The end index (" + endIndex + ") may not be less than the start index (" + startIndex + ")");
        }
        start = new RegionAnchor(startIndex);
        end = new RegionAnchor(endIndex);
        anchorSet.add(start);
        anchorSet.add(end);
    }
    
    /** Returns the PAnchor defining the start of this region. */
    public PAnchor getStart() {
        return start;
    }
    
    /** Returns the PAnchor defining the end of this region. */
    public PAnchor getEnd() {
        return end;
    }
    
    public int getStartIndex() {
        return start.getIndex();
    }
    
    public int getEndIndex() {
        return end.getIndex();
    }
    
    public String toString() {
        return "PAnchorRegion[start=" + start + ",end=" + end + "]";
    }
    
    /** Notification that either of the start and end anchors has been deleted. */
    public void delete() { }
    
    private class RegionAnchor extends PAnchor {
        public RegionAnchor(int index) {
            super(index);
        }
        
        public void delete() {
            PAnchorRegion.this.delete();
        }
    }
}
