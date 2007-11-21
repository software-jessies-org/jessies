package e.util;

import java.io.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;

public class XmlUtilities {
    public static Document makeEmptyDocument() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        return builder.newDocument();
    }
    
    public static Document readXmlFromDisk(String filename) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(FileUtilities.fileFromString(filename));
        return document;
    }
    
    public static void writeXmlToDisk(String filename, Document document) throws Exception {
        // Set up a Transformer to produce indented XML output.
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xalan}indent-amount", "4");
        
        // Create the XML content...
        StringWriter content = new StringWriter();
        transformer.transform(new DOMSource(document), new StreamResult(content));
        
        // And then carefully write it to disk.
        File file = FileUtilities.fileFromString(filename);
        if (StringUtilities.writeAtomicallyTo(file, content.toString()) == false) {
            Log.warn("\"" + file + "\" content should have been:\n" + content.toString());
        }
    }
    
    private XmlUtilities() {
    }
}
