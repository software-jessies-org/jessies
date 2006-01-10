package terminator;

import java.io.*;

/**
 * Improves the performance of opening a new shell from the command line or
 * from applications such as the GNOME panel. This should not be extended to
 * execute arbitrary commands without going to appropriate lengths to secure
 * and authenticate the communication.
 */
public class TerminatorServer {
    public void newShell(PrintWriter out, String line) {
        try {
            // We don't accept any arguments over the network, because that could easily be a security hole.
            Terminator.getSharedInstance().parseCommandLine(new String[0], out, out);
        } catch (Exception ex) {
            ex.printStackTrace(out);
        }
    }
}
