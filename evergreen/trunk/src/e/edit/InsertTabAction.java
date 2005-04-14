package e.edit;

import java.awt.event.*;

/**
 * The ETextArea action that inserts a tab.
 */
public class InsertTabAction extends ETextAction {
    public static final String ACTION_NAME = "insert-tab";

    public InsertTabAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        getTextArea().replaceSelection(getTextArea().getIndentationString());
    }
}
