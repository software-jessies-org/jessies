package e.edit;

import e.ptextarea.*;
import java.io.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class HtmlTagger {
    private static final EditorKit KIT = new HTMLEditorKit();
    
    private final PTextArea textArea;
    private final TagReader.TagListener listener;
    
    public HtmlTagger(PTextArea textArea, TagReader.TagListener listener) {
        this.textArea = textArea;
        this.listener = listener;
    }
    
    public String scan() {
        final StringBuilder digest = new StringBuilder();
        
        HTMLDocument document = new HTMLDocument() {
            public HTMLEditorKit.ParserCallback getReader(int pos) {
                return new HTMLEditorKit.ParserCallback() {
                    private StringBuilder builder;
                    private int startOffset;
                    
                    public void handleStartTag(HTML.Tag t, MutableAttributeSet attributes, int offset) {
                        if (isHeadingOrTitleTag(t)) {
                            builder = new StringBuilder();
                            startOffset = offset;
                        } else if (t == HTML.Tag.A) {
                            // FIXME: generate tags for anchors (<a name='bark'>) too?
                            //final String anchorName = (String) attributes.getAttribute(HTML.Attribute.NAME);
                        }
                    }
                    
                    public void handleEndTag(HTML.Tag t, int offset) {
                        if (isHeadingOrTitleTag(t)) {
                            final String name = builder.toString();
                            final int lineNumber = textArea.getLineOfOffset(startOffset) + 1;
                            final char type = (t == HTML.Tag.TITLE) ? 'T' : 'H'; // Title or Heading?
                            // FIXME: we need to stack Tag, String pairs. when we hit an Hn tag, we pop until we hit a Hm (m < n) tag, join the fqn, and push the new pair.
                            final String fullyQualifiedName = "";
                            listener.tagFound(new TagReader.Tag(name, lineNumber, type, "", fullyQualifiedName));
                            
                            // FIXME: we should do more intelligent digesting.
                            digest.append(name);
                            digest.append(lineNumber);
                            digest.append(type);
                            
                            builder = null;
                        }
                    }
                    
                    public void handleText(char[] chars, int offset) {
                        if (builder != null) {
                            builder.append(chars);
                        }
                    }
                    
                    public boolean isHeadingOrTitleTag(HTML.Tag t) {
                        return (t == HTML.Tag.TITLE || t == HTML.Tag.H1 || t == HTML.Tag.H2 || t == HTML.Tag.H3 || t == HTML.Tag.H4 || t == HTML.Tag.H5 || t == HTML.Tag.H6);
                    }
                };
            }
        };
        
        // FIXME: write CharSequenceReader? file a Sun bug?
        final CharSequence buffer = textArea.getTextBuffer();
        final Reader reader = new StringReader(buffer.toString());
        try {
            KIT.read(reader, document, 0);
        } catch (Exception ex) {
            listener.taggingFailed(ex);
        }
        
        return digest.toString();
    }
}
