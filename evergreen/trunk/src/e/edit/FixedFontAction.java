package e.edit;

import java.awt.*;

/**
 * The ETextArea action to change to a fixed-width font.
 */
public class FixedFontAction extends ChangeFontAction {
  public FixedFontAction() {
    super("Fixed");
  }
  
  public Font getFont() {
    return getConfiguredFixedFont();
  }
}
