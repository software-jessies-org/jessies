package e.edit;

import java.awt.*;

/**
 * The ETextArea action to change to a proportional font.
 */
public class ProportionalFontAction extends ChangeFontAction {
  public ProportionalFontAction() {
    super("Proportional");
  }
  
  public Font getFont() {
    return getConfiguredFont();
  }
}
