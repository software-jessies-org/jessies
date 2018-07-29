package terminator.view.highlight;

import e.util.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.ArrayList;
import javax.swing.*;
import terminator.model.*;
import terminator.view.*;

/**
 * Highlights the results of user-initiated finds.
 */
public class FindHighlighter {
    private static final ExecutorService executorService = ThreadUtilities.newSingleThreadExecutor("Background Find");
    
    private Pattern pattern;
    private String regularExpression = "";
    
    public String getName() {
        return "Find Highlighter";
    }
    
    /**
     * Sets the current sought regular expression. Existing highlights will
     * be removed, matches in the current text will be found, and future
     * matches will be found as they appear.
     * 
     * 'newRegularExpression' can be "" to cancel match highlighting.
     * 
     * Status changes will be reported to 'findStatusDisplay' on the EDT.
     */
    public void setPattern(final TerminalView view, String newRegularExpression, final FindStatusDisplay findStatusDisplay) {
        // Don't waste time re-finding all the current matches.
        if (newRegularExpression.equals(regularExpression)) {
            return;
        }
        
        // Cancel the current find.
        forgetPattern(view);
        findStatusDisplay.setStatus("", false);
        
        // Is that all we're here for?
        if (newRegularExpression.length() == 0) {
            return;
        }
        
        // Check that we can actually compile the new regular expression.
        try {
            this.pattern = PatternUtilities.smartCaseCompile(newRegularExpression);
            this.regularExpression = newRegularExpression;
        } catch (PatternSyntaxException ex) {
            findStatusDisplay.setStatus(ex.getDescription(), true);
            return;
        }
        
        executorService.execute(new SwingWorker<Object, Object>() {
            private int matchCount;
            
            @Override
            protected Object doInBackground() {
                matchCount = addHighlightsInternal(view, 0);
                return null;
            }
            
            @Override
            protected void done() {
                findStatusDisplay.setStatus(StringUtilities.pluralize(matchCount, "match", "matches"), false);
            }
        });
    }
    
    public void forgetPattern(TerminalView view) {
        view.removeFindMatches();
        this.pattern = null;
        this.regularExpression = "";
    }
    
    /** Request to add highlights to all lines of the view from the index given onwards. */
    public void addHighlightsFrom(TerminalView view, int firstLineIndex) {
        addHighlightsInternal(view, firstLineIndex);
    }

    /**
     * Returns the number of highlights added.
     */
    private int addHighlightsInternal(TerminalView view, int firstLineIndex) {
        if (pattern == null) {
            return 0;
        }
        view.getBirdView().setValueIsAdjusting(true);
        try {
            // FIXME: this code is duplicated in UrlHighlighter.addHighlightsFrom.
            TerminalModel model = view.getModel();
            int count = 0;
            // FIXME: this strange scoping avoids allocating an ArrayList for lines without matches, but there must be a cleaner way.
            final ArrayList<Range> matches = new ArrayList<Range>();
            for (int i = model.getLineCount() - 1; i >= firstLineIndex; i--) {
                String text = model.getDisplayTextLine(i).getString();
                Matcher matcher = pattern.matcher(text);
                matches.clear();
                while (matcher.find()) {
                    matches.add(new Range(matcher.start(), matcher.end()));
                    ++count;
                }
                if (!matches.isEmpty()) {
                    // FIXME: the toArray is a mistake. We should use List<Range> instead.
                    view.setFindMatches(i, matches.toArray(new Range[matches.size()]));
                }
            }
            return count;
        } finally {
            view.getBirdView().setValueIsAdjusting(false);
        }
    }
}
