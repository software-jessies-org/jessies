package e.edit;

import java.awt.*;

/**
 * Changes to a proportional font.
 */
public class ProportionalFontAction extends ChangeFontAction {
  public ProportionalFontAction() {
    super("Proportional");
  }
  
  public Font getFont() {
    return getConfiguredFont();
  }
}
