package e.edit;

import java.awt.*;

/**
 * Changes to the appropriate font for the content.
 */
public class AppropriateFontAction extends ChangeFontAction {
  public AppropriateFontAction() {
    super("Appropriate");
  }
  
  public Font getFont() {
    return getAppropriateFontForContent(getFocusedTextArea().getText());
  }
}
