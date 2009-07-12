package terminator;

import e.util.*;
import java.io.*;
import java.util.*;

/**
 * Improves the performance of opening a new shell from the command line or
 * from applications such as the GNOME panel.
 * We allow the socket to tell us to execute arbitrary code.
 * This is only OK because we trust the file security on the secret file.
 * We don't accept connections except from localhost.
 */
public class TerminatorServer {
    public void parseCommandLine(PrintWriter out, String line) {
        List<String> arguments = new ArrayList<String>();
        String[] encodedArguments = line.split(" ");
        for (String encodedArgument : encodedArguments) {
            String argument = StringUtilities.urlDecode(encodedArgument);
            arguments.add(argument);
        }
        // Discard this method's name.
        arguments.remove(0);
        Terminator.getSharedInstance().parseCommandLine(arguments, out);
    }
}
