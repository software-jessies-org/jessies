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
    
    public boolean isEnabled() {
        return super.isEnabled() && (getFocusedTextWindow() != null);
    }
    
    public void actionPerformed(ActionEvent e) {
        final ETextWindow window = getFocusedTextWindow();
        final ETextArea text = window.getText();
        final JList list = new JList(text.getSpellingChecker().listMisspellings().toArray());
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                // Highlight all matches of this misspelling.
                String literal = (String) list.getSelectedValue();
                text.find(StringUtilities.regularExpressionFromLiteral(literal));
                // Go to first match.
                text.setCaretPosition(0);
                window.findNext();
            }
        });
        list.setCellRenderer(new EListCellRenderer());

        FormPanel formPanel = new FormPanel();
        formPanel.addRow("Misspellings:", new JScrollPane(list));
        FormDialog.show(Edit.getFrame(), "Misspellings", formPanel);
    }
}
