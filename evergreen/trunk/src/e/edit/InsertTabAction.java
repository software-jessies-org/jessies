package e.edit;

import java.awt.event.*;
import javax.swing.text.*;

/**
The ETextArea action that inserts a tab.
*/
public class InsertTabAction extends TextAction {
    public static final String ACTION_NAME = "insert-tab";

    public InsertTabAction() {
        super(ACTION_NAME);
    }

    public void actionPerformed(ActionEvent e) {
        ETextArea target = (ETextArea) getFocusedComponent();
        target.replaceSelection(target.getIndentationString());
    }
}
