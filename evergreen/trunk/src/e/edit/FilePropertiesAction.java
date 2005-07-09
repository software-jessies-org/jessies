package e.edit;

import java.awt.event.*;
import java.nio.charset.*;
import java.util.*;
import javax.swing.*;
import e.forms.*;
import e.ptextarea.*;
import e.util.*;

/**
 * The ETextArea action to open file properties dialog where you can view
 * or alter the end of line string, indentation string, and character
 * encoding.
 */
public class FilePropertiesAction extends ETextAction {
    public static final String ACTION_NAME = "File Properties...";
    
    private JTextField endOfLineStringField = new JTextField("", 40);
    private JTextField indentStringField = new JTextField("", 40);
    private JTextField charsetStringField = new JTextField("", 40);
    
    public FilePropertiesAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        
        ETextArea text = window.getText();
        PTextBuffer buffer = text.getTextBuffer();
        String endOfLineString = (String) buffer.getProperty(PTextBuffer.LINE_ENDING_PROPERTY);
        String initialEndOfLine = StringUtilities.escapeForJava(endOfLineString);
        endOfLineStringField.setText(initialEndOfLine);
        
        String indentationString = text.getIndentationString();
        String initialIndentationString = StringUtilities.escapeForJava(indentationString);
        indentStringField.setText(initialIndentationString);
        
        charsetStringField.setText((String) buffer.getProperty(PTextBuffer.CHARSET_PROPERTY));
        charsetStringField.setEditable(false);
        charsetStringField.setEnabled(false);
        
        SortedMap<String, Charset> charsets = Charset.availableCharsets();
        for (String charset : charsets.keySet()) {
            System.out.println(charset + " = " + Charset.forName(charset).displayName());
        }
        
        FormPanel formPanel = new FormPanel();
        formPanel.addRow("End of Line:", endOfLineStringField);
        formPanel.addRow("Indent With:", indentStringField);
        formPanel.addRow("Character Encoding:", charsetStringField);
        boolean okay = FormDialog.show(Edit.getInstance().getFrame(), "File Properties", formPanel, "Apply");
        
        if (okay == false) {
            return;
        }
        
        String newEndOfLine = StringUtilities.unescapeJava(endOfLineStringField.getText());
        buffer.putProperty(PTextBuffer.LINE_ENDING_PROPERTY, newEndOfLine);
        String newIndentationString = StringUtilities.unescapeJava(indentStringField.getText());
        buffer.putProperty(PTextBuffer.INDENTATION_PROPERTY, newIndentationString);
    }
}
