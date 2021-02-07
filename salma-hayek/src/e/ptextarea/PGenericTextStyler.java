package e.ptextarea;

import java.util.*;

public abstract class PGenericTextStyler implements PTextStyler {
    protected PTextArea textArea;
    
    // lastGoodLine is the index of the last line for which we are sure the lineEndContexts map is
    // correctly populated. This means that the last line index for which we can generate correct
    // stylings is lastGoodLine+1.
    private int lastGoodLine;
    private TreeMap<Integer, PSequenceMatcher.RegionEnd> lineEndContexts;
    
    public PGenericTextStyler(PTextArea textArea) {
        this.textArea = textArea;
        if (textArea != null) {
            initLineEndContexts();
            initTextListener();
            textArea.setTextStyler(this);
        }
    }
    
    public List<PLineSegment> getTextSegments(int line) {
        while ((lastGoodLine + 1) < line) {
            // Need to bring the lineEndContexts up to the current line.
            TextSegmentListBuilder builder = new TextSegmentListBuilder(line);  // We will discard this.
            lineEndContexts.put(lastGoodLine + 1, processLine(lastGoodLine + 1, builder));
            lastGoodLine++;
        }
        TextSegmentListBuilder builder = new TextSegmentListBuilder(line);
        lineEndContexts.put(line, processLine(line, builder));
        lastGoodLine = Math.max(lastGoodLine, line);
        return builder.getSegmentList();
    }
    
    public boolean keywordsAreCaseSensitive() {
        return true;
    }
    
    private void initTextListener() {
        textArea.getTextBuffer().addTextListener(new PTextListener() {
            public void textCompletelyReplaced(PTextEvent event) {
                initLineEndContexts();
            }
            
            public void textInserted(PTextEvent event) {
                dirtyFromOffset(event);
            }
            
            public void textRemoved(PTextEvent event) {
                dirtyFromOffset(event);
            }
        });
    }
    
    public void initStyleApplicators() {
        Set<String> keywords;
        if (keywordsAreCaseSensitive()) {
            keywords = new HashSet<String>();
        } else {
            keywords = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        }
        keywords.addAll(Arrays.asList(getKeywords()));
        if (keywords.size() > 0) {
            textArea.addStyleApplicator(new KeywordStyleApplicator(textArea, keywords, getKeywordRegularExpression()));
        }
    }
    
    private void initLineEndContexts() {
        lastGoodLine = 0;
        lineEndContexts = new TreeMap<Integer, PSequenceMatcher.RegionEnd>();
    }
    
    private void dirtyFromOffset(PTextEvent event) {
        if (textArea.isLineWrappingInvalid()) {
            return;
        }
        final int line = textArea.getLineList().getLineIndex(event.getOffset());
        if (line >= lastGoodLine) {
            return;
        }
        // If any newlines were added or removed, then just invalidate anything after here.
        // It's easier, and is costly only in less-common cases.
        if (containsNewline(event.getCharacters())) {
            setLastGoodLine(line - 1);
            // If newlines were added or removed, then the text area will be repainting everything
            // from this point on anyway, so there's no point at all in triggering a repaint from here.
            return;
        }
        // The insert or remove was purely within this line. Check whether the end style
        // has changed or not. If it has, then invalidate everything from this point and
        // force a repaint.
        TextSegmentListBuilder builder = new TextSegmentListBuilder(line);
        PSequenceMatcher.RegionEnd regionEnd = processLine(line, builder);
        if (areEqual(regionEnd, lineEndContexts.get(line))) {
            return; // Nothing to do - all is as it was.
        }
        setLastGoodLine(line - 1);
        textArea.repaintFromLine(textArea.getSplitLineIndex(line - 1));
    }
    
    private void setLastGoodLine(int lastGoodLine) {
        this.lastGoodLine = lastGoodLine;
        while (!lineEndContexts.isEmpty()) {
            int top = lineEndContexts.lastKey();
            if (top <= lastGoodLine) {
                break;
            }
            lineEndContexts.remove(top);
        }
    }
    
    private boolean containsNewline(CharSequence seq) {
        for (int i = 0; i < seq.length(); i++) {
            if (seq.charAt(i) == '\n') {
                return true;
            }
        }
        return false;
    }
    
    /**
     * This is parameterized so that we can recognize the GNU Make keyword "filter-out", and various strange GNU Assembler directives.
     * The value of the first capturing group will be tested to ensure that it's a member of the styler's keyword set.
     */
    protected String getKeywordRegularExpression() {
        return "\\b(\\w+)\\b";
    }
    
    private PSequenceMatcher.RegionEnd processLine(int line, TextSegmentListBuilder builder) {
        String str = textArea.getLineList().getLineContents(line).toString();
        PSequenceMatcher.RegionEnd endFinder = lineEndContexts.get(line - 1);
        int index = 0;
        if (endFinder != null) {
            index = endFinder.getEndIndexForLine(str);
            builder.addStyledSegment((index == -1) ? str.length() : index, endFinder.getStyle());
            if (index == -1) {
                // We've not found the end of the styled segment yet.
                // Return the same finder again so it will be applied to the next line.
                return endFinder;
            }
        }
        for (; index < str.length(); index++) {
            for (PSequenceMatcher matcher : getLanguageSequenceMatchers()) {
                PSequenceMatcher.RegionEnd end = matcher.match(str, index);
                if (end == null) {
                    continue;
                }
                // We got a match. The first thing we need to do is output the default-styled text up to this point.
                builder.addStyledSegment(index, PStyle.NORMAL);
                index = end.getEndIndex();  // -1 if it extends beyond this line.
                builder.addStyledSegment((index == -1) ? str.length() : index, end.getStyle());
                // If the index _is_ -1, then we return the finder, so it'll be used to check for the end
                // in the next line.
                if (index == -1) {
                    return end;
                }
                // If we got here, index is now after the styled segment we just picked up.
                // We have to break out of the loop to get to the next iteration of the 'index' for loop.
                break;
            }
        }
        // If we got here, the line ended outside a multi-line block, so we have to add a segment for
        // whatever text is at the end.
        // In some situations, index might be off the end of the string (if a styled section goes up to the end
        // of the line), so use str.length() here, _not_ index.
        builder.addStyledSegment(str.length(), PStyle.NORMAL);
        return null;
    }
    
    protected abstract List<PSequenceMatcher> getLanguageSequenceMatchers();
    
    protected class TextSegmentListBuilder {
        private ArrayList<PLineSegment> list = new ArrayList<>();
        private int lineStartOffset;
        private int start = 0;
        
        public TextSegmentListBuilder(int lineIndex) {
            // Get the index of the first char in this line from the start of the text buffer.
            this.lineStartOffset = textArea.getLineStartOffset(lineIndex);
        }
        
        public void addStyledSegment(int end, PStyle style) {
            if (end == start) {
                return;
            }
            list.add(new PTextSegment(textArea, lineStartOffset + start, lineStartOffset + end, style));
            start = end;
        }
        
        public List<PLineSegment> getSegmentList() {
            return list;
        }
    }
    
    protected static boolean areEqual(Object one, Object two) {
        return (one == null || two == null) ? (one == two) : one.equals(two);
    }
}
