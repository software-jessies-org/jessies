package terminator.model;

import java.awt.*;

/**
 * Phil talks about this class as being immutable, yet it's not final.
 * I'm also not sure what would break if we could mutate Style
 * instances.
 *
 * For now, I've subclassed this and overridden getBackground.
 */
public class Style {
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
	
	/**
	 * Returns a new Style that represents this style's elements applied
	 * to the given style. Any attributes which this style doesn't have
	 * will be copied from the given style.
	 * 
	 * There's a small, fixed set of possible styles returned from this
	 * method, but we don't do any optimization to take advantage of this.
	 * We might want to reconsider that; it looks to me that something like
	 * sweeping a selection will needlessly create lots of new Style
	 * instances.
	 */
	public Style appliedTo(Style originalStyle) {
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
