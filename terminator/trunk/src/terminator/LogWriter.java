package terminatorn;

import java.io.*;
import java.text.*;
import java.util.*;

/**
 * Logs terminal output to a file.
 */
public class LogWriter {
    private static DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ssZ");
    
    private FileWriter stream;
    
    public LogWriter(String prefix) throws IOException {
        String timestamp = dateFormatter.format(new Date());
        String directory = makeDirectory();
        String filename =  prefix + '-' + timestamp + ".txt";
        stream = new FileWriter(directory + filename);
    }
    
    public void append(char ch) throws IOException {
        stream.write(ch);
        if (ch == '\n') {
            stream.flush();
        }
    }
    
    private String makeDirectory() {
        String directoryName = System.getProperty("user.home") + File.separator + ".terminal-logs" + File.separator;
        File directory = new File(directoryName);
        if (directory.exists() == false) {
            directory.mkdir();
        }
        return directoryName;
    }
}
