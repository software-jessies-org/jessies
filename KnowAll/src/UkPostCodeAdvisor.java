/*
    Copyright (C) 2004, Elliott Hughes.

    This file is part of KnowAll.

    KnowAll is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    KnowAll is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KnowAll; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

 */

import java.util.regex.*;

public class UkPostCodeAdvisor implements Advisor {
    /**
     * The official definition is here, but we use a simplification:
     * http://www.govtalk.gov.uk/gdsc/html/noframes/PostCode-2-1-Release.htm
     */
    private static final Pattern PATTERN = Pattern.compile("([A-Z]{1,2}[0-9]{1,2}[A-Z]? *[0-9][A-Z]{2})");

    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            suggestionsBox.addSuggestion(new Suggestion("UK Post Code", "<a href=\"http://www.multimap.com/map/browse.cgi?client=public&db=pc&addr1=&client=public&addr2=&advanced=&addr3=&pc=" + matcher.group(1).replaceAll(" ", "") + "&quicksearch=&cidr_client=none\">" + matcher.group(1) + "</a>"));
        }
    }
}
