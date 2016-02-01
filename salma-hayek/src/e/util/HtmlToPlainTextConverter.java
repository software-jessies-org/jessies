package e.util;

import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.html.parser.*;

/**
 * Converts an HTML string to a corresponding plain text string.
 * The conversion is currently very simple.
 * 
 * Possible enhancements include:
 * 
 * + Use ASCII art to represent HR.
 * + Use ASCII art to represent UL/OL.
 * + Show the HREF text from A tags and SRC text from IMG tags. (Also ALT text, if available.)
 * + Attempt to preserve TABLE structure.
 * + Strip SCRIPT text in the same way we strip STYLE text.
 * 
 * Realistically, though, most HTML mail is spam, most of the rest is no better than its text/plain companion.
 * The few exceptions are solicited commercial email (Amazon or Netflix, say), and they're not generally replied to (which is the primary use of this class).
 * 
 * I've seen roughly this implementation all over the place, but the original may have been by Elliotte Rusty Harold:
 * http://www.cafeaulait.org/slides/sd2000east/webclient/index.html
 */
public class HtmlToPlainTextConverter {
    private String plainText;
    
    public HtmlToPlainTextConverter(String html) {
        try {
            HtmlParserCallback callback = new HtmlParserCallback();
            new ParserDelegator().parse(new StringReader(html), callback, true);
            this.plainText = callback.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            this.plainText = html;
        }
    }
    
    public String getPlainText() {
        return plainText;
    }
    
    public static String convert(String html) {
        return new HtmlToPlainTextConverter(html).getPlainText();
    }
    
    public static void main(String[] args) {
        for (String arg : args) {
            System.err.println(convert(e.util.StringUtilities.readFile(arg)));
        }
    }
    
    private static class HtmlParserCallback extends HTMLEditorKit.ParserCallback {
        private StringBuilder result = new StringBuilder();
        private int styleTagDepth = 0;
        
        public void handleComment(char[] chars, int position) {
        }
        
        public void handleEndTag(HTML.Tag tag, int position) {
            if (tag == HTML.Tag.STYLE) {
                --styleTagDepth;
            }
            
            if (tag.isBlock()) {
                result.append("\n\n");
            } else if (tag.breaksFlow()) {
                result.append("\n");
            }
        }
        
        public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag.isBlock()) {
                result.append("\n\n");
            } else if (tag.breaksFlow()) {
                result.append("\n");
            } else {
                result.append(" ");
            }
        }
        
        public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
            if (tag == HTML.Tag.STYLE) {
                ++styleTagDepth;
            }
        }
        
        public void handleText(char[] chars, int position) {
            if (styleTagDepth == 0) {
                result.append(chars);
            }
        }
        
        public void handleError(String errorMessage, int position) {
            //result.append("[HTML error " + errorMessage + "]");
        }
        
        public String toString() {
            return result.toString();
        }
    }
}
