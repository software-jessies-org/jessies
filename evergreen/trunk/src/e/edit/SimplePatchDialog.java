package e.edit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;

public class SimplePatchDialog {
    /** Background color for the @@ lines. */
    private static final Color VERY_LIGHT_GRAY = new Color(230, 230, 230);
    
    /** Background color for the +++ lines. */
    private static final Color TRIPLE_PLUS_BACKGROUND = new Color(0xcc, 0xcc, 0xff);
    
    /** Background color for the --- lines. */
    private static final Color TRIPLE_MINUS_BACKGROUND = new Color(0xff, 0xcc, 0xcc);
    
    private static final String PREFIX = "e.edit.SimplePatchDialog-";
    
    private SimplePatchDialog() {
    }
    
    public static JComponent makeScrollablePatchView(String fromName, String fromContent, String toName, String toContent) {
        return new JScrollPane(makePatchView(fromName, fromContent, toName, toContent));
    }
    
    public static JComponent makePatchView(String fromName, String fromContent, String toName, String toContent) {
        String fromFile = FileUtilities.createTemporaryFile(PREFIX, "file containing " + fromName, fromContent);
        String toFile = FileUtilities.createTemporaryFile(PREFIX, "file containing " + toName, toContent);
        
        ArrayList<String> lines = new ArrayList<String>();
        ArrayList<String> errors = new ArrayList<String>();
        String[] command = new String[] { "diff", "-u", "-b", "-B", "-L", toName, toFile, "-L", fromName, fromFile };
        int status = ProcessUtilities.backQuote(null, command, lines, errors);
        
        if (status == 0) {
            lines.add("(No non-whitespace differences.)");
        }
        
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.size(); ++i) {
            builder.append(lines.get(i));
            builder.append('\n');
        }
        
        PTextArea textArea = new PTextArea();
        textArea.setEditable(false);
        textArea.setTextStyler(new PPatchTextStyler(textArea));
        textArea.setText(builder.toString());
        textArea.setFont(ChangeFontAction.getConfiguredFixedFont());
        
        for (int i = 0; i < textArea.getLineCount(); ++i) {
            String lineText = textArea.getLineText(i);
            Color color = null;
            if (lineText.startsWith("+++")) {
                color = TRIPLE_PLUS_BACKGROUND;
            } else if (lineText.startsWith("---")) {
                color = TRIPLE_MINUS_BACKGROUND;
            } else if (lineText.startsWith("@@ ")) {
                color = VERY_LIGHT_GRAY;
            }
            if (color != null) {
                int start = textArea.getLineStartOffset(i);
                int end = textArea.getLineEndOffsetBeforeTerminator(i) + 1;
                textArea.addHighlight(new PPatchTextStyler.PatchHighlight(textArea, start, end, color));
            }
        }
        
        return textArea;
    }
    
    public static void showPatchBetween(String title, String fromName, String fromContent, String toName, String toContent) {
        FormBuilder form = new FormBuilder(Edit.getInstance().getFrame(), title);
        form.getFormPanel().addRow("Differences:", makeScrollablePatchView(fromName, fromContent, toName, toContent));
        form.showNonModal();
    }
}
