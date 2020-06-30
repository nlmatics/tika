package org.apache.tika.extractor;

import com.google.common.io.Files;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class TestMe {
    @Test
    public void testSimple() throws IOException {
        File file = new File("ENTER FILE HERE");
        byte[] bytes = Files.toByteArray(file);
        AutoDetectParser tikaParser = new AutoDetectParser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler handler;
        try {
            handler = factory.newTransformerHandler();
        } catch (TransformerConfigurationException ex) {
            throw new IOException(ex);
        }
        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        handler.setResult(new StreamResult(out));
        ExpandedTitleContentHandler handler1 = new ExpandedTitleContentHandler(handler);
        try {
            tikaParser.parse(new ByteArrayInputStream(bytes), handler1, new Metadata());
        } catch (SAXException | TikaException ex) {
            throw new IOException(ex);
        }
        try {
            FileWriter myWriter = new FileWriter("OUTPUT FILE HERE");
            myWriter.write(out.toString());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        //System.out.println(out.toString());

    }
}
