package terminator.model;

import java.awt.*;

/**

@author Phil Norman
*/

public class Style implements StyleMutator {
	// If any of these is null, it should be ignored.  We use Boolean references because they
	// can be used to represent 3 states - null, TRUE and FALSE.
	private Color foreground;
	private Color background;
	private Boolean isBold;
	private Boolean isUnderlined;
	
	public Style(Color foreground, Color background, boolean isBold, boolean isUnderlined) {
		this(foreground, background, Boolean.valueOf(isBold), Boolean.valueOf(isUnderlined));
	}
	
	public Style(Color foreground, Color background, Boolean isBold, Boolean isUnderlined) {
		this.foreground = foreground;
		this.background = background;
		this.isBold = isBold;
		this.isUnderlined = isUnderlined;
	}
	
	public Style mutate(Style originalStyle) {
		Color mutatedForeground = hasForeground() ? getForeground() : originalStyle.getForeground();
		Color mutatedBackground = hasBackground() ? getBackground() : originalStyle.getBackground();
		boolean mutatedBold = hasBold() ? isBold() : originalStyle.isBold();
		boolean mutatedUnderlined = hasUnderlined() ? isUnderlined() : originalStyle.isUnderlined();
		return new Style(mutatedForeground, mutatedBackground, mutatedBold, mutatedUnderlined);
	}

	public boolean hasForeground() {
		return foreground != null;
	}
	
	public Color getForeground() {
		return foreground;
	}
	
	public boolean hasBackground() {
		return background != null;
	}
	
	public Color getBackground() {
		return background;
	}
	
	public boolean hasBold() {
		return isBold != null;
	}
	
	public boolean isBold() {
		return hasBold() && isBold.booleanValue();
	}
	
	public boolean hasUnderlined() {
		return isUnderlined != null;
	}
	
	public boolean isUnderlined() {
		return hasUnderlined() && isUnderlined.booleanValue();
	}
}
