package e.util;

import java.awt.*;
import java.awt.image.*;

public class ImageUtilities {
    /**
     * Improves on java.awt.RenderingHints' unsafe API.
     */
    public enum InterpolationHint {
        NONE(null),
        BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC),
        BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
        NEAREST_NEIGHBOR(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        
        InterpolationHint(Object hint) {
            this.hint = hint;
        }
        
        public Object hint;
    }
    
    public static BufferedImage scale(Image sourceImage, int newWidth, int newHeight, InterpolationHint interpolationHint) {
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImage.createGraphics();
        if (interpolationHint.hint != null) {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint.hint);
        }
        g.drawImage(sourceImage, 0, 0, newWidth, newHeight, null);
        return scaledImage;
    }
    
    private ImageUtilities() {
        // Utilities class.
    }
}
