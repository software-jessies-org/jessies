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
    private File file;
    private ArrayList history = new ArrayList();
    
    public StringHistory(String leafName) {
        this.file = new File(Edit.getPreferenceFilename(leafName));
        try {
            readHistoryFile();
        } catch (IOException ex) {
            Log.warn("Failed to read history file from " + file.getPath());
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
        history.remove(string);  // Avoid duplication.
        history.add(string);
        writeToHistoryFile(string);
    }
    
    public void readHistoryFile() throws IOException {
        history = new ArrayList();
        if (file.exists() == false) {
            return;
        }
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                history.add(line);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    Log.warn("Error closing history file " + file.getPath(), ex);
                }
            }
        }
    }
    
    public void writeToHistoryFile(String string) {
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter(file, true));
            writer.println(string);
            writer.close();
        } catch (IOException ex) {
            Log.warn("Failed to append string \"" + string + "\" to history file " + file.getPath(), ex);
            if (writer != null) {
                writer.close();
            }
        }
    }
}
