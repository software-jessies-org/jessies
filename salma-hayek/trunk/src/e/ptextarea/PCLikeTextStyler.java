package e.ptextarea;

import java.util.*;
import java.util.regex.*;
import java.util.List;
import e.util.*;

/**
 * A PCLikeTextStyler does the main work for the C, C++ and Java stylers.  The C, C++ and
 * Java subclasses provide only information about valid keywords.  This class understands the
 * single- and multi-line comment structures, quoted strings, and how to find keywords in what's
 * left over.
 * 
 * @author Phil Norman
 */

public abstract class PCLikeTextStyler extends PAbstractTextStyler implements PTextListener {
    private HashSet keywords = new HashSet();
    private int lastGoodLine;
    private boolean[] commentCache;
    private Pattern keywordPattern = Pattern.compile("\\b\\w+\\b");
    
    public PCLikeTextStyler(PTextArea textArea) {
        super(textArea);
        initCommentCache();
        textArea.getTextBuffer().addTextListener(this);
        textArea.setTextStyler(this);
    }
    
    protected void addKeywords(String[] keywordList) {
        for (int i = 0; i < keywordList.length; i++) {
            keywords.add(keywordList[i]);
        }
    }
    
    /**
     * Returns true if the styler should comment to end of line on seeing '#'.
     */
    public abstract boolean supportShellComments();
    
    /**
     * Adds a text segment of type String to the given segment list.  Override
     * this method if you wish to perform some validation on the string and introduce
     * error-style sections into it.
     */
    public void addStringSegment(TextSegmentListBuilder builder, String line, int start, int end) {
        builder.addStyledSegment(end, PStyle.STRING);
    }
    
    private void initCommentCache() {
        lastGoodLine = 0;
        commentCache = new boolean[100];
    }
    
    private void ensureCommentCacheLength(int index) {
        if (index >= commentCache.length) {
            boolean[] newCache = new boolean[index + 100];
            System.arraycopy(commentCache, 0, newCache, 0, commentCache.length);
            commentCache = newCache;
        }
    }
    
    public PTextSegment[] getTextSegments(int lineIndex) {
        String line = textArea.getLineContents(lineIndex).toString();
        List result = getMainSegments(lineIndex, line);
        if (keywords.size() > 0) {
            List mainSegments = result;
            result = new ArrayList();
            for (int i = 0; i < mainSegments.size(); i++) {
                PTextSegment mainSegment = (PTextSegment) mainSegments.get(i);
                if (mainSegment.getStyle() == PStyle.NORMAL) {
                    result.addAll(getKeywordAddedSegments(mainSegment));
                } else {
                    result.add(mainSegment);
                }
            }
        }
        return (PTextSegment[]) result.toArray(new PTextSegment[result.size()]);
    }
    
    private List getKeywordAddedSegments(PTextSegment segment) {
        ArrayList result = new ArrayList();
        String text = segment.getText();
        Matcher matcher = keywordPattern.matcher(text);
        int normalStart = 0;
        int offset = segment.getOffset();
        while (matcher.find()) {
            String keyword = matcher.group();
            if (keywords.contains(keyword)) {
                if (matcher.start() > normalStart) {
                    result.add(segment.subSegment(normalStart, matcher.start()));
                }
                result.add(new PTextSegment(textArea, offset + matcher.start(), offset + matcher.end(), PStyle.KEYWORD));
                normalStart = matcher.end();
            }
        }
        if (segment.getText().length() > normalStart) {
            result.add(segment.subSegment(normalStart));
        }
        return result;
    }
    
    private List getMainSegments(int lineIndex, String line) {
        TextSegmentListBuilder builder = new TextSegmentListBuilder(textArea.getLineStartOffset(lineIndex));
        boolean comment = startsCommented(lineIndex);
        int lastStart = 0;
        for (int i = 0; i < line.length(); ) {
            if (comment) {
                int commentEndIndex = line.indexOf("*/", i);
                if (commentEndIndex == -1) {
                    commentEndIndex = line.length();
                } else {
                    commentEndIndex += 2;
                }
                builder.addStyledSegment(commentEndIndex, PStyle.COMMENT);
                i = commentEndIndex;
                lastStart = commentEndIndex;
                comment = false;
            } else {
                char ch = line.charAt(i);
                if (supportShellComments() && ch == '#') {
                    comment = true;
                    if (lastStart < i) {
                        builder.addStyledSegment(i, PStyle.NORMAL);
                    }
                    builder.addStyledSegment(line.length(), PStyle.COMMENT);
                    i = line.length();
                    lastStart = i;
                } else if (ch == '/') {
                    if (i < line.length() - 1) {
                        if (line.charAt(i + 1) == '*') {
                            comment = true;
                            if (lastStart < i) {
                                builder.addStyledSegment(i, PStyle.NORMAL);
                            }
                            lastStart = i;
                            i += 2;
                        } else if (line.charAt(i + 1) == '/') {
                            if (lastStart < i) {
                                builder.addStyledSegment(i, PStyle.NORMAL);
                            }
                            builder.addStyledSegment(line.length(), PStyle.COMMENT);
                            i = line.length();
                            lastStart = i;
                        } else {
                            i++;
                        }
                    } else {
                        i++;
                    }
                } else if (ch == '"' || ch == '\'') {
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
                        addStringSegment(builder, line, i, stringEnd);
                        i = stringEnd;
                    }
                    lastStart = i;
                } else {
                    i++;
                }
            }
        }
        if (lastStart < line.length()) {
            builder.addStyledSegment(line.length(), comment ? PStyle.COMMENT : PStyle.NORMAL);
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
    
    private boolean startsCommented(int lineIndex) {
        if (lastGoodLine < lineIndex) {
            ensureCommentCacheLength(lineIndex);
            PLineList lineList = textArea.getLineList();
            for (int i = lastGoodLine; i < lineIndex; i++) {
                String line = lineList.getLine(i).getContents().toString();
                commentCache[i + 1] = lineEndsCommented(line, commentCache[i]);
            }
            lastGoodLine = lineIndex;
        }
        return commentCache[lineIndex];
    }
    
    /**
     * Returns true if the given line will end commented. By "end commented",
     * I think this means "end in an open comment that implies that the next
     * line begins inside a comment".
     */
    private boolean lineEndsCommented(String line, boolean startsCommented) {
        boolean comment = startsCommented;
        int index = 0;
        while (true) {
            if (comment) {
                // Commented - comments eat strings.
                int endIndex = line.indexOf("*/", index);
                if (endIndex == -1) {
                    break;
                }
                comment = false;
                index = endIndex + 2;
            } else {
                // Uncommented - strings eat comments.
                char previous = 0;
                char lastQuote = 0;
                boolean escaped = false;
                for (int i = index; i < line.length(); i++) {
                    char thisChar = line.charAt(i);
                    if (lastQuote == 0) {
                        if (escaped == false && isQuote(thisChar)) {
                            lastQuote = thisChar;
                        }
                        if (previous == '/' && thisChar == '*') {
                            comment = true;
                            index = i + 1;
                            break;
                        } else if (previous == '/' && thisChar == '/') {
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
                    previous = thisChar;
                }
                if (comment == false) {
                    break;
                }
            }
        }
        return comment;
    }
    
    /** Returns true iff the given char is a quote of some sort. */
    public boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"');
    }

    public void textInserted(PTextEvent event) {
        dirtyFromOffset(event);
    }
    
    public void textRemoved(PTextEvent event) {
        dirtyFromOffset(event);
    }
    
    private void dirtyFromOffset(PTextEvent event) {
        if (textArea.isLineWrappingInvalid()) {
            return;
        }
        CharSequence seq = textArea.getTextBuffer();
        StringBuffer buf = new StringBuffer();
        String prefix = seq.subSequence(Math.max(0, event.getOffset() - 2), event.getOffset()).toString();
        int endIndex = event.getOffset();
        if (event.isInsert()) {
            endIndex += event.getLength();
        }
        String suffix = seq.subSequence(endIndex, Math.min(endIndex + 1, seq.length())).toString();
        String withMiddleText = prefix + event.getCharacters() + suffix;
        String withoutMiddleText = prefix + suffix;
        if (hasCommentMarker(withMiddleText) || hasCommentMarker(withoutMiddleText) || hasNewline(event.getCharacters())) {
            lastGoodLine = Math.min(lastGoodLine, textArea.getLineList().getLineIndex(event.getOffset()));
            textArea.repaintFromLine(textArea.getSplitLineIndex(lastGoodLine));
        }
    }
    
    private boolean hasNewline(CharSequence text) {
        return StringUtilities.contains(text, '\n');
    }
    
    private boolean hasCommentMarker(String text) {
        return (text.indexOf("/*") != -1) || (text.indexOf("*/") != -1);
    }
    
    public void textCompletelyReplaced(PTextEvent event) {
        initCommentCache();
    }
    
    protected class TextSegmentListBuilder {
        private ArrayList list = new ArrayList();
        private int lineStartOffset;
        private int start = 0;
        
        public TextSegmentListBuilder(int lineStartOffset) {
            this.lineStartOffset = lineStartOffset;
        }
        
        public void addStyledSegment(int end, PStyle style) {
            list.add(new PTextSegment(textArea, lineStartOffset + start, lineStartOffset + end, style));
            start = end;
        }
        
        public List getSegmentList() {
            return list;
        }
    }
}
