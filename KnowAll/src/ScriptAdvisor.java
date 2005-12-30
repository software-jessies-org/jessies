/*
 * Copyright (C) 2005, Elliott Hughes.
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
import java.util.regex.*;

/**
 * Runs a command whenever the selection matches a given regular expression.
 * 
 * The commandTemplate is rewritten such that $1 (and so on) are replaced
 * by the value of the corresponding capturing group.
 * 
 * The results are presented under the given heading.
 */
public class ScriptAdvisor implements Advisor {
    private String heading;
    private Pattern pattern;
    private String commandTemplate;
    
    public ScriptAdvisor(String heading, String regularExpression, String commandTemplate) {
        this.heading = heading;
        this.pattern = Pattern.compile(regularExpression);
        this.commandTemplate = commandTemplate;
    }
    
    public void advise(SuggestionsBox suggestionsBox, String text) {
        final Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String command = new Rewriter("(\\$(\\d+))") {
                public String replacement() {
                    int group = Integer.parseInt(group(2));
                    return matcher.group(group);
                }
            }.rewrite(commandTemplate);
            
            ArrayList<String> result = new ArrayList<String>();
            ProcessUtilities.backQuote(null, command.split(" "), result, result);
            
            suggestionsBox.addSuggestion(new Suggestion(heading, StringUtilities.join(result, "<br>")));
        }
    }
}
