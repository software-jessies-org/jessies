package e.edit;

import e.ptextarea.*;
import e.util.*;
import java.util.*;

/**
CommandInterpreter is a very simple interpreter for the kinds of commands that
external tools that provide, when they're run using the '|!' prefix.

The basic syntax is key=value pairs, with a single blank line between one command
and the next. Comments are not supported (this is an inter-process protocol).
Values are either literal strings, up to the next newline, or if the first character is
a double-quote ("), the end of the string is the matching double-quote. If a string contains
a double-quote itself, that is escaped by doubling it. So in other words, the way to provide
a value including a quoted string is:

new_text="This contains ""quoted text"""

Double-quoted strings can be multi-line.

Commands provided:

--  Set the caret position to line=10, char=3
command=select
start_line=10
start_char=3
--

--  Insert a new "#!/bin/bash" line at the start of a file (note the need for a newline).
command=replace
start_line=1
new_text="#!/bin/bash
"
--

--  Delete the third line.
command=replace
start_line=3
end_line=4
--

--  Insert a banner at the top of the file, and select the word "this" in it.
command=replace
start_line=1
new_text="*************
** this is a banner
*************
"

command=select
start_line=2
start_char=3
end_line=2
end_char=7
--
*/
public class CommandInterpreter {
    private String data;
    private int pos = 0;
    
    private CommandInterpreter(String data) {
        this.data = data;
    }
    
    private Map<String, String> next() {
        Map<String, String> result = new HashMap<String, String>();
        while (true) {
            int nextEq = data.indexOf('=', pos);
            int nextNewline = data.indexOf('\n', pos);
            if (nextEq == -1) {
                return result.isEmpty() ? null : result;
            }
            if (nextNewline != -1 && nextNewline < nextEq) {
                pos = nextNewline + 1;
                return result;
            }
            String key = data.substring(pos, nextEq);
            pos = nextEq + 1;
            if (pos >= data.length()) {
                return result.isEmpty() ? null : result;
            }
            String value;
            if (data.charAt(pos) == '"') {
                pos++;
                int endPos = findClosingQuote(data, pos);
                value = data.substring(pos, endPos);
                pos = endPos;
                int newlineAfter = data.indexOf('\n', pos);
                if (newlineAfter != -1) {
                    pos = newlineAfter + 1;
                }
            } else {
                value = data.substring(pos, nextNewline);
                pos = nextNewline + 1;
            }
            result.put(key, value);
        }
    }
    
    private static int findClosingQuote(String data, int pos) {
        boolean wasQuote = false;
        for (; pos < data.length(); pos++) {
            char ch = data.charAt(pos);
            if (ch == '"') {
                wasQuote = !wasQuote;
            } else {
                if (wasQuote) {
                    return pos - 1;
                }
            }
        }
        return pos;
    }
    
    public static void runCommands(PTextArea textArea, String commands) {
        CommandInterpreter interp = new CommandInterpreter(commands);
        while (true) {
            Map<String, String> command = interp.next();
            if (command == null) {
                return;
            }
            runCommand(textArea, command);
        }
    }
    
    private static void runCommand(PTextArea textArea, Map<String, String> values) {
        String command = values.get("command");
        try {
            if ("replace".equals(command)) {
                runReplaceCommand(textArea, new TextRange(textArea, values), strValue(values, "new_text", ""));
            } else if ("select".equals(command)) {
                runSelectCommand(textArea, new TextRange(textArea, values));
            } else {
                Log.warn("Unknown command \"" + command + "\" from tool.");
            }
        } catch (Exception ex) {
            Log.warn("Failed to exec \"" + command + "\" command from tool", ex);
        }
    }
    
    private static void runReplaceCommand(PTextArea textArea, TextRange range, String newText) {
        textArea.replaceRange(newText, range.start, range.end);
    }
    
    private static void runSelectCommand(PTextArea textArea, TextRange range) {
        textArea.select(range.start, range.end);
    }
    
    private static int intValue(Map<String, String> values, String key) {
        return intValue(values, key, -1);
    }
    
    private static int intValue(Map<String, String> values, String key, int dflt) {
        String value = values.get(key);
        if (value == null) {
            return dflt;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return dflt;
        }
    }
    
    private static String strValue(Map<String, String> values, String key, String dflt) {
        String value = values.get(key);
        return value == null ? dflt : value;
    }
    
    static class TextRange {
        int start;
        int end;
        
        TextRange(PTextArea textArea, Map<String, String> values) {
            int startLine = intValue(values, "start_line", -1);
            int startChar = intValue(values, "start_char", 0);
            int endLine = intValue(values, "end_line", startLine);
            int endChar = intValue(values, "end_char", startChar);
            // Line number -> index.
            startLine--;
            endLine--;
            // Char index in whole characters -> number of char values.
            startChar = Character.offsetByCodePoints(textArea.getLineContents(startLine), 0, startChar);
            endChar = Character.offsetByCodePoints(textArea.getLineContents(endLine), 0, endChar);
            this.start = textArea.getLineStartOffset(startLine) + startChar;
            this.end = textArea.getLineStartOffset(endLine) + endChar;
        }
    }
}
