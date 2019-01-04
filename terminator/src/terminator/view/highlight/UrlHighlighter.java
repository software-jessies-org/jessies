package terminator.view.highlight;

import e.util.PatternUtilities;
import e.util.Range;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import terminator.Terminator;
import terminator.TerminatorPreferences;
import terminator.model.TerminalModel;
import terminator.view.TerminalView;

/**
 * This works in conjunction with the TerminalView mouse listener that tracks
 * and repaints highlights under the mouse.
 */
public class UrlHighlighter {
    public void addHighlightsFrom(final TerminalView view, final int firstLineIndex) {
        // FIXME: this code is duplicated in FindHighlighter.addHighlightsInternal.
        addMatches(PatternUtilities.HYPERLINK_PATTERN, view, firstLineIndex);
        // If the user has configured a script to handle error links, then include the errors regexp as 'URL's.
        if (Terminator.getPreferences().getString(TerminatorPreferences.ERROR_LINK_CMD) != "") {
            addMatches(PatternUtilities.ERROR_PATTERN, view, firstLineIndex);
        }
    }
    
    private void addMatches(Pattern pattern, final TerminalView view, final int firstLineIndex) {
        final TerminalModel model = view.getModel();
        ArrayList<Range> matches = new ArrayList<>();
        for (int i = model.getLineCount() - 1; i >= firstLineIndex; i--) {
            String text = model.getTextLine(i).getString();
            Matcher matcher = pattern.matcher(text);
            matches.clear();
            while (matcher.find()) {
                matches.add(new Range(matcher.start(), matcher.end()));
            }
            if (!matches.isEmpty()) {
                // FIXME: the toArray is a mistake. We should use List<Range> instead.
                view.setUrlMatches(i, matches.toArray(new Range[matches.size()]));
            }
        }
    }
}
