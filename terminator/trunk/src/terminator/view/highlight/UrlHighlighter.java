package terminator.view.highlight;

import e.gui.SimpleDialog;
import e.util.BrowserLauncher;
import e.util.PatternUtilities;
import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import terminator.model.Location;
import terminator.model.Style;
import terminator.model.TerminalModel;
import terminator.view.TerminalView;

/**
 * This works in conjunction with the TerminalView mouse listener that tracks
 * and repaints highlights under the mouse.
 */
public class UrlHighlighter implements Highlighter {

    /**
     * A style that changes its mind in a similar way to that in SelectionHighlighter.
     */
    private static class UnderlineWhenMouseOverStyle extends Style {
        
        private final TerminalView view;
        private Highlight highlight;

        public UnderlineWhenMouseOverStyle(TerminalView view) {
            super(null, null, null, null, false);
            this.view = view;
        }

        /**
         * Breaks the unusual highlight-style cycle.
         */
        public void initialize(Highlight highlight) {
            this.highlight = highlight;
        }
        
        @Override
        public boolean hasUnderlined() {
            return isUnderlined();
        }
        
        @Override
        public boolean isUnderlined() {
            return view.isHighlightUnderMouse(highlight);
        }
    }
    
    public void addHighlights(final TerminalView view, final int firstLineIndex) {
        final TerminalModel model = view.getModel();
        for (int i = firstLineIndex; i < model.getLineCount(); i++) {
            final Matcher matcher = PatternUtilities.HYPERLINK_PATTERN.matcher(model.getTextLine(i).getString());
            while (matcher.find()) {
                view.addHighlight(createHighlight(view, i, matcher));
            }
        }
    }

    private Highlight createHighlight(final TerminalView view, final int lineNo, final Matcher match) {
        final Location start = new Location(lineNo, match.start());
        final Location end = new Location(lineNo, match.end());
        final UnderlineWhenMouseOverStyle style = new UnderlineWhenMouseOverStyle(view);
        final Highlight highlight = new Highlight(this, start, end, style);
        style.initialize(highlight);
        highlight.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return highlight;
    }

    public String getName() {
        return "URL highlighter";
    }

    public void highlightClicked(final TerminalView view, final Highlight highlight, final String highlitText, final MouseEvent event) {
        if (event.isControlDown()) {
            try {
                BrowserLauncher.openURL(highlitText);
            } catch (Throwable th) {
                SimpleDialog.showDetails(view, "Problem opening URL", th);
            }
        }
    }
}
