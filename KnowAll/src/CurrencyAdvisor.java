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

public class CurrencyAdvisor implements Advisor {
    /**
     * Hard Problem. Currently we only look for dollars. I'd at least like
     * to support AUD, CAD, CHF, EUR, HKD, JPY, NZD, USD and ZAR. Probably
     * some tourist currencies too.
     */
    private static final Pattern PATTERN = Pattern.compile("\\$([\\d.]+)");

    /**
     * FIXME: ideally, KnowAll would regularly download a list of conversion
     * rates, and do the conversions itself.
     * Maybe from here: http://www.oanda.com/convert/fxdaily?value=1&date_fmt=us&redirected=1&lang=en&exch=GBP&exch2=&expr2=&format=CSV&dest=Get+Table&sel_list=AUD_CAD_CHF_EUR_HKD_JPY_NZD_USD_ZAR
     *
     * It would also be particularly useful if the user had some say over
     * the currency to be converted into. I can see two obvious possibilities:
     * either the user specifies their local currency, and everything gets
     * converted into that, or the user specifies a set of interesting
     * currencies, and we use those (like the interesting bases in the
     * NumberAdvisor that gives whatever you don't already have out of
     * hex, decimal and binary).
     */
    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            suggestionsBox.addSuggestion(new Suggestion("Currency", "<a href=\"http://www.xe.com/ucc/convert.cgi?Amount=" + matcher.group(1) + "&From=USD&To=GBP\">$" + matcher.group(1) + " in pounds</a>"));
        }
    }
}
