package e.ptextarea;

import java.awt.Color;

/**
 * A trivial styler for plain text, which colors all text black.
 */
public class PPlainTextStyler implements PTextStyler {
    private static final PTextSegment[] EMPTY_LINE_SEGMENTS = new PTextSegment[0];

    public static final PPlainTextStyler INSTANCE = new PPlainTextStyler();

    private PPlainTextStyler() {
        // Only one instance.
    }

    public PTextSegment[] getLineSegments(PTextArea.SplitLine splitLine) {
        if (splitLine.getLength() == 0) {
            return EMPTY_LINE_SEGMENTS;
        }
        return new PTextSegment[] {
            new PTextSegment(0, splitLine.getContents().toString())
        };
    }

    public Color getDefaultColor(int style) {
        return Color.BLACK;
    }
}
