package e.debugger;

import java.io.*;
import java.lang.reflect.*;

import e.util.*;

/**
 * A server that responds to any commands defined in the ServerCommandHandler interface.
 */

public class DebuggerServer extends InAppServer {
    
    private DebuggerCommandHandler debuggerCommandHandler;
    
    public DebuggerServer(DebuggerCommandHandler debuggerCommandHandler) {
        super("DebuggerCommandHandler", System.getProperty("preferencesDirectory") + File.separator + "/debugger-server-port");
        this.debuggerCommandHandler = debuggerCommandHandler;
    }
    
    public boolean handleCommand(String line, PrintWriter out) {
        System.err.println(line);
        String[] split = line.split("[\t ]");
        Class[] argTypes = new Class[split.length - 1];
        String[] args = new String[split.length - 1];
        for (int i = 0; i < args.length; i++) {
            argTypes[i] = String.class;
            args[i] = split[i + 1];
        }
        try {
            Method method = DebuggerCommandHandler.class.getMethod(split[0], argTypes);
            Object result = method.invoke(debuggerCommandHandler, (Object[]) args);
            out.println((result == null) ? "OK" : result.toString());
            return true;
        } catch (NoSuchMethodException nsmex) {
            out.println("DebugServer: didn't understand request \"" + line + "\".");
        } catch (Exception ex) {
            String s = "DebugServer: request denied \"" + line + "\" (" + ex.toString() + ").";
            out.println(s);
        } finally {
            out.flush();
            out.close();
        }
        return false;
    }
}
