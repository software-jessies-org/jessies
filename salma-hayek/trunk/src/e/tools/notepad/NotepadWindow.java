package e.tools.notepad;

import e.ptextarea.*;
import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class NotepadWindow extends JFrame {
    private PTextArea textArea;
    private Timer autoSaveTimer;
    private File file = FileUtilities.fileFromString("~/Library/NotepadContents.txt");
    
    public NotepadWindow() {
        super("Notepad");
        setLayout(new BorderLayout());
        setSize(new Dimension(640, 480));
        textArea = new PTextArea();
        textArea.setWrapStyleWord(true);
        restore();
        initAutoSave();
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("window closing");
                store();
            }
        });
    }
    
    private void store() {
        if (textArea.getTextBuffer().getUndoBuffer().isClean() == false) {
            textArea.getTextBuffer().writeToFile(file);
        }
    }
    
    private void restore() {
        try {
            textArea.getTextBuffer().readFromFile(file);
        } catch (Exception ex) {
            // It's okay for the file to be missing; it's probably the first
            // time we've run.
        }
    }
    
    private void initAutoSave() {
        autoSaveTimer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                store();
            }
        });
        autoSaveTimer.setRepeats(false);
        // FIXME: I notice that most users of PTextListener are slightly
        // inconvenienced by the fact that there are three separate methods.
        // Only some of the internal PTextArea classes actually make use of
        // this. (Is PAbstractLanguageStyler's behavior in face of
        // textCompletelyReplaced correct? It seems to do too little.)
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textCompletelyReplaced(PTextEvent e) {
                store();
            }
            
            public void textRemoved(PTextEvent e) {
                store();
            }
            
            public void textInserted(PTextEvent e) {
                store();
            }
        });
    }
}
