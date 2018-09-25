package e.ptextarea;

import java.util.*;
import java.util.regex.*;

public class PGoIndenter extends PSimpleIndenter {
    private static final Pattern CASE_DEFAULT_PATTERN = Pattern.compile("^[ \\t]+(case.*|default):");
    
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
            if (seg.getStyle() == PStyle.STRING || seg.getStyle() == PStyle.COMMENT || seg.getStyle() == PStyle.NEWLINE) {
                continue;
            }
            sb.append(seg.getCharSequence());
        }
        return sb.toString();
    }
    
    private boolean isInMultiLineString(int index) {
        final int startIndex = index;
        for (; index >= 0; index--) {
            List<PLineSegment> ls = textArea.getLineSegments(index);
            if (ls.size() == 0) {
                continue; // Empty line; carry on backwards.
            }
            // If this is the first line, then we just have to check the first segment, not iterate back through.
            if (startIndex == index) {
                PLineSegment seg = ls.get(0);
                if (seg.getStyle() != PStyle.STRING) {
                    return false;
                }
                CharSequence charSeq = seg.getCharSequence();
                // The only case where we can't tell if this line begins in a multi-line string, is if the
                // string here contains only a single '`' character. In such a case it can either be the
                // beginning or the end of a ML string, but we have to keep scanning backwards to be sure.
                if (charSeq.length() == 1 && charSeq.charAt(0) == '`') {
                    continue;
                }
                // Now our choice is simple: if the first character is a '`', then we didn't start in a multi-line string,
                // as we know this sequence is longer than 1. In all other cases, we assume we did start in one.
                // There's a corner case here: if someone writes a line containing only a ""-style string (so simple, not
                // multi-line) then we can't really tell. However, a "" string without any indentation nor other chars
                // to its left is so far from go coding style, we can probably ignore it.
                // Anyway, we can't simply check for "" and assume it's not a ML string, as it might well be quotes that
                // happen to be part of a ML string.
                return charSeq.charAt(0) != '`';
            }
            ArrayList<PLineSegment> reversed = new ArrayList<PLineSegment>(ls);
            Collections.reverse(reversed);
            for (int i = 0; i < reversed.size(); i++) {
                PLineSegment seg = reversed.get(i);
                if (seg.getStyle() == PStyle.NEWLINE) {
                    continue; // Not interesting.
                }
                // If we see something other than a string, then we're not in a multi-line string.
                if (seg.getStyle() != PStyle.STRING) {
                    return false;
                }
                CharSequence charSeq = seg.getCharSequence();
                final boolean startsML = charSeq.charAt(0) == '`';
                final boolean endsML = charSeq.charAt(charSeq.length() - 1) == '`';
                // If we find the end of a ML string, the line we wanted was not in a ML string.
                if (endsML) {
                    return false;
                }
                // If we find the start, but not the end, then we're definitely inside.
                if (startsML) {
                    return true;
                }
            }
        }
        return false; // Start of file can't be in a multi-line string.
    }
    
    @Override public String calculateNewIndentation(int lineIndex) {
        if (isInMultiLineString(lineIndex)) {
            return null;
        }
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
        String line = getNonStringBits(lineIndex);
        if (line != null) {
            if (CASE_DEFAULT_PATTERN.matcher(line).matches() && indent.length() > textArea.getIndentationString().length()) {
                // Switch statement case and default statements are unindented by one level.
                // As we've composed the indent ourselves by stitching together textArea.getIndentationString()s,
                // trimming off from the start is equivalent to trimming off the end.
                indent = indent.substring(textArea.getIndentationString().length());
            } else {
                // If the current line (the one we're indenting) has leading close-brackets, deal with these too.
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
