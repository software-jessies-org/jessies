package e.util;

import java.awt.*;
import java.awt.image.*;

public class ImageUtilities {
    /**
     * Improves on java.awt.RenderingHints' unsafe API.
     */
    public enum InterpolationHint {
        REPLICATE(null),
        BICUBIC(RenderingHints.VALUE_INTERPOLATION_BICUBIC),
        BILINEAR(RenderingHints.VALUE_INTERPOLATION_BILINEAR),
        NEAREST_NEIGHBOR(RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        
        InterpolationHint(Object hint) {
            this.hint = hint;
        }
        
        public Object hint;
    }
    
    public static Image scale(Image sourceImage, int newWidth, int newHeight, InterpolationHint interpolationHint) {
        if (newWidth == 0 || newHeight == 0) {
            throw new IllegalArgumentException("neither newWidth (" + newWidth + ") nor newHeight (" + newHeight + ") may be zero");
        }
        if (newWidth < 0 && newHeight < 0) {
            throw new IllegalArgumentException("one of newWidth (" + newWidth + ") and newHeight (" + newHeight + ") must be positive");
        }
        
        if (interpolationHint.hint != null) {
            return scaleWithAcceleration(sourceImage, newWidth, newHeight, interpolationHint);
        } else {
            // This can't be handed off to the graphics hardware in the same
            // way as above. There's about a 10% difference in CPU usage on
            // Linux with Java 5, but on Mac OS setting KEY_INTERPOLATION to
            // null throws an exception and leaving it at its default setting
            // causes blurring. The default on Linux seems to be equivalent
            // to Image.SCALE_REPLICATE, but there's no way to specify that,
            // and I don't want to rely on undocumented behavior.
            return sourceImage.getScaledInstance(newWidth, newHeight, Image.SCALE_REPLICATE);
        }
    }
    
    private static final Image scaleWithAcceleration(Image sourceImage, int newWidth, int newHeight, InterpolationHint interpolationHint) {
        // Image.getScaledInstance would do this for us.
        final int srcWidth = sourceImage.getWidth(null);
        final int srcHeight = sourceImage.getHeight(null);
        if (newWidth < 0) {
            newWidth = srcWidth * newHeight / srcHeight;
        }
        if (newHeight < 0) {
            newHeight = srcHeight * newWidth / srcWidth;
        }
        
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaledImage.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolationHint.hint);
        g.drawImage(sourceImage, 0, 0, newWidth, newHeight, null);
        return scaledImage;
    }
    
    private ImageUtilities() {
        // Utilities class.
    }
}
