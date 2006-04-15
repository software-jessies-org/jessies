package e.gui;

public interface BirdsEye {
    public int getVisibleLineCount();
    
    public int getVisibleLineIndex(int logicalLineIndex);
    
    public int getLogicalLineIndex(int visibleLineIndex);
    
    public boolean isCurrentLineIndex(int logicalLineIndex);
    
    public void goToLineAtIndex(int logicalLineIndex);
}
