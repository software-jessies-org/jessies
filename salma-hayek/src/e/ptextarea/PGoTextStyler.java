package e.ptextarea;

import java.util.*;
import e.util.*;

public class PGoTextStyler extends PAbstractTextStyler {
    private int lastGoodLine;
    private BitSet mlStringCache;  // multi-line strings ('`...`')

    public PGoTextStyler(PTextArea textArea) {
        super(textArea);
        if (textArea != null) {
            initMLStringCache();
            initTextListener();
            textArea.setTextStyler(this);
        }
    }

    private void initMLStringCache() {
        lastGoodLine = 0;
        mlStringCache = new BitSet();
    }

    public void initStyleApplicators() {
        Set<String> keywords = new HashSet<String>();
        keywords.addAll(Arrays.asList(getKeywords()));
        textArea.addStyleApplicator(new KeywordStyleApplicator(textArea, keywords, "\\b(\\w+)\\b"));
    }

    public String[] getKeywords() {
        return new String[] {
            // From https://golang.org/ref/spec#Keywords
            "break", "default", "func", "interface", "select",
            "case", "defer", "go", "map", "struct",
            "chan", "else", "goto", "package", "switch",
            "const", "fallthrough", "if", "range", "type",
            "continue", "for", "import", "return", "var",

            // Plus types:
            "bool", "byte", "rune",
            "uint8", "uint16", "uint32", "uint64",
            "int8", "int16", "int32", "int64",
            "float32", "float64", "complex64", "complex128",
            "uint", "int", "uintptr",
            "string", "error",

            // And fixed values:
            "false", "true", "nil",
        };
    }

    public List<PLineSegment> getTextSegments(int lineIndex) {
        String line = textArea.getLineContents(lineIndex).toString();
        TextSegmentListBuilder builder = new TextSegmentListBuilder(textArea.getLineStartOffset(lineIndex));
        boolean mlString = startsInMLString(lineIndex);
        int lastStart = 0;
        for (int i = 0; i < line.length(); ) {
            if (mlString) {
                int mlStringEndIndex = line.indexOf('`', i);
                if (mlStringEndIndex == -1) {
                    mlStringEndIndex = line.length();
                } else {
                    mlStringEndIndex++;
                }
                builder.addStyledSegment(mlStringEndIndex, PStyle.STRING);
                i = mlStringEndIndex;
                lastStart = mlStringEndIndex;
                mlString = false;
            } else {
                if (isStartOfCommentToEndOfLine(line, i)) {
                    mlString = true;
                    if (lastStart < i) {
                        builder.addStyledSegment(i, PStyle.NORMAL);
                    }
                    builder.addStyledSegment(line.length(), PStyle.COMMENT);
                    i = line.length();
                    lastStart = i;
                    break;
                }

                if (line.charAt(i) == '`') {
                    mlString = true;
                    if (lastStart < i) {
                        builder.addStyledSegment(i, PStyle.NORMAL);
                    }
                    lastStart = i;
                    i++;
                } else if (isQuote(line.charAt(i))) {
                    if (lastStart < i) {
                        builder.addStyledSegment(i, PStyle.NORMAL);
                    }
                    int stringEnd = i + 1;
                    String matchString = String.valueOf(line.charAt(i));
                    while (stringEnd != -1) {
                        stringEnd = line.indexOf(matchString, stringEnd);
                        if (stringEnd != -1) {
                            stringEnd++;
                            if (getBackslashBeforeCount(line, stringEnd - 1) % 2 == 0) {
                                break;  // Not escaped.
                            }
                        }
                    }
                    // If it falls out because stringEnd == -1, we have an unterminated string.
                    if (stringEnd == -1) {
                        builder.addStyledSegment(line.length(), PStyle.ERROR);
                        i = line.length();
                    } else {
                        builder.addStyledSegment(stringEnd, PStyle.STRING);
                        i = stringEnd;
                    }
                    lastStart = i;
                } else {
                    i++;
                }
            }
        }
        if (lastStart < line.length()) {
            builder.addStyledSegment(line.length(), mlString ? PStyle.STRING : PStyle.NORMAL);
        }
        return builder.getSegmentList();
    }

    private int getBackslashBeforeCount(String string, int index) {
        int result = 0;
        for (int i = index - 1; i >= 0; i--) {
            if (string.charAt(i) == '\\') {
                result++;
            } else {
                break;
            }
        }
        return result;
    }

    private void initTextListener() {
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textCompletelyReplaced(PTextEvent event) {
                initMLStringCache();
            }

            public void textInserted(PTextEvent event) {
                dirtyFromOffset(event);
            }

            public void textRemoved(PTextEvent event) {
                dirtyFromOffset(event);
            }
        });
    }

    private void dirtyFromOffset(PTextEvent event) {
        if (textArea.isLineWrappingInvalid()) {
            return;
        }
        if (StringUtilities.contains(event.getCharacters(), '`')) {
            lastGoodLine = Math.min(lastGoodLine, textArea.getLineList().getLineIndex(event.getOffset()));
            textArea.repaintFromLine(textArea.getSplitLineIndex(lastGoodLine));
        }
    }

    private boolean startsInMLString(int lineIndex) {
        if (lastGoodLine < lineIndex) {
            PLineList lineList = textArea.getLineList();
            for (int i = lastGoodLine; i < lineIndex; i++) {
                String line = lineList.getLineContents(i).toString();
                mlStringCache.set(i + 1, lineEndsInMLString(line, mlStringCache.get(i)));
            }
            lastGoodLine = lineIndex;
        }
        return mlStringCache.get(lineIndex);
    }

    private boolean lineEndsInMLString(String line, boolean startsInMLString) {
        boolean inString = startsInMLString;
        int index = 0;
        while (true) {
            if (inString) {
                // InMLString - ML strings eat everything.
                int endIndex = line.indexOf('`', index);
                if (endIndex == -1) {
                    break;
                }
                inString = false;
                index = endIndex + 1;
            } else {
                // Not in ML string - strings and comments eat ML edges.
                char lastQuote = 0;
                boolean escaped = false;
                for (; index < line.length(); index++) {
                    char thisChar = line.charAt(index);
                    if (lastQuote == 0) {
                        if (escaped == false && isQuote(thisChar)) {
                            lastQuote = thisChar;
                        }
                        if (line.charAt(index) == '`') {
                            inString = true;
                            index++;
                        } else if (isStartOfCommentToEndOfLine(line, index)) {
                            break;
                        }
                    } else {
                        if (escaped == false && thisChar == lastQuote) {
                            lastQuote = 0;
                        }
                    }
                    if (thisChar == '\\') {
                        escaped = !escaped;
                    } else {
                        escaped = false;
                    }
                }
                if (inString == false) {
                    break;
                }
            }
        }
        return inString;
    }

    protected boolean isStartOfCommentToEndOfLine(String line, int atIndex) {
        return line.startsWith("//", atIndex);
    }

    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"');
    }
}
