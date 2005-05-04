package e.edit;

import java.awt.*;
import java.awt.event.*;

/**
 * The ETextArea action to change to a proportional font.
 */
public class ProportionalFontAction extends ChangeFontAction {
  public ProportionalFontAction() {
    super("Proportional");
  }
  
  public Font getFont() {
    return ETextArea.getConfiguredFont();
  }
}
