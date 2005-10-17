package e.tools.dict;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import e.ptextarea.*;
import e.util.*;

public class DictionaryBrowser extends JFrame {
    private DictionaryServerConnection server = new DictionaryServerConnection();
    private PTextArea textArea;
    
    public DictionaryBrowser() {
        super("Dictionary");
        setContentPane(makeUI());
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLocationByPlatform(true);
        setSize(new Dimension(640, 480));
    }
    
    private JComponent makeUI() {
        this.textArea = new PTextArea();
        
        final JTextField textField = new JTextField();
        textField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                lookUpInDictionary(textField.getText());
                textField.selectAll();
            }
        });
        
        JPanel ui = new JPanel(new BorderLayout());
        ui.add(textField, BorderLayout.NORTH);
        ui.add(new JScrollPane(textArea), BorderLayout.CENTER);
        return ui;
    }
    
    private void lookUpInDictionary(String word) {
        try {
            String definition = server.getDefinitionFor(word);
            textArea.setText(definition);
        } catch (IOException ex) {
            // FIXME: better error reporting.
            ex.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                GuiUtilities.initLookAndFeel();
                new DictionaryBrowser().setVisible(true);
            }
        });
    }
}
