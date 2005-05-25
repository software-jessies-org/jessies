package e.ptextarea;

import java.awt.*;
import java.util.*;

/**
 * A trivial styler for plain text, which colors all text black.
 */
public class PPlainTextStyler extends PAbstractTextStyler {
    private static final PTextSegment[] EMPTY_LINE_SEGMENTS = new PTextSegment[0];

    public PPlainTextStyler(PTextArea textArea) {
        super(textArea);
    }

    public PTextSegment[] getTextSegments(int line) {
        int start = textArea.getLineStartOffset(line);
        int end = textArea.getLineEndOffsetBeforeTerminator(line);
        return new PTextSegment[] {
            new PTextSegment(textArea, start, end, PStyle.NORMAL)
        };
    }

    public Color getColorForStyle(int style) {
        return Color.BLACK;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        // We have no language, so we have no keywords.
    }
}
