package e.edit;

import java.awt.event.*;
import javax.swing.*;
import e.forms.*;
import e.ptextarea.*;
import e.util.*;

/**
The ETextArea action to open file properties dialog where you can view
or alter the end of line string, indentation string, and other stuff as we
think of it. (File encoding would be one possibility.)
*/
public class FilePropertiesAction extends ETextAction {
    public static final String ACTION_NAME = "File Properties...";
    
    private JTextField endOfLineStringField = new JTextField("", 40);
    private JTextField indentStringField = new JTextField("", 40);
    
    public FilePropertiesAction() {
        super(ACTION_NAME);
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        
        ETextArea text = window.getText();
        PTextBuffer buffer = text.getPTextBuffer();
        String endOfLineString = (String) buffer.getProperty(PTextBuffer.LINE_ENDING_PROPERTY);
        if (endOfLineString == null) {
            endOfLineString = System.getProperty("line.separator");
        }
        String initialEndOfLine = StringUtilities.escapeForJava(endOfLineString);
        endOfLineStringField.setText(initialEndOfLine);
        
        String indentationString = text.getIndentationString();
        String initialIndentationString = StringUtilities.escapeForJava(indentationString);
        indentStringField.setText(initialIndentationString);

        FormPanel formPanel = new FormPanel();
        formPanel.addRow("End of Line:", endOfLineStringField);
        formPanel.addRow("Indent With:", indentStringField);
        boolean okay = FormDialog.show(Edit.getFrame(), "File Properties", formPanel, "Apply");
        
        if (okay == false) {
            return;
        }
        
        String newEndOfLine = StringUtilities.unescapeJava(endOfLineStringField.getText());
        buffer.putProperty(PTextBuffer.LINE_ENDING_PROPERTY, newEndOfLine);
        String newIndentationString = StringUtilities.unescapeJava(indentStringField.getText());
        buffer.putProperty(PTextBuffer.INDENTATION_PROPERTY, newIndentationString);
    }
}
