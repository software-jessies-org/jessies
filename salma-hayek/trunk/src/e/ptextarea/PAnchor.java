package e.ptextarea;


/**
 * A PAnchor is an index within a PText whose location is fixed not in terms of character
 * index, but rather in terms of the character it indexes.  If a PAnchor is defined as location
 * 100, and then 20 characters are inserted at location 50, that PAnchor's index will be
 * automatically updated to be 120.
 * In order to properly function, a PAnchor must have been added to a PAnchorSet which in
 * turn listens to the PText.
 * 
 * @author Phil Norman
 */

public class PAnchor implements Comparable {
    private int index;
    
    public PAnchor(int index) {
        this.index = index;
    }
    
    /** Returns the current index at which this anchor is anchored. */
    public int getIndex() {
        return index;
    }
    
    /** Changes the index at which this anchor is anchored. */
    public void setIndex(int index) {
        this.index = index;
    }
    
    /**
     * Notification that this anchor has been destroyed, because the character it indexes
     * has been deleted.
     */
    public void delete() { }
    
    public int hashCode() {
        return super.hashCode();
    }
    
    public boolean equals(Object obj) {
        return (index == ((PAnchor) obj).index);
    }
    
    public int compareTo(Object obj) {
        return (((PAnchor) obj).index - index);
    }
}
