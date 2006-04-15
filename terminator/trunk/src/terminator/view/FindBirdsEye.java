package terminator.view;

import java.awt.*;
import e.gui.*;
import terminator.model.*;

public class FindBirdsEye implements BirdsEye {
    private JTextBuffer view;
    private int currentLineIndex = -1;
    
    public FindBirdsEye(JTextBuffer view) {
        this.view = view;
    }
    
    public void setCurrentLineIndex(int currentLineIndex) {
        this.currentLineIndex = currentLineIndex;
    }
    
    public int getVisibleLineCount() {
        return view.getModel().getLineCount();
    }
    
    public int getVisibleLineIndex(int logicalLineIndex) {
        return logicalLineIndex;
    }
    
    public int getLogicalLineIndex(int visibleLineIndex) {
        return visibleLineIndex;
    }
    
    public boolean isCurrentLineIndex(int logicalLineIndex) {
        return (logicalLineIndex == currentLineIndex);
    }
    
    public void goToLineAtIndex(int logicalLineIndex) {
        setCurrentLineIndex(logicalLineIndex);
        Rectangle textPosition = view.modelToView(new Location(logicalLineIndex, 0));
        int mid = textPosition.y + textPosition.height / 2;
        Rectangle visible = view.getVisibleRect();
        visible.y = mid - visible.height / 2;
        view.scrollRectToVisible(visible);
    }
}
