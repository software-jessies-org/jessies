package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.event.*;
import javax.swing.*;

public class ShowMisspellingsAction extends ETextAction {
    public static final String ACTION_NAME = "Show Misspellings...";
    
    public ShowMisspellingsAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        final ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        final ETextArea text = window.getText();
        final JList list = new JList(text.getSpellingChecker().listMisspellings().toArray());
        list.setPrototypeCellValue("this would be quite a long misspelling");
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                // Highlight all matches of this misspelling.
                String literal = (String) list.getSelectedValue();
                FindAction.INSTANCE.findInText(window, StringUtilities.regularExpressionFromLiteral(literal));
                // Go to first match.
                text.setCaretPosition(0);
                window.findNext();
            }
        });
        list.setCellRenderer(new EListCellRenderer(true));

        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Misspellings:", new JScrollPane(list));
        FormDialog.showNonModal(Edit.getFrame(), "Misspellings", formPanel);
    }
}
