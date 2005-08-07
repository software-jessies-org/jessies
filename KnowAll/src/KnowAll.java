/*
    Copyright (C) 2002-2005, Elliott Hughes.

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

import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.html.*;

public class KnowAll extends JFrame {
    private JTextPane textPane = new JTextPane();
    private JLabel statusBar = new JLabel(" ");

    private ArrayList<Advisor> advisors = new ArrayList<Advisor>();

    private ClipboardMonitor clipboardMonitor;

    public KnowAll() {
        super("KnowAll");
        initAdvisors();
        initTextPane();
        initUi();
        initClipboardMonitor();
    }

    private void initTextPane() {
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.addHyperlinkListener(new HyperlinkListener() {
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    openUrl(e.getURL().toString());
                } else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED) {
                    setStatus(e.getURL().toString());
                } else if (e.getEventType() == HyperlinkEvent.EventType.EXITED) {
                    setStatus(" ");
                }
            }
        });
        HTMLEditorKit editorKit = (HTMLEditorKit) textPane.getEditorKit();
        StyleSheet styleSheet = editorKit.getStyleSheet();
        styleSheet.removeStyle("body");
        styleSheet.addRule("body { font-family: Arial,Helvetica,sans-serif }");
        styleSheet.addRule("body { font-size: 10 }");
    }

    private void initUi() {
        getContentPane().add(new JScrollPane(textPane), BorderLayout.CENTER);
        getContentPane().add(statusBar, BorderLayout.SOUTH);
        setSize(300, 500);
        setVisible(true);
    }

    private void initClipboardMonitor() {
        clipboardMonitor = new ClipboardMonitor(new ClipboardListener() {
            public void clipboardContentsChangedTo(String contents) {
                try {
                    searchFor(contents);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void initAdvisors() {
        advisors.add(new CurrencyAdvisor());
        advisors.add(new IsbnAdvisor());
        advisors.add(new NumberAdvisor());
        advisors.add(new UkPostCodeAdvisor());
    }

    private String pluralize(int i, String singular, String plural) {
        return i + " " + (i == 1 ? singular : plural);
    }

    public void searchFor(final String input) throws UnsupportedEncodingException {
        int lines = 0;
        for (int i = 0; i < input.length(); ++i) {
            if (input.charAt(i) == '\n') ++lines;
        }
        lines = Math.max(1, lines);
        String text = "<html>";
        text += pluralize(input.length(), "character", "characters");
        text += " on " + pluralize(lines, " line", " lines") + ".";
        text += "<br><hr noshade>";
        text += "<tt>";
        text += "<font size=-8>";
        text += input;
        text += "</tt>";
        text += "</pre>";
        text += "<hr noshade>";
        SuggestionsBox suggestionsBox = new SuggestionsBox();
        for (Advisor advisor : advisors) {
            advisor.advise(suggestionsBox, input);
        }
        String lastHeading = "";
        for (int i = 0; i < suggestionsBox.size(); ++i) {
            Suggestion suggestion = suggestionsBox.getSuggestion(i);
            if (lastHeading.equals(suggestion.heading)) {
                text += "<br>" + suggestion.text;
            } else {
                text += titledText(suggestion.heading, suggestion.text);
                lastHeading = suggestion.heading;
            }
        }
        if (input.indexOf('\n') == -1) {
            String urlEncodedInput = URLEncoder.encode(input, "UTF-8");
            text += titledText("Google Search", "<a href=\"http://www.google.com/search?hl=en&ie=UTF-8&q=" + urlEncodedInput + "\">" + input + "</a>");
        }
        textPane.setText(text);
    }

    public static String titledText(String heading, String text) {
        // FIXME: do this with a style sheet.
        return "<h4><u>" + heading + "</u></h4>" + text;
    }

    public void openUrl(final String url) {
        try {
            // FIXME: this only works on Mac OS.
            Runtime.getRuntime().exec("open " + url);
        } catch (Exception ex) {
            ex.printStackTrace(); // FIXME: report in a dialog.
        }
    }

    public String chooseSearchUrl(String s) {
        if (s.startsWith("train")) {
            // FIXME: can we do something better here, filling in some details?
            return "http://212.87.65.227/bin/newquery.exe/en";
        }

        if (s.startsWith("postcode ")) {
            StringBuffer postcode = new StringBuffer();
            for (int i = "postcode ".length(); i < s.length(); i++) {
                char ch = s.charAt(i);
                if (Character.isDigit(ch) || Character.isLetter(ch)) {
                    postcode.append(ch);
                }
            }
            return "http://www.multimap.com/map/browse.cgi?client=public&db=pc&addr1=&client=public&addr2=&advanced=&addr3=&pc=" + postcode.toString().toUpperCase();
        }

        if (s.startsWith("weather ")) {
            s = s.substring("weather ".length());
            return "http://www.bbc.co.uk/cgi-bin/weather/world_location/location.cgi?wlocation=" + s.replace(' ', '+');
        }

        if (s.endsWith(":")) {
            s = s.substring(0, s.length() - 1);
            return "http://dictionary.reference.com/search?q=" + s.replace(' ', '+');
        }

        boolean iFeelLucky = false;
        if (s.endsWith("!")) {
            s = s.substring(0, s.length() - 1);
            iFeelLucky = true;
        }
        String url = "http://www.google.com/search?hl=en&ie=ISO-8859-1&q=";
        url += s.replace(' ', '+');
        url += iFeelLucky ? "&btnI=I'm+Feeling+Lucky" : "&btnG=Google+Search";
        return url;
        // http://www.xe.com/ucc/convert.cgi?Amount=5&From=CHF&To=GBP
    }

    public void setStatus(final String text) {
        if (text.length() > 0) {
            statusBar.setText(text);
        } else {
            // Work around a Swing design flaw.
            statusBar.setText(" ");
        }
    }

    public static void main(String[] args) {
        KnowAll knowAll = new KnowAll();
    }
}
