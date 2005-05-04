package e.edit;

import java.awt.*;
import java.awt.event.*;

/**
 * The ETextArea action to change to the appropriate font for the content.
 */
public class AppropriateFontAction extends ChangeFontAction {
  public AppropriateFontAction() {
    super("Appropriate");
  }
  
  public Font getFont() {
    ETextArea text = getTextArea();
    return text.getAppropriateFontForContent();
  }
}
