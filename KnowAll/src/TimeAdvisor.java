/*
 * Copyright (C) 2006, Elliott Hughes.
 * 
 * This file is part of KnowAll.
 * 
 * KnowAll is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * KnowAll is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with KnowAll; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */

import e.util.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

public class TimeAdvisor implements Advisor {
    private String heading;
    private Pattern pattern;
    private String commandTemplate;
    
    public TimeAdvisor() {
        this.pattern = Pattern.compile("(?i)\\b(\\d+)\\s*(ns|us|ms|s|sec|secs|seconds)\\b");
    }
    
    public void advise(SuggestionsBox suggestionsBox, String text) {
        ArrayList<String> result = new ArrayList<String>();
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            final long count = Long.parseLong(matcher.group(1));
            final TimeUnit unit = parseTimeUnit(matcher.group(2));
            final String originalForm = matcher.group(1) + " " + matcher.group(2);
            final long milliseconds = TimeUnit.MILLISECONDS.convert(count, unit);
            // FIXME: this output only really makes sense for times > 1s.
            result.add(originalForm + " = " + TimeUtilities.durationToIsoString(milliseconds));
        }
        if (result.isEmpty() == false) {
            suggestionsBox.addSuggestion(new Suggestion("Time", StringUtilities.join(result, "<br>")));
        }
    }
    
    private TimeUnit parseTimeUnit(String s) {
        if (s.equalsIgnoreCase("ns")) {
            return TimeUnit.NANOSECONDS;
        } else if (s.equalsIgnoreCase("us")) {
            return TimeUnit.MICROSECONDS;
        } else if (s.equalsIgnoreCase("ms")) {
            return TimeUnit.MILLISECONDS;
        } else {
            return TimeUnit.SECONDS;
        }
    }
}
