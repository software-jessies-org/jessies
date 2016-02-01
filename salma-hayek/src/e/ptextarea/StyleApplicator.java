package e.ptextarea;

import java.util.*;

public interface StyleApplicator {
    /**
     * Returns the list of text segments produced by applying a particular
     * style to a given normal segment. This is used, for example, by the
     * keyword styler to return segments of KEYWORD style within runs of
     * NORMAL style.
     */
    public List<PLineSegment> applyStylingTo(String line, PLineSegment normalSegment);
    
    /**
     * Tests whether this style applicator works on the given style.
     * applyStylingTo will only be passed segments of a style for which
     * this method returns true.
     */
    public boolean canApplyStylingTo(PStyle style);
}
