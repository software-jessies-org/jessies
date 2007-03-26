package e.util;

import java.awt.datatransfer.*;

/**
 * Makes selection of large amounts of text on X11 systems cheap by deferring the copy until the user actually tries to paste.
 * The trade-off is that failures occur after the user thinks we've already succeeded.
 */
public abstract class LazyStringSelection implements Transferable, ClipboardOwner {
    private String cachedValue;
    
    public LazyStringSelection() {
    }
    
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] { DataFlavor.stringFlavor };
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.stringFlavor);
    }
    
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor.equals(DataFlavor.stringFlavor)) {
            synchronized (this) {
                if (cachedValue == null) {
                    cachedValue = reallyGetText();
                }
            }
            return cachedValue;
        }
        throw new UnsupportedFlavorException(flavor);
    }
    
    // We could return a char[] or whatever, but sun.awt.datatransfer.DataTransferer will only turn it into a String and then into a byte[].
    // We can return a byte[], but that will be turned into a String and then back into a byte[].
    // All we can do is return a String as cheaply as possible and accept that we need enough heap for a couple of copies at once.
    public abstract String reallyGetText();
    
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
}
