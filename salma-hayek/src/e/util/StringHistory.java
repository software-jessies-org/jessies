package e.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * A duplicate-free list of strings, optionally persisted to disk.
 * Strings at low-numbered indexes are older than those at high-numbered indexes.
 * FIXME: we should put some bound on the amount of history we're prepared to keep.
 * 
 * @author Phil Norman
 */
public class StringHistory {
    private static final ExecutorService executor = ThreadUtilities.newSingleThreadExecutor("StringHistory Writer");
    
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
        // Avoid duplicates by removing any old entry first.
        history.remove(string);
        history.add(string);
        writeHistoryFile();
    }
    
    public void remove(String string) {
        history.remove(string);
        writeHistoryFile();
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
                for (String line : StringUtilities.readLinesFromFile(filename)) {
                    history.add(line);
                }
            }
        } catch (Exception ex) {
            Log.warn("Error reading history from file \"" + filename + "\".", ex);
        }
    }
    
    private void writeHistoryFile() {
        if (filename == null) {
            return;
        }
        
        // Make sure that we don't write to disk off the EDT.
        // FIXME: this relies on the calling code not calling us too frequently; a timer might be a better idea.
        executor.execute(new Runnable() {
            public void run() {
                String error = StringUtilities.writeFile(FileUtilities.fileFromString(filename), history);
                if (error != null) {
                    Log.warn("Failed to write history to file \"" + filename + "\" (" + error + ").");
                }
            }
        });
    }
}
