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

public class NumberAdvisor implements Advisor {
    /**
     * Hexadecimal numbers are introduced with 0x; let's ignore the kind of
     * people who use implicit hex so's not to encourage them. We might want
     * to support #c8c8c8 and the like, though arguably that should be
     * interpreted as a color triple rather than a number.
     *
     * Should we bother with octal? When was the last time you saw any?
     * I'm more likely to see decimal or implicit hex with leading zeroes than
     * octal.
     */
    private static final Pattern PATTERN = Pattern.compile("(?i)(0x)?([0-9a-f]+)");

    /**
     * The bases we're interested in. Decimal and hexadecimal numbers found
     * in the input will be shown in each of these bases (plus ASCII).
     */
    private static final int[] BASES = { 16, 10, 2 };

    public void advise(SuggestionsBox suggestionsBox, String text) {
        Matcher matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            int radix = matcher.group(1) != null ? 16 : 10;
            String number = matcher.group(2);
            if (radix == 10 && containsAnyOf(number, "abcdefABCDEF")) {
                continue;
            }
            long value = Long.parseLong(number, radix);
            if (value < 10) {
                continue;
            }
            String lhs = radixToPrefix(radix) + number;
            String suggestion = "<tt>" + lhs + " = ";
            for (int i = 0; i < BASES.length; ++i) {
                if (BASES[i] != radix) {
                    suggestion += " " + radixToPrefix(BASES[i]) + Long.toString(value, BASES[i]);
                }
            }
            suggestion += " " + toAscii(value) + "</tt>";
            suggestionsBox.addSuggestion(new Suggestion("Number", suggestion));
        }
    }

    private boolean containsAnyOf(String s, String characters) {
        for (int i = 0; i < s.length(); ++i) {
            if (characters.indexOf(s.charAt(i)) != -1) {
                return true;
            }
        }
        return false;
    }

    public String toAscii(long number) {
        StringBuffer result = new StringBuffer("\"</tt>");
        int byteCount = ((number >> 32) != 0) ? 8 : 4;
        for (int i = 0; i < byteCount; i++) {
            char c = (char) (number & 0xff);
            if (c < ' ' || c >= 127) {
                char replacementChar = (c < ' ') ? (char)(c + '@') : '.';
                result.insert(0, "<font color=\"red\">" + replacementChar + "</font>");
            } else {
                result.insert(0, c);
            }
            number >>= 8;
        }
        result.insert(0, " <tt>\"");
        return result.toString();
    }

    public String radixToPrefix(int radix) {
        switch (radix) {
            case 8: return "0";
            case 16: return "0x";
            default:
                return "";
        }
    }
}
