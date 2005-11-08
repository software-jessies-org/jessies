package terminator;

import java.io.*;

public class TerminatorServer {
    public void newTerminal(PrintWriter out, String line) {
        try {
            String tail = line.substring("newTerminal ".length());
            String[] arguments = (tail.length() > 0) ? tail.split("\u0000") : new String[0];
            Terminator.getSharedInstance().parseCommandLine(arguments, out, out);
        } catch (Exception ex) {
            ex.printStackTrace(out);
        }
    }
}
