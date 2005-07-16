package e.ptextarea;

import java.util.*;

/**
 * A CharSequence that returns ' ' for uninteresting characters, for the
 * purposes of bracket matching. For example, if you're matching a bracket
 * in something of style NORMAL, you don't want it to match a COMMENT or
 * STRING.
 * 
 * This is a rather inefficient implementation if you're editing a large file
 * because it iterates over all the logical segments, even if it doesn't need
 * to, and it copies the entire text.
 */
public class PSameStyleCharSequence {
    private PSameStyleCharSequence() {
    }
    
    /**
     * Returns a CharSequence that masks out characters not relevant to the
     * character at the given offset.
     */
    public static CharSequence forOffset(PTextArea textArea, int offset) {
        Iterator<PLineSegment> it = textArea.getLogicalSegmentIterator(offset);
        if (it.hasNext()) {
            PLineSegment segment = it.next();
            PStyle style = segment.getStyle();
            if (style == PStyle.STRING || style == PStyle.COMMENT) {
                if (segment.getOffset() == offset) {
                    // If we're at the start of a STRING or COMMENT segment, we
                    // can't possibly match a bracket; our only chance is if the
                    // last character of the previous segment (which would have
                    // to be NORMAL, too) is an opening bracket, so we fall
                    // through and return the NORMAL characters.
                    // So this can match backwards (and only backwards):
                    //   (/*k*/|)
                    // And this can match forwards (and only forwards):
                    //   (|/*k*/)
                } else {
                    // We only want to match within one run of this style.
                    return new SegmentRunCharSequence(textArea, collectRun(segment, it));
                }
            }
        }
        // We want to filter out all the rest.
        return new MangledCharSequence(textArea);
    }
    
    /**
     * Returns a list of segments starting with 'firstSegment' and continuing
     * with as many consecutive elements from 'it' as have the same style. We
     * include newline segments as being unimportant. Note that this code has
     * the perhaps surprising (but not illogical) behavior of considering
     * adjacent lines of a multi-line comment as part of a run, but adjacent
     * comment-to-end-of-line lines as separate single-item runs. (This is
     * because only in the former case does the indentation have the COMMENT
     * style. In the latter case, it has NORMAL style.)
     * 
     * TEST: That means that the parentheses in that paragraph can be matched,
     * but the ones inside the method above can't.
     */
    private static List<PLineSegment> collectRun(PLineSegment firstSegment, Iterator<PLineSegment> it) {
        final PStyle firstStyle = firstSegment.getStyle();
        
        ArrayList<PLineSegment> run = new ArrayList<PLineSegment>();
        run.add(firstSegment);
        
        while (it.hasNext()) {
            PLineSegment segment = it.next();
            PStyle thisStyle = segment.getStyle();
            if (thisStyle == PStyle.NEWLINE) {
                // Don't bother adding it to the run, but consider it the end
                // of the run, either.
            } else if (thisStyle != firstStyle) {
                break;
            } else {
                run.add(segment);
            }
        }
        return run;
    }
    
    /**
     * Masks out the characters in the text area that aren't in a particular
     * line segment. This class isn't thread-safe; PTextBuffer should have a
     * "modCount" like the java.util collections so we can throw a
     * ConcurrentModificationException if we're no longer valid. (Note that
     * because we haven't copied anything from the text buffer, it's not
     * directly modifications to the text buffer we're afraid of; it's their
     * knock-on effect of potentially invalidating the line segment. An
     * alternative solution might be to pull the offset and end out of the
     * segment and store them as anchors?)
     */
    public static class SegmentRunCharSequence implements CharSequence {
        private PTextBuffer textBuffer;
        private List<PLineSegment> run;
        
        public SegmentRunCharSequence(PTextArea textArea, List<PLineSegment> run) {
            this.textBuffer = textArea.getTextBuffer();
            this.run = run;
        }
        
        public char charAt(int index) {
            for (PLineSegment segment : run) {
                if (index >= segment.getOffset() && index < segment.getEnd()) {
                    return textBuffer.charAt(index);
                }
            }
            return ' ';
        }
        
        public int length() {
            return textBuffer.length();
        }
        
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("subSequence(" + start + ", " + end + ")");
        }
    }
    
    /**
     * Masks out any non-NORMAL text in the text area. This class is not
     * thread-safe (or efficient) because it copies the full text of the
     * text area, and does nothing to cope with changes to the text area.
     */
    public static class MangledCharSequence implements CharSequence {
        private StringBuilder mangledText;
        
        // This constructor is *hideously* expensive.  We need to replace this
        // with something a bit more cycle-friendly.
        public MangledCharSequence(PTextArea textArea) {
            this.mangledText = new StringBuilder(textArea.getTextBuffer());
            Iterator<PLineSegment> it = textArea.getLogicalSegmentIterator(0);
            while (it.hasNext()) {
                PLineSegment segment = it.next();
                if (segment.getStyle() != PStyle.NORMAL) {
                    for (int i = segment.getOffset(); i < segment.getEnd(); ++i) {
                        /* TEST: this { should match this } */
                        /* TEST: the for loop's brace shouldn't match this } */
                        /* TEST: this { shouldn't match anything */
                        mangledText.setCharAt(i, ' ');
                    }
                }
            }
        }
        
        public char charAt(int index) {
            return mangledText.charAt(index);
        }
        
        public int length() {
            return mangledText.length();
        }
        
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException("subSequence(" + start + ", " + end + ")");
        }
    }
}
