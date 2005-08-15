package e.ptextarea;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A simple styler for patches.
 */
public class PPatchTextStyler extends PAbstractTextStyler {
    public PPatchTextStyler(PTextArea textArea) {
        super(textArea);
    }
    
    @Override
    public List<PLineSegment> getTextSegments(int line) {
        int start = textArea.getLineStartOffset(line);
        int end = textArea.getLineEndOffsetBeforeTerminator(line);
        List<PLineSegment> result = new ArrayList<PLineSegment>();
        String lineText = textArea.getLineText(line);
        PStyle style = PStyle.NORMAL;
        if (lineText.startsWith("+")) {
            style = PStyle.PATCH_PLUS;
        } else if (lineText.startsWith("-")) {
            style = PStyle.PATCH_MINUS;
        } else if (lineText.startsWith("@")) {
            style = PStyle.PATCH_AT;
        }
        result.add(new PTextSegment(textArea, start, end, style));
        return result;
    }
    
    public void addKeywordsTo(Collection<String> collection) {
        // We have no language, so we have no keywords.
    }
    
    public static class PatchHighlight extends PColoredHighlight {
        public PatchHighlight(PTextArea textArea, int startIndex, int endIndex, Color color) {
            super(textArea, startIndex, endIndex, color);
        }
        
        public String getHighlighterName() {
            return "PatchHighlight";
        }
    }
}
