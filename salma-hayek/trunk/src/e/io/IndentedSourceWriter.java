package e.io;

import java.io.*;

/**
 * Indents source output via the println method. The indentation is primitive,
 * but should be sufficient for anything you'd reasonably want to do in
 * generated C++ or Java. ('switch' statements being an obvious exception.)
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
        if (line.equals("}")) {
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
