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
    private ArrayList<String> history = new ArrayList<String>();
    
    public StringHistory() {
        this(null);
    }
    
    public StringHistory(String filename) {
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
        return history.get(index);
    }
    
    public void add(String string) {
        if (string.length() == 0) {
            return;
        }
        history.remove(string);  // Avoid duplication.
        history.add(string);
        writeToHistoryFile(string);
    }
    
    public void clear() {
        history = new ArrayList<String>();
        if (filename != null) {
            FileUtilities.fileFromString(filename).delete();
        }
    }
    
    public void readHistoryFile() throws IOException {
        history = new ArrayList<String>();
        if (filename == null) {
            return;
        }
        
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
        if (filename == null) {
            return;
        }
        
        String error = StringUtilities.appendToFile(FileUtilities.fileFromString(filename), string + "\n");
        if (error != null) {
            Log.warn("Failed to append string \"" + string + "\" to history file '" + filename + "' (" + error + ").");
        }
    }
}
