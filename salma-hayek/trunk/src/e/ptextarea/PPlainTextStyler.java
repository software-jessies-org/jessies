package e.ptextarea;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A trivial styler for plain text, which colors all text black.
 */
public class PPlainTextStyler extends PAbstractTextStyler {
    private static final PTextSegment[] EMPTY_LINE_SEGMENTS = new PTextSegment[0];

    public PPlainTextStyler(PTextArea textArea) {
        super(textArea);
    }

    public List<PLineSegment> getTextSegments(int line) {
        int start = textArea.getLineStartOffset(line);
        int end = textArea.getLineEndOffsetBeforeTerminator(line);
        List<PLineSegment> result = new ArrayList<PLineSegment>();
        result.add(new PTextSegment(textArea, start, end, PStyle.NORMAL));
        return result;
    }

    public Color getColorForStyle(int style) {
        return Color.BLACK;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        // We have no language, so we have no keywords.
    }
}
