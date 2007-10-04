package e.edit;

import java.awt.*;

/**
 * The ETextArea action to change to the appropriate font for the content.
 */
public class AppropriateFontAction extends ChangeFontAction {
  public AppropriateFontAction() {
    super("Appropriate");
  }
  
  public Font getFont() {
    return getAppropriateFontForContent(getFocusedTextArea().getText());
  }
}
