package e.edit;

import java.io.*;
import java.util.*;

import e.util.*;

/**
 * A StringHistory maintains a file-backed historical list of strings.
 * 
 * @author Phil Norman
 */

public class StringHistory {
    private String filename;
    private ArrayList history = new ArrayList();
    
    public StringHistory(String leafName) {
        this.filename = Edit.getPreferenceFilename(leafName);
        try {
            readHistoryFile();
        } catch (IOException ex) {
            Log.warn("Failed to read history file from '" + filename + "'.");
        }
    }
    
    public int size() {
        return history.size();
    }
    
    public int getLatestHistoryIndex() {
        return size() - 1;
    }
    
    public String get(int index) {
        return (String) history.get(index);
    }
    
    public void add(String string) {
        if (string.length() == 0) {
            return;
        }
        history.remove(string);  // Avoid duplication.
        history.add(string);
        writeToHistoryFile(string);
    }
    
    public void readHistoryFile() throws IOException {
        history = new ArrayList();
        try {
            if (FileUtilities.exists(filename)) {
                String[] lines = StringUtilities.readLinesFromFile(filename);
                history.addAll(Arrays.asList(lines));
            }
        } catch (Exception ex) {
            Log.warn("Error reading history '" + filename + "'.", ex);
        }
    }
    
    public void writeToHistoryFile(String string) {
        String error = StringUtilities.appendToFile(FileUtilities.fileFromString(filename), string + "\n");
        if (error != null) {
            Log.warn("Failed to append string \"" + string + "\" to history file '" + filename + "' (" + error + ").");
        }
    }
}
