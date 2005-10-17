package e.tools.dict;

import java.io.*;
import java.net.*;
import java.util.regex.*;

/**
 * Implements the Dictionary Server Protocol (from a client perspective) as defined in RFC2229.
 */
public class DictionaryServerConnection {
    private static final int DICT_PORT = 2628;
    
    private Socket socket;
    private BufferedReader in;
    private PrintStream out;
    private boolean initialized = false;
    
    public DictionaryServerConnection() {
        initConnection();
    }
    
    private void initConnection() {
        try {
            this.socket = new Socket("dict.org", DICT_PORT);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            this.out = new PrintStream(socket.getOutputStream(), true, "UTF-8");
            this.initialized = true;
            String banner = in.readLine();
            System.out.println(banner);
            // FIXME: must be a "220", or we should report failure!
            out.println("CLIENT e.tools.dict.DictionaryBrowser (http://www.jessies.org/~enh/)");
            String clientResponse = in.readLine();
            System.out.println(clientResponse);
            // FIXME: must be a "250 ok", or we should report failure!
        } catch (IOException ex) {
            // FIXME: better error reporting.
            ex.printStackTrace();
        }
    }
    
    public String getDefinitionFor(String word) throws IOException {
        out.println("DEFINE * " + word);
        String line = in.readLine();
        if (line.startsWith("1") == false) {
            return "(not found: '" + line + "')";
        }
        if (line.startsWith("150 ")) {
            // Ignore the count of how many definitions were retrieved.
            line = in.readLine();
        }
        
        StringBuilder result = new StringBuilder();
        while (line.startsWith("250 ok") == false) {
            // We should be looking at a "151".
            String dictionaryName = extractDictionaryNameFrom151(line);
            
            String definition = readLines();
            result.append(dictionaryName);
            result.append('\n');
            result.append(definition);
            result.append('\n');
            line = in.readLine();
        }
        return result.toString();
    }
    
    private String extractDictionaryNameFrom151(String line) {
        Pattern pattern = Pattern.compile("^151\\s+\".+\"\\s+\\S+\\s+\"(.*)\"$");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return "(unknown dictionary: '" + line + "')";
        }
    }
    
    private String readLines() throws IOException {
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            if (line.equals(".")) {
                // That's all, folks!
                break;
            } else if (line.startsWith(".")) {
                // A leading '.' is quoted by another '.', strangely enough.
                line = line.substring(1);
            }
            result.append(line);
            result.append('\n');
        }
        return result.toString();
    }
}
