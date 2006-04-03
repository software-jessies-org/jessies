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

import java.util.regex.*;

// FIXME: regular expressions are pretty unconvincing in this application.
public class ImperialAdvisor implements Advisor {
    // 12 lb, 1,234 lbs, 1000lb bomb.
    // Weight: 2.9 pounds (Intel Mac mini).
    private static final Pattern LBS_PATTERN = Pattern.compile("\\b([\\d.,]+)[\\s-]*(lb|lbs|pound|pounds)\\b");
    
    // Carrie Fisher and Kylie Minogue are both 5'1".
    // Buckminster Fuller was 5 feet 2 inches tall.
    // The Harrier's wingspan is 30ft 4in.
    // 4 feet.
    // 3 inches.
    // 23-inch Apple Cinema HD Display.
    // FIXME: should cope with areas such as: Footprint 14.1 x 9.6 inches
    private static final Pattern IN_PATTERN = Pattern.compile("\\b(([\\d.,]+)[\\s-]*(?:ft|feet|foot|')\\s*)?(([\\d.,]+)[\\s-]*(?:inches|inch|in|\"))?");
    
    // 61°F Overcast.
    private static final Pattern F_PATTERN = Pattern.compile("\\b([\\d.]+)\\s?\u00b0?F\\b");
    
    // FIXME: should know to use subunits (100 g, say, or 2.3 cm).
    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher matcher = LBS_PATTERN.matcher(text);
        while (matcher.find()) {
            double lbs = Double.parseDouble(matcher.group(1).replaceAll(",", ""));
            double kg = 0.45359237 * lbs;
            suggestionsBox.addSuggestion(new Suggestion("Weight", matcher.group() + " = " + String.format("%.2f", kg) + " kg"));
        }
        matcher = IN_PATTERN.matcher(text);
        while (matcher.find()) {
            String feet = matcher.group(1);
            String inches = matcher.group(3);
            if (feet == null && inches == null) {
                continue;
            }
            double ft = (feet != null) ? Double.parseDouble(matcher.group(2).replaceAll(",", "")) : 0.0;
            double in = 12.0 * ft;
            in += (inches != null) ? Double.parseDouble(matcher.group(4).replaceAll(",", "")) : 0;
            double m = 0.0254 * in;
            suggestionsBox.addSuggestion(new Suggestion("Length", matcher.group() + " = " + String.format("%.4f", m) + " m"));
        }
        matcher = F_PATTERN.matcher(text);
        while (matcher.find()) {
            double f = Double.parseDouble(matcher.group(1));
            double c = (f - 32.0) * (5.0 / 9.0);
            suggestionsBox.addSuggestion(new Suggestion("Temperature", matcher.group() + " = " + String.format("%.1f", c) + " \u00b0C"));
        }
    }
}
