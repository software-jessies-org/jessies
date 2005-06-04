package e.ptextarea;

import java.util.*;

public interface StyleApplicator {
    /**
     * Returns the list of text segments produced by applying a particular
     * style to a given normal segment. This is used, for example, by the
     * keyword styler to return segments of KEYWORD style within runs of
     * NORMAL style.
     */
    public List<PTextSegment> applyStylingTo(PTextSegment normalSegment);
}
