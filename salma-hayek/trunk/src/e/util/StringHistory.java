package e.util;

import java.io.*;
import java.util.*;

/**
 * A file-backed historical list of strings.
 * 
 * @author Phil Norman
 */
public class StringHistory {
    private String filename;
    private ArrayList<String> history;
    
    public StringHistory() {
        this(null);
    }
    
    public StringHistory(String filename) {
        this.filename = filename;
        this.history = new ArrayList<String>();
        readHistoryFile();
    }
    
    public int size() {
        return history.size();
    }
    
    public int getLatestHistoryIndex() {
        return size() - 1;
    }
    
    public String get(int index) {
        return history.get(index);
    }
    
    public void add(String string) {
        if (string.length() == 0) {
            return;
        }
        history.remove(string); // Avoid duplicates.
        history.add(string);
        writeToHistoryFile(string);
    }
    
    public void clear() {
        history = new ArrayList<String>();
        if (filename != null) {
            FileUtilities.fileFromString(filename).delete();
        }
    }
    
    private void readHistoryFile() {
        try {
            if (filename != null && FileUtilities.exists(filename)) {
                String[] lines = StringUtilities.readLinesFromFile(filename);
                history.addAll(Arrays.asList(lines));
            }
        } catch (Exception ex) {
            Log.warn("Error reading history from file \"" + filename + "\".", ex);
        }
    }
    
    private void writeToHistoryFile(String string) {
        if (filename == null) {
            return;
        }
        
        String error = StringUtilities.appendToFile(FileUtilities.fileFromString(filename), string + "\n");
        if (error != null) {
            Log.warn("Failed to append string \"" + string + "\" to history file \"" + filename + "\" (" + error + ").");
        }
    }
}
