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

public class PackageTrackingAdvisor implements Advisor {
    private static final Pattern UPS_PATTERN = Pattern.compile("\\b(1Z\\d{4}W\\d{11})\\b");
    private static final Pattern FED_EX_PATTERN = Pattern.compile("\\b((DT)?\\d{12})\\b");
    
    // FIXME: the user's likely to want us to offer to continually track the package until it arrives.
    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher upsMatcher = UPS_PATTERN.matcher(text);
        while (upsMatcher.find()) {
            suggestionsBox.addSuggestion(new Suggestion("Track Package", "<a href=\"http://wwwapps.ups.com/WebTracking/processInputRequest?sort_by=status&tracknums_displayed=1&TypeOfInquiryNumber=T&loc=en_US&InquiryNumber1=" + upsMatcher.group(1) + "&track.x=0&track.y=0\">Track UPS Package</a>"));
        }
        Matcher fedExMatcher = FED_EX_PATTERN.matcher(text);
        while (fedExMatcher.find()) {
            suggestionsBox.addSuggestion(new Suggestion("Track Package", "<a href=\"http://www.fedex.com/Tracking?language=english&cntry_code=us&tracknumbers=" + fedExMatcher.group(1) + "\">Track FedEx Package</a>"));
        }
    }
}
