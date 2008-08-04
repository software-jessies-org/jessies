package e.edit;

import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.util.regex.*;
import javax.swing.*;

public class ShowMisspellingsAction extends PTextAction {
    public ShowMisspellingsAction() {
        super("Show _Misspellings...", null, false);
    }
    
    private static String regularExpressionForWord(String word) {
        return "(?i)\\b" + StringUtilities.regularExpressionFromLiteral(word) + "\\b";
    }
    
    public void performOn(final PTextArea textArea) {
        String content = textArea.getText();
        Object[] misspelledWords = textArea.getSpellingChecker().listMisspellings().toArray();
        
        if (misspelledWords.length == 0) {
            Evergreen.getInstance().showAlert("No misspellings", "There are no misspellings in the selected window.");
            return;
        }
        
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
                ETextWindow textWindow = (ETextWindow) SwingUtilities.getAncestorOfClass(ETextWindow.class, textArea);
                // Highlight all matches of this misspelling.
                String literal = (String) list.getSelectedValue();
                literal = literal.substring(0, literal.indexOf(" ("));
                FindAction.INSTANCE.findInText(textWindow, regularExpressionForWord(literal));
                // Go to first match.
                textArea.setCaretPosition(0);
                textArea.findNext();
            }
        });
        list.setCellRenderer(new EListCellRenderer(true));
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "Misspellings");
        form.getFormPanel().addRow("Misspellings:", new JScrollPane(list));
        form.showNonModal();
    }
}
