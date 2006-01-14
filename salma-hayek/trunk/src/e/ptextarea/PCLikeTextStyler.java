package e.ptextarea;

import java.util.*;
import java.util.regex.*;
import e.util.*;

/**
 * A PCLikeTextStyler does the main work for the C, C++ and Java stylers.  The
 * various subclasses (supporting languages such as C++ and Java) configure
 * functionality implemented here.  This class understands the single- and
 * multi-line comment structures, quoted strings, and how to find keywords in
 * what's left over.
 * 
 * FIXME: it would be nice to support arbitrary multiline comment styles, such as HTML's "<!--" and "-->". (That's probably the only other one worth worrying about. Perl and Ruby have various multiline quoting mechanisms, but they're a lesser priority than having an HTML styler.)
 * 
 * @author Phil Norman
 */
public abstract class PCLikeTextStyler extends PAbstractTextStyler {
    private HashSet<String> keywords = new HashSet<String>();
    private int lastGoodLine;
    private BitSet commentCache;
    
    public PCLikeTextStyler(PTextArea textArea) {
        super(textArea);
        initCommentCache();
        initStyleApplicator();
        initTextListener();
        textArea.setTextStyler(this);
    }
    
    /**
     * Returns true if the styler should comment to end of line on seeing '#'.
     */
    protected abstract boolean supportShellComments();
    
    /**
     * Returns true if the styler should comment to end of line on seeing '//'.
     */
    protected abstract boolean supportDoubleSlashComments();
    
    /**
     * Returns true if the style should count text in a C-like comment (such as this) as comment.
     */
    protected abstract boolean supportSlashStarComments();
    
    /**
     * Adds a text segment of type String to the given segment list.  Override
     * this method if you wish to perform some validation on the string and introduce
     * error-style sections into it.
     */
    public void addStringSegment(TextSegmentListBuilder builder, String line, int start, int end) {
        builder.addStyledSegment(end, PStyle.STRING);
    }
    
    private void initStyleApplicator() {
        addKeywordsTo(keywords);
        if (keywords.size() > 0) {
            textArea.addStyleApplicator(new KeywordStyleApplicator(textArea, keywords, getKeywordRegularExpression()));
        }
    }
    
    private void initTextListener() {
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textCompletelyReplaced(PTextEvent event) {
                initCommentCache();
            }
            
            public void textInserted(PTextEvent event) {
                dirtyFromOffset(event);
            }
            
            public void textRemoved(PTextEvent event) {
                dirtyFromOffset(event);
            }
        });
    }
    
    // This is parameterized so that we can recognize the GNU make keyword "filter-out".
    protected String getKeywordRegularExpression() {
        return "\\b(\\w+)\\b";
    }
    
    private void initCommentCache() {
        lastGoodLine = 0;
        commentCache = new BitSet();
    }
    
    public List<PLineSegment> getTextSegments(int lineIndex) {
        String line = textArea.getLineContents(lineIndex).toString();
        return getMainSegments(lineIndex, line);
    }
    
    private List<PLineSegment> getMainSegments(int lineIndex, String line) {
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
                        if (supportSlashStarComments() && line.charAt(i + 1) == '*') {
                            comment = true;
                            if (lastStart < i) {
                                builder.addStyledSegment(i, PStyle.NORMAL);
                            }
                            lastStart = i;
                            i += 2;
                        } else if (supportDoubleSlashComments() && line.charAt(i + 1) == '/') {
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
                } else if (isQuote(ch)) {
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
            PLineList lineList = textArea.getLineList();
            for (int i = lastGoodLine; i < lineIndex; i++) {
                String line = lineList.getLine(i).getContents().toString();
                commentCache.set(i + 1, lineEndsCommented(line, commentCache.get(i)));
            }
            lastGoodLine = lineIndex;
        }
        return commentCache.get(lineIndex);
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
                        if (supportSlashStarComments() && previous == '/' && thisChar == '*') {
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
    
    /**
     * Returns true iff the given character is a quote of some sort.
     * It's safe to override this and increase or reduce the number of quote characters.
     * The makefile styler removes single quotes, for example, and the scripting language stylers add backquote.
     */
    protected boolean isQuote(char ch) {
        return (ch == '\'' || ch == '\"');
    }
    
    private void dirtyFromOffset(PTextEvent event) {
        if (textArea.isLineWrappingInvalid()) {
            return;
        }
        CharSequence seq = textArea.getTextBuffer();
        StringBuilder buf = new StringBuilder();
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
        return text.contains("/*") || text.contains("*/");
    }
    
    protected class TextSegmentListBuilder {
        private ArrayList<PLineSegment> list = new ArrayList<PLineSegment>();
        private int lineStartOffset;
        private int start = 0;
        
        public TextSegmentListBuilder(int lineStartOffset) {
            this.lineStartOffset = lineStartOffset;
        }
        
        public void addStyledSegment(int end, PStyle style) {
            list.add(new PTextSegment(textArea, lineStartOffset + start, lineStartOffset + end, style));
            start = end;
        }
        
        public List<PLineSegment> getSegmentList() {
            return list;
        }
    }
}
