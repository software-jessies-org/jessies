package e.util;

import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 * A duplicate-free list of strings, optionally persisted to disk.
 * Strings at low-numbered indexes are older than those at high-numbered indexes.
 * 
 * @author Phil Norman
 */
public class StringHistory {
    private String filename;
    private ArrayList<String> history;
    
    /**
     * Creates a new empty history that will not be written to disk.
     */
    public StringHistory() {
        this(null);
    }
    
    /**
     * Creates a new history that starts with any history already on disk, and which writes changes out to disk as the occur.
     */
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
    
    /**
     * Returns a list of all the strings in this history that match the given regular expression.
     * The strings are returned oldest first.
     * It's easy for a caller to sort the result, but it wouldn't be easy for the caller to infer the chronological ordering.
     */
    public List<String> getStringsMatching(String regularExpression) {
        Pattern pattern = PatternUtilities.smartCaseCompile(regularExpression);
        ArrayList<String> result = new ArrayList<String>();
        for (String candidate : history) {
            Matcher matcher = pattern.matcher(candidate);
            if (matcher.find()) {
                result.add(candidate);
            }
        }
        return result;
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
