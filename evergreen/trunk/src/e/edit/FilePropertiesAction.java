package e.edit;

import e.forms.*;
import e.gui.*;
import e.ptextarea.*;
import e.util.*;
import java.awt.event.*;
import java.nio.charset.*;
import java.util.*;
import javax.swing.*;

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
    private JLabel fileTypeField = new JLabel(" ");
    
    public FilePropertiesAction() {
        super(ACTION_NAME);
        GnomeStockIcon.useStockIcon(this, "gtk-info");
    }
    
    public void actionPerformed(ActionEvent e) {
        ETextWindow window = getFocusedTextWindow();
        if (window == null) {
            return;
        }
        
        ETextArea textArea = window.getTextArea();
        final PTextBuffer buffer = textArea.getTextBuffer();
        String endOfLineString = (String) buffer.getProperty(PTextBuffer.LINE_ENDING_PROPERTY);
        String initialEndOfLine = StringUtilities.escapeForJava(endOfLineString);
        endOfLineStringField.setText(initialEndOfLine);
        
        String indentationString = textArea.getIndentationString();
        String initialIndentationString = StringUtilities.escapeForJava(indentationString);
        indentStringField.setText(initialIndentationString);
        
        // FIXME: if you add a charset here, you'll probably have to modify ByteBufferDecoder so we can read the resulting file back in.
        final JComboBox charsetCombo = new JComboBox(new Object[] { "UTF-8", "ISO-8859-1", "UTF-16BE", "UTF-16LE" });
        charsetCombo.setSelectedItem(buffer.getProperty(PTextBuffer.CHARSET_PROPERTY));
        
        fileTypeField.setText(window.getFileType().getName());
        
        FormBuilder form = new FormBuilder(Evergreen.getInstance().getFrame(), "File Properties");
        FormPanel formPanel = form.getFormPanel();
        formPanel.addRow("End of Line:", endOfLineStringField);
        formPanel.addRow("Indent With:", indentStringField);
        formPanel.addRow("Character Encoding:", charsetCombo);
        formPanel.addRow("File Type:", fileTypeField); // FIXME: ideally, we'd let the user override this, too.
        
        form.getFormDialog().setAcceptCallable(new java.util.concurrent.Callable<Boolean>() {
            public Boolean call() {
                String newCharsetName = (String) charsetCombo.getSelectedItem();
                boolean encodingOkay = buffer.attemptEncoding(newCharsetName);
                if (encodingOkay == false) {
                    Evergreen.getInstance().showAlert("Can't encode file with encoding", "The " + newCharsetName + " encoding is not capable of representing all characters found in this file.");
                    return Boolean.FALSE;
                }
                String newEndOfLine = StringUtilities.unescapeJava(endOfLineStringField.getText());
                buffer.putProperty(PTextBuffer.LINE_ENDING_PROPERTY, newEndOfLine);
                String newIndentationString = StringUtilities.unescapeJava(indentStringField.getText());
                buffer.putProperty(PTextBuffer.INDENTATION_PROPERTY, newIndentationString);
                return Boolean.TRUE;
            }
        });
        form.show("Apply");
    }
}
