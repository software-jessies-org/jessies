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

public class IsbnAdvisor implements Advisor {
    /**
     * An ISBN is a 10-digit number (where the last digit may be 'X'), with
     * optional dashes between pretty much any of the digits:
     * http://isbn-international.org/en/userman/chapter4.html
     */
    private static final Pattern PATTERN = Pattern.compile("(\\d-?\\d-?\\d-?\\d-?\\d-?\\d-?\\d-?\\d-?\\d-?[\\dX])");

    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            suggestionsBox.addSuggestion(new Suggestion("ISBN", "<a href=\"http://www.isbn.nu/" + matcher.group(1).replaceAll("-", "") + "\">" + matcher.group(1) + "</a>"));
        }
    }
}
