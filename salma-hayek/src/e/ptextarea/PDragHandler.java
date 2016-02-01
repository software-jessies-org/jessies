package e.ptextarea;

public interface PDragHandler {
    public void makeInitialSelection(int pressedOffset);
    public void mouseDragged(int newOffset);
}
