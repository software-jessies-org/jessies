package e.edit;

import e.forms.*;
import e.gui.*;
import e.util.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;

public class ShowMisspellingsAction extends ETextAction {
    public static final String ACTION_NAME = "Show Misspellings...";
    
    public ShowMisspellingsAction() {
        super(ACTION_NAME);
    }
    
    private static String regularExpressionForWord(String word) {
        return "(?i)\\b" + StringUtilities.regularExpressionFromLiteral(word) + "\\b";
    }
    
    public void actionPerformed(ActionEvent e) {
        final ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        
        final ETextArea text = window.getText();
        String content = text.getText();
        Object[] misspelledWords = text.getSpellingChecker().listMisspellings().toArray();
        String[] listItems = new String[misspelledWords.length];
        for (int i = 0; i < listItems.length; ++i) {
            String word = (String) misspelledWords[i];
            Pattern pattern = Pattern.compile(regularExpressionForWord(word));
            Matcher matcher = pattern.matcher(content);
            int count = 0;
            while (matcher.find()) {
                ++count;
            }
            listItems[i] = word + " (" + count + ")";
        }
        
        final JList list = new JList(listItems);
        list.setPrototypeCellValue("this would be quite a long misspelling");
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) {
                    return;
                }
                // Highlight all matches of this misspelling.
                String literal = (String) list.getSelectedValue();
                literal = literal.substring(0, literal.indexOf(" ("));
                FindAction.INSTANCE.findInText(window, regularExpressionForWord(literal));
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
