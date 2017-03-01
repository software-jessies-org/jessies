package e.ptextarea;

import java.util.*;

public class PGoIndenter extends PSimpleIndenter {
    public PGoIndenter(PTextArea textArea) {
        super(textArea);
    }
    
    @Override public boolean isElectric(char c) {
        return "{}[]():".indexOf(c) != -1;
    }
    
    private CharSequence getIndent(CharSequence cs) {
        for (int i = 0; i < cs.length(); i++) {
            if (!Character.isWhitespace(cs.charAt(i))) {
                return cs.subSequence(0, i);
            }
        }
        return null;  // No indent, or no contents (in which case ignore trailing spaces).
    }
    
    private String getNonStringBits(int index) {
        List<PLineSegment> ls = textArea.getLineSegments(index);
        if (ls.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (PLineSegment seg : ls) {
            if (ls.get(0).getStyle() == PStyle.STRING || ls.get(0).getStyle() == PStyle.COMMENT || ls.get(0).getStyle() == PStyle.NEWLINE) {
                return null;
            }
            sb.append(seg.getCharSequence());
        }
        return sb.toString();
    }
    
    @Override public String calculateNewIndentation(int lineIndex) {
        // Start by going back to the earliest previous line which has content, but no indent, caching all interesting lines.
        ArrayList<String> prevLines = new ArrayList<String>();  // Avoid fetching the same thing and checking for ML strings over and over again.
        for (int startIndex = lineIndex - 1; startIndex >= 0; --startIndex) {
            // We use the line segments (which have been through the styler) so we can detect when we're in strings
            // or comments, and just remove all that stuff. This will make our job easier later, as we won't need to
            // worry about whether a bracket is inside a string or not.
            String content = getNonStringBits(startIndex);
            if (content == null) {
                continue;
            }
            CharSequence indent = getIndent(content);
            if (indent == null) {  // Blank line - ignore.
                continue;
            }
            // This is a potentially interesting line, even if it's the first line, so store it in our 'interesting lines' cache.
            prevLines.add(content);
            if (indent.length() == 0) {
                break;
            }
        }
        
        // Now prevLines contains the pre-processed code, all comments and strings elided, which goes back from the
        // line before the one to be indented, to the latest earlier line which starts without indent. This will be
        // a func definition, or (var|import|const) \( beginning.
        Stack<Indent> indents = new Stack<Indent>();
        // We appended to prevLines, walking backwards through the file, so we need to process the last one first.
        // Very biblical.
        String indent = "";
        for (int i = prevLines.size() - 1; i >= 0; i--) {
            int level = 0;
            String line = prevLines.get(i);
            //System.err.println("Line: '" + line + "'");
            for (int j = 0; j < line.length(); j++) {
                char ch = line.charAt(j);
                if (isOpen(ch)) {
                    indents.push(new Indent(indent, getClose(ch)));
                    level++;
                    //System.err.println("  [" + ch + "] level -> " + level);
                } else if (isClose(ch)) {
                    if (!indents.empty() && (indents.peek().closer == ch)) {
                        indent = indents.pop().indent;
                        //System.err.println("  [" + ch + "] popped");
                        level = Math.max(0, level - 1);
                        //System.err.println("  [" + ch + "] level <- " + level);
                    }
                }
            }
            if (level > 0) {
                indent = indent + textArea.getIndentationString();
                //System.err.println("  level = " + level + "; indent -> '" + indent + "'");
            }
        }
        // If the current line (the one we're indenting) has leading close-brackets, deal with these too.
        String line = getNonStringBits(lineIndex);
        if (line != null) {
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (isOpen(ch)) {
                    break;
                }
                if (isClose(ch)) {
                    if (!indents.empty() && (indents.peek().closer == ch)) {
                        indent = indents.pop().indent;
                        //System.err.println("  [" + ch + "] popped");
                    }
                }
            }
        }
        return indent;
    }
    
    // OPEN and CLOSE are co-indexed. IOW, OPEN[n] must correspond to CLOSE[n].
    private static final String OPEN = "({[";
    private static final String CLOSE = ")}]";
    
    private boolean isOpen(char ch) {
        return OPEN.indexOf(ch) != -1;
    }
    
    private boolean isClose(char ch) {
        return CLOSE.indexOf(ch) != -1;
    }
    
    // Call this with a char that isn't an open bracket, and you'll get a stack trace.
    private char getClose(char ch) {
        return CLOSE.charAt(OPEN.indexOf(ch));
    }
    
    private static class Indent {
        public String indent;
        public char closer;
        
        public Indent(String indent, char closer) {
            this.indent = indent;
            this.closer = closer;
        }
    }
}
