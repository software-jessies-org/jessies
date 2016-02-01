package e.io;

import java.io.*;

/**
 * Indents source output via the println method. The indentation is primitive,
 * but should be sufficient for anything you'd reasonably want to do in
 * generated C++ or Java. ('switch' statements being an obvious exception.)
 * 
 * Note: no-one should put too much effort into improving this class; the right
 * fix for aberrant behavior is to make it take an instance of what's currently
 * called PIndenter, but which could probably be generalized out of the
 * PTextArea code.
 */
public class IndentedSourceWriter {
    private PrintStream out;
    private int indentationDepth;
    
    public IndentedSourceWriter(PrintStream out) {
        this.out = out;
        this.indentationDepth = 0;
    }
    
    public void println(String line) {
        line = line.trim();
        if (line.startsWith("}")) {
            --indentationDepth;
        }
        if (line.endsWith(":") == false) {
            printIndentation();
        }
        out.println(line);
        if (line.endsWith("{")) {
            ++indentationDepth;
        }
    }
    
    private void printIndentation() {
        for (int i = 0; i < indentationDepth; ++i) {
            out.print("    ");
        }
    }
}
