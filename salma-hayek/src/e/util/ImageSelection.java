package e.util;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.*;

/**
 * A Transferable able to transfer an AWT Image.
 * Similar to the JDK StringSelection class.
 */
public class ImageSelection implements Transferable {
    private Image image;
    
    public static void copyImageToClipboard(Image image) {
        // Work around a Sun bug that causes a hang in "sun.awt.image.ImageRepresentation.reconstruct".
        new javax.swing.ImageIcon(image); // Force load.
        BufferedImage newImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        newImage.createGraphics().drawImage(image, 0, 0, null);
        image = newImage;
        
        ImageSelection imageSelection = new ImageSelection(image);
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        toolkit.getSystemClipboard().setContents(imageSelection, null);
    }
    
    public ImageSelection(Image image) {
        this.image = image;
    }
    
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (flavor.equals(DataFlavor.imageFlavor) == false) {
            throw new UnsupportedFlavorException(flavor);
        }
        return image;
    }
    
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.imageFlavor);
    }
    
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[] {
            DataFlavor.imageFlavor
        };
    }
}
