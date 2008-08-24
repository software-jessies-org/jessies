package e.util;

import java.util.*;
import java.util.regex.*;

/**
 * Tries to guess the depth of indentation ("two spaces", "four spaces",
 * "single tab", et cetera) in use.
 * 
 * One area the current implementation has trouble is if you have a coding
 * standard that has a specific indentation, a separate amount of indentation
 * for wrapped lines, a short maximum line length, and languages/libraries
 * that use fairly long lines meaning that many lines are wrapped. It's hard
 * to imagine how IndentationGuesser could recognize wrapped lines (for all
 * the languages we support) and ignore them. One possibility -- assuming that
 * the wrapped-line indentation is an integer multiple of the normal
 * indentation -- might be to try to recognize these, and count them as votes
 * for each of their factors. Might that give the desired result?
 */
public class IndentationGuesser {
    private static final Stopwatch stopwatch = Stopwatch.get("IndentationGuesser.guessIndentationFromFile");
    
    private static final Pattern INDENTATION_PATTERN_1 = Pattern.compile("^(\\s+)[A-Za-z].*$");
    private static final Pattern INDENTATION_PATTERN_2 = Pattern.compile("^(\\s*)[{}]$");
    
    private static class LineIterator implements Iterator<CharSequence> {
        private CharSequence chars;
        private int length;
        
        private int start;
        private int end;
        
        public LineIterator(CharSequence chars) {
            this.chars = chars;
            this.length = chars.length();
            this.start = this.end = 0;
        }
        
        public boolean hasNext() {
            return (start < length);
        }
        
        public CharSequence next() {
            while (end < length && chars.charAt(end) != '\n') {
                ++end;
            }
            CharSequence result = chars.subSequence(start, end);
            start = end + 1;
            end = start;
            return result;
        }
        
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * Returns the best guess at the indentation in use in the given content.
     * Uses the given "fallback" string if it can't find anything better.
     */
    public static String guessIndentationFromFile(CharSequence chars, String fallback) {
        Stopwatch.Timer timer = stopwatch.start();
        try {
            CharSequence previousIndent = "";
            Bag<CharSequence> indentations = new Bag<CharSequence>();
            CharSequence emergencyAlternative = fallback;
            
            LineIterator it = new LineIterator(chars);
            int lineCount = 0;
            while (it.hasNext()) {
                CharSequence line = it.next();
                ++lineCount;
                
                if (lineCount > 4096) {
                    // If we haven't learned enough after 4096 lines, we may as well give up.
                    break;
                }
                
                Matcher matcher = INDENTATION_PATTERN_1.matcher(line);
                if (matcher.matches()) {
                    CharSequence indent = matcher.group(1);
                    if (indent.length() < emergencyAlternative.length()) {
                        emergencyAlternative = indent;
                    }
                    previousIndent = indent;
                }
                matcher = INDENTATION_PATTERN_2.matcher(line);
                if (matcher.matches()) {
                    CharSequence indent = matcher.group(1);
                    if (indent.length() > previousIndent.length()) {
                        CharSequence difference = indent.subSequence(0, previousIndent.length());
                        indentations.add(difference);
                    } else if (indent.length() < previousIndent.length()) {
                        CharSequence difference = previousIndent.subSequence(0, indent.length());
                        indentations.add(difference);
                    }
                    previousIndent = indent;
                }
            }
            //System.out.println("indentations=" + indentations);
            if (indentations.isEmpty()) {
                //System.out.println(" - no line just containing an indented brace?");
                return emergencyAlternative.toString();
            } else {
                return indentations.commonestItem().toString();
            }
        } finally {
            timer.stop();
            if (TimeUtilities.nsToS(timer.ns()) > 0.5) {
                Log.warn("IndentationGuesser took " + TimeUtilities.nsToString(timer.ns()) + " on " + chars.length() + " characters!");
            }
        }
    }
    
    private IndentationGuesser() {
    }
    
    public static void main(String[] args) {
        for (String filename : args) {
            System.out.println(filename + ": \"" + guessIndentationFromFile(StringUtilities.readFile(filename), "    ") + "\"");
        }
    }
}
