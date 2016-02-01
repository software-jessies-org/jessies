package e.ptextarea;

import e.util.*;
import java.util.*;

/**
 * Implements indentation for members of the C family, parameterized to cater for their differences.
 */
public abstract class PCFamilyIndenter extends PSimpleIndenter {
    private static final String ALIGN_FUNCTION_ARGS = "alignFunctionArgs";
    private static final String NAMESPACE_INDENT = "namespaceIndent";
    private static final String BRACE_INDENT = "braceIndent";
    private static final String SQUARE_BRACKET_INDENT = "squareBracketIndent";
    private static final String PARENTHESIS_INDENT = "parenthesisIndent";
    private static final String SWITCH_LABEL_OUTDENT = "switchLabelOutdent";
    private static final String ACCESS_SPECIFIER_OUTDENT = "accessSpecifierOutdent";

    public PCFamilyIndenter(PTextArea textArea) {
        super(textArea);
    }

    @Override
    public ArrayList<Preference> getPreferences() {
        ArrayList<Preference> result = super.getPreferences();
        result.add(new Preference(ALIGN_FUNCTION_ARGS, Boolean.TRUE, "Align args in multi-line function calls"));
        result.add(new Preference(NAMESPACE_INDENT, "", "Indent after C++ 'namespace xx {' lines"));
        result.add(new Preference(BRACE_INDENT, "    ", "Indent after an open curly brace"));
        result.add(new Preference(SQUARE_BRACKET_INDENT, "    ", "Indent after an open square bracket"));
        result.add(new Preference(PARENTHESIS_INDENT, "    ", "Indent after round brackets (also expression continuation)"));
        result.add(new Preference(SWITCH_LABEL_OUTDENT, "", "Outdent for 'case' and 'default' switch labels"));
        result.add(new Preference(ACCESS_SPECIFIER_OUTDENT, " ", "Outdent for access specifiers"));
        return result;
    }

    private void debug(CharSequence message) {
        if (false) {
            System.err.println(message);
        }
    }

    @Override public boolean isElectric(char c) {
        FileType fileType = textArea.getFileType();
        if ((c == '#' || c == '<') && (fileType == FileType.C_PLUS_PLUS)) {
            // C++ wants special handling of operator<< and pre-processor directives.
            return true;
        }
        if (c == ':' && (fileType == FileType.C_PLUS_PLUS || fileType == FileType.JAVA)) {
            // C++ and Java want special handling of goto and switch labels, and C++ access specifiers.
            return true;
        }
        if (PBracketUtilities.isCloseBracket(c)) {
            return true;
        }
        return false;
    }

    public boolean isBlockBegin(String activePartOfLine) {
        if (activePartOfLine.length() == 0) {
            return false;
        }
        char lastChar = activePartOfLine.charAt(activePartOfLine.length() - 1);
        if ((lastChar == '<') && (activePartOfLine.length() >= 2)) {
            // We must not consider left-shifts to be block starts.
            return (activePartOfLine.charAt(activePartOfLine.length() - 2) != '<');
        } else {
            return PBracketUtilities.isOpenBracket(lastChar);
        }
    }

    public boolean isSwitchLabel(String activePartOfLine) {
        return activePartOfLine.matches("(case\\b.*|default\\s*):.*");
    }

    /*
     * Returns the contents of the indexed line, after the following changes have been made:
     * 1: Any comment will have been replaced entirely with spaces.
     * 2: Any string will be replaced with a beginning and ending double-quote, and spaces for contents.
     * 3: Any trailing whitespace will have been removed.
     * The effect of 1 and 3 combined is that any line which consists solely of a comment and/or whitespace
     * will be returned as an empty string.
     */
    private String extractEffectivePartOfLine(int lineIndex) {
        List<PLineSegment> segments = textArea.getLineSegments(lineIndex);
        StringBuilder result = new StringBuilder(256);  // Initialize with some sensible capacity.
        for (PLineSegment segment: segments) {
            if (segment.getStyle() == PStyle.PREPROCESSOR) {
                // Any lines which contain preprocessor stuff can be dropped for indentation purposes.
                return "";
            }
            if (segment.getStyle() == PStyle.COMMENT || segment.getStyle() == PStyle.HYPERLINK) {
                // Ignore comments completely.  Just in case there's a /* */ comment in the middle of some code, we replace the
                // comment string with the equivalent amount of whitespace, as the extra characters may have some effect on
                // indentation if there's an open bracket after them.
                result.append(StringUtilities.nCopies(segment.getCharSequence().length(), ' '));
            } else if (segment.getStyle() == PStyle.STRING) {
                // If we see a string, we just store a space-containing string of the same length.
                // This is to get rid of any brackets or other indentation-changing characters.
                // We keep the string's length the same, as it may have an effect on indentation.
                CharSequence string = segment.getCharSequence();
                if (string.length() < 2) {
                    // This shouldn't be possible.
                    result.append(string);
                } else {
                    result.append('"');
                    result.append(StringUtilities.nCopies(string.length() - 2, ' '));
                    result.append('"');
                }
            } else {
                result.append(segment.getCharSequence());
            }
        }
        return StringUtilities.trimTrailingWhitespace(result.toString());
    }

    public LinkedList<String> extractPreviousStatements(int lineIndex) {
        LinkedList<String> result = new LinkedList<String>();
        Stack<Character> brackets = new Stack<Character>();
        boolean terminate = false;
        int semicolonsSeen = 0;
        for (int i = lineIndex - 1; i >= 0 && !terminate; i--) {
            String line = extractEffectivePartOfLine(i);
            // Ignore any lines which only contain comments and/or whitespace.
            if (line.isEmpty()) {
                continue;
            }
            for (int j = line.length() - 1; j >= 0; j--) {
                char ch = line.charAt(j);
                // For now, ignore < and > characters as, although they may affect indentation in C++ templates and
                // Java generics, they're more often used as greater/less than operators.
                if (ch == '>' || ch == '<') {
                    continue;
                }
                if (PBracketUtilities.isCloseBracket(ch)) {
                    // If this close bracket has nothing but whitespace to its left, we assume it's already indented at the
                    // correct level.
                    if (line.substring(0, j).matches("^ *$")) {
                        terminate = true;
                    }
                    brackets.push(PBracketUtilities.getPartnerForBracket(ch));
                } else if (PBracketUtilities.isOpenBracket(ch)) {
                    if (brackets.empty()) {
                        // TODO: think.
                    } else {
                        if (brackets.peek() == ch) {
                            brackets.pop();
                        } else {
                            // If the open bracket doesn't match a later closed bracket, we assume that the code we're
                            // writing is part-way through a statement of some kind.  If the open bracket is the first thing
                            // on the line, we're going to assume its indentation is definitive, so we'll include it and nothing
                            // before it in the result.
                            if (line.substring(0, j).matches("^ *$")) {
                                terminate = true;
                            }
                        }
                    }
                }
            }
            result.addFirst(line);
            // If there are no unmatched close brackets left, we count how many lines we've seen which end in a
            // semicolon.  We need to be careful of for loops, which in the worst case can look like this:
            //  for (
            //     int x = 0;
            //     x < 10;
            //     ++x)
            // So if we count 3 lines ending in semicolons, and there are no unmatched close brackets, we can't be
            // returning something which starts in the middle of a for loop.
            if (brackets.empty() && line.endsWith(";")) {
                semicolonsSeen++;
                if (semicolonsSeen >= 3) {
                    terminate = true;
                }
            }

        }
        return result;
    }

    private static class Indent {
        // The 'openBracket' variable is either going to be:
        // '{' or '(' - an actual open bracket.
        // ':' - a case/default statement.
        // ';' - a line continuation indent.
        private final char openBracket;
        private final String indentation;
        private final String subIndent;

        public Indent(char openBracket, String indentation, String subIndent) {
            this.openBracket = openBracket;
            this.indentation = indentation;
            this.subIndent = subIndent;
        }

        public String toString() {
            return "Indent[char=" + openBracket + ", ilen=" + indentation.length() + ", sublen=" + subIndent.length() + "]";
        }

        public char getOpenBracket() {
            return openBracket;
        }

        public String getIndentation() {
            return indentation;
        }

        public String getSubIndent() {
            return subIndent;
        }
    }

    void printIndentStack(Stack<Indent> indents) {
        debug("Full indent stack:");
        for (Indent indent: indents) {
            debug("    " + indent.toString());
        }
        debug("<<<");
    }

    String calculateNewIndentation(LinkedList<String> previousStatements, String trimmedCurrentLine) {
        if (previousStatements.isEmpty()) {
            return "";
        }
        Stack<Indent> indentLevels = new Stack<Indent>();
        String defaultIndentation = PIndenter.indentationOf(previousStatements.getFirst());
        for (String line: previousStatements) {
            // We assume that each line in previousStatements is indented correctly, so we just record in our
            // stack the level of indentation applicable to each bracket.
            String indentation = PIndenter.indentationOf(line);
            if (isSwitchLabel(StringUtilities.trimLeadingWhitespace(line))) {
                debug("Line '" + line + "' has indent stack:");
                printIndentStack(indentLevels);
                Indent baseLevel = dropTrailingCaseOrDefaultOrContinuationIndent(indentLevels);
                debug("Got base level " + baseLevel);
                if (baseLevel != null) {
                    defaultIndentation = baseLevel.getIndentation();
                }
                indentLevels.push(new Indent(':', defaultIndentation, getOpenBracketIndent(':')));
            }
            // We can happily check each char, as we've neutralized brackets in comments and strings.
            for (int i = 0; i < line.length(); i++) {
                char ch = line.charAt(i);
                if (ch == '<' || ch == '>') {
                    // Don't deal with Java generics or C++ templates yet, as they're almost unparseable.
                    continue;
                }
                if (PBracketUtilities.isOpenBracket(ch)) {
                    if (isNamespace(line)) {
                        indentLevels.push(new Indent(ch, indentation, getNamespaceIndent()));
                    } else {
                        if (alignFunctionArgs() && i < line.length() - 1) {
                            // Text to the right of the open bracket, so align the next line to the same indent
                            // as the number of chars up to and including this bracket.
                            String subIndent = StringUtilities.nCopies(i + 1 - indentation.length(), ' ');
                            indentLevels.push(new Indent(ch, indentation, subIndent));
                            defaultIndentation = indentation + subIndent;
                        } else {
                            // Open bracket at the end of the line, so use the default indent.
                            String subIndent = getOpenBracketIndent(ch);
                            indentLevels.push(new Indent(ch, defaultIndentation, subIndent));
                            defaultIndentation = defaultIndentation + subIndent;
                        }
                    }
                } else {
                    if (PBracketUtilities.isCloseBracket(ch)) {
                        // This close bracket should correspond to the bottom-most open bracket on the stack.
                        // If it doesn't, just ignore it - the code is clearly in a slightly broken state.
                        char openCh = PBracketUtilities.getPartnerForBracket(ch);
                        dropTrailingCaseOrDefaultOrContinuationIndent(indentLevels);
                        if (indentLevels.empty()) {
                            // A closing bracket with no corresponding opening bracket.  If this bracket is
                            // the first non-whitespace character on the line, it must be a definitive indent
                            // position, so we override the defaultIndentation with this, so that if we see nothing
                            // further of interest, we at least use this level.
                            String lineBeforeBracket = line.substring(0, i);
                            if (StringUtilities.trimLeadingWhitespace(lineBeforeBracket).isEmpty()) {
                                defaultIndentation = lineBeforeBracket;
                            }
                        } else if (indentLevels.peek().getOpenBracket() == openCh) {
                            defaultIndentation = indentLevels.pop().getIndentation();
                            Indent baseLevel = dropTrailingCaseOrDefaultOrContinuationIndent(indentLevels);
                            if (baseLevel != null) {
                                defaultIndentation = baseLevel.getIndentation();
                            }
                        }
                    }
                }
            }
            // Check if the line ends with a ';' or a '}'.  If not (and if the line's not empty), the line contains a
            // statement which is continued onto the next line, so we must add a line continuation indent.
            if (statementContinuesOnNextLine(indentLevels, line) && isTemplate(line) == false) {
                indentLevels.push(new Indent(';', defaultIndentation, getOpenBracketIndent(';')));
            }
        }
        // If the current line is some kind of case or default label, or if it starts with a close parenthesis, remove
        // any case or default indentation levels, as we need to indent from the base of the switch statement block
        // in all these cases, to avoid double-indent.
        if (isSwitchLabel(trimmedCurrentLine) || trimmedCurrentLine.startsWith("}")) {
            Indent baseLevel = dropTrailingCaseOrDefaultOrContinuationIndent(indentLevels);
            if (baseLevel != null) {
                defaultIndentation = baseLevel.getIndentation();
            }
        }
        // If there's nothing left in the stack, we fall back to using the same indentation level as
        // the last definitive line.
        if (indentLevels.empty()) {
            return defaultIndentation;
        } else {
            // If we still have stuff on the stack, then the last element of these is the open bracket
            // we should derive our new indentation from.
            Indent indent = indentLevels.pop();
            return indent.getIndentation() + indent.getSubIndent();
        }
    }

    // Returns true if a continuation is needed, because 'line' is not terminated.
    // Note that in the case where the lowest element of indentLevels is a parenthesis, we
    // assume that if the last character in the line is a comma, this is a function argument
    // separator, and so , is considered in this case a line terminator.
    private boolean statementContinuesOnNextLine(Stack<Indent> indentLevels, String line) {
        if (line.isEmpty()) {
            return false;
        }
        char lastCh = line.charAt(line.length() - 1);
        if (lastCh == ',' && !indentLevels.empty()) {
            return indentLevels.peek().getOpenBracket() != '(';
        }
        return -1 == ":;}{(".indexOf(lastCh);
    }

    private Indent dropTrailingCaseOrDefaultOrContinuationIndent(Stack<Indent> indentLevels) {
        Indent result = null;
        while (!indentLevels.empty()) {
            char ch = indentLevels.peek().getOpenBracket();
            if (ch == ':' || ch == ';') {
                result = indentLevels.pop();
            } else {
                break;
            }
        }
        return result;
    }

    private boolean alignFunctionArgs() {
        return preferences.getBoolean(ALIGN_FUNCTION_ARGS);
    }

    private String getNamespaceIndent() {
        return preferences.getString(NAMESPACE_INDENT);
    }

    private String getSwitchLabelOutdent() {
        return preferences.getString(SWITCH_LABEL_OUTDENT);
    }

    private String getAccessSpecifierOutdent() {
        return preferences.getString(ACCESS_SPECIFIER_OUTDENT);
    }

    private String getOpenBracketIndent(char ch) {
        switch (ch) {
        case '{':
        case ':':
            return preferences.getString(BRACE_INDENT);
        case '[':
            return preferences.getString(SQUARE_BRACKET_INDENT);
        case '(':
        case ';':
            return preferences.getString(PARENTHESIS_INDENT);
        default:
            return "";
        }
    }

    /**
     * The 'indentation' is what's been worked out from the previous lines.
     * The 'trimmedLine' argument is all of the current line, but with any leading whitespace
     * removed, comments replaced with whitespace, and strings with their contents replaced with spaces.
     * The result of this method should be the whitespace to use to indent this line.
     */
    private String adjustedIndentationForCurrentLine(String indentation, String trimmedLine) {
        if (trimmedLine.isEmpty()) {
            return indentation;
        }
        String indentToRemove = "";
        if (PBracketUtilities.isCloseBracket(trimmedLine.charAt(0))) {
            indentToRemove = getOpenBracketIndent(PBracketUtilities.getPartnerForBracket(trimmedLine.charAt(0)));
        } else if (isSwitchLabel(trimmedLine)) {
            indentToRemove = getSwitchLabelOutdent();
        } else if (isAccessSpecifier(trimmedLine)) {
            indentToRemove = getAccessSpecifierOutdent();
        }
        return removeIndent(indentation, indentToRemove);
    }

    private String removeIndent(String indentation, String indentToRemove) {
        if (!indentToRemove.isEmpty()) {
            if (indentation.endsWith(indentToRemove)) {
                indentation = indentation.substring(0, indentation.length() - indentToRemove.length());
            }
        }
        return indentation;
    }

    // Overridden in the C++ indenter.
    protected boolean isAccessSpecifier(String activePartOfLine) {
        return false;
    }
    protected boolean isNamespace(String activePartOfLine) {
        return false;
    }
    protected boolean isTemplate(String activePartOfLine) {
        return false;
    }

    @Override public String calculateNewIndentation(int lineIndex) {
        debug("--------------------------------------------------------------------");
        FileType fileType = textArea.getFileType();

        String activePartOfLine = getActivePartOfLine(lineIndex);

        // A special case for C++'s preprocessor.
        if (fileType == FileType.C_PLUS_PLUS && activePartOfLine.startsWith("#")) {
            return "";
        }

        // A special case for C++'s operator<<.
        if (fileType == FileType.C_PLUS_PLUS && activePartOfLine.startsWith("<<")) {
            if (lineIndex == 0) {
                return "";
            }
            String previousLine = textArea.getLineText(lineIndex - 1);
            int previousOperatorOutIndex = previousLine.indexOf("<<");
            if (previousOperatorOutIndex != -1) {
                return StringUtilities.nCopies(previousOperatorOutIndex, ' ');
            }
        }

        // TODO: First, check if this line is within a multi-line comment, and if so, apply some comment-specific logic.
        // Then, fall back to the code case:
        LinkedList<String> previousStatements = extractPreviousStatements(lineIndex);
        debug("=======================");
        debug("Line " + (lineIndex + 1) + " has " + previousStatements.size() + " lines of previous statement:");
        for (String line: previousStatements) {
            debug(line);
        }
        debug(textArea.getLineContents(lineIndex));
        debug("=======================");

        String trimmedLine = StringUtilities.trimLeadingWhitespace(textArea.getLineContents(lineIndex).toString());
        String newIndentation = calculateNewIndentation(previousStatements, trimmedLine);
        debug("Calculated new indentation:");
        debug(newIndentation + trimmedLine);
        String effectiveLine = extractEffectivePartOfLine(lineIndex);

        return adjustedIndentationForCurrentLine(newIndentation, StringUtilities.trimLeadingWhitespace(effectiveLine));
    }

    protected abstract boolean isLabel(String activePartOfLine);
}
