package terminator.view.highlight;

import e.gui.SimpleDialog;
import e.util.BrowserLauncher;
import e.util.PatternUtilities;
import e.util.Range;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.ArrayList;
import terminator.model.Location;
import terminator.model.Style;
import terminator.model.TerminalModel;
import terminator.view.TerminalView;

/**
 * This works in conjunction with the TerminalView mouse listener that tracks
 * and repaints highlights under the mouse.
 */
public class UrlHighlighter {
    public void addHighlightsFrom(final TerminalView view, final int firstLineIndex) {
        // FIXME: this code is duplicated in FindHighlighter.addHighlightsInternal.
        final TerminalModel model = view.getModel();
        ArrayList<Range> matches = new ArrayList<Range>();
        for (int i = model.getLineCount() - 1; i >= firstLineIndex; i--) {
            String text = model.getTextLine(i).getString();
            Matcher matcher = PatternUtilities.HYPERLINK_PATTERN.matcher(text);
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
