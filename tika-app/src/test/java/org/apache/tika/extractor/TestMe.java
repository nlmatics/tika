package org.apache.tika.extractor;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.ExpandedTitleContentHandler;
import org.junit.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;


//import com.google.common.io.Files;


public class TestMe {
    @Test
    public void testSimple() throws IOException {
        //File file = new File("/Users/reshavabraham/work/data/morgan_stanley_data/res23.pdf");
        // File file = new File("/Users/reshavabraham/work/data/test.pdf");
        // File file = new File("/Users/reshavabraham/scp/scp2/125 Greenwich_Deal Intro Update - Financing.pdf");
        // File file = new File("/Users/reshavabraham/scp/scp2/111_Leroy_OM_FINAL OM.pdf");
        // File file = new File("/Users/reshavabraham/scp/scp2/11 Jane Street_Final OM_Lo Res_Senior.pdf");

        //File file = new File("/Users/reshavabraham/scp/original-docs/scp2/111 Washington Executive Summary.v.6.1.18.pdf");
        //File file = new File("/Users/reshavabraham/scp/original-docs/scp4/The Godfrey Hotel Phoenix - Oxford Capital Group +True North - Debt OM.pdf");
        // File file = new File("/Users/batyastein/NLMatics/tika_project/pdf_inputs/e-elt_executivesummary.pdf");
        Path file = new File("/Users/batyastein/NLMatics/tika_project/pdf_inputs/e-elt_executivesummary.pdf").toPath();

        byte[] bytes = Files.readAllBytes(file);
        AutoDetectParser tikaParser = new AutoDetectParser();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SAXTransformerFactory factory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler handler;
        PDFParserConfig pdfConfig = new PDFParserConfig();
        pdfConfig.setExtractInlineImages(true);
        pdfConfig.setExtractUniqueInlineImagesOnly(true);
        ParseContext context = new ParseContext();
        try {
            handler = factory.newTransformerHandler();
        } catch (TransformerConfigurationException ex) {
            throw new IOException(ex);
        }
        EmbeddedDocumentExtractor embeddedDocumentExtractor =
                new EmbeddedDocumentExtractor() {
                    @Override
                    public boolean shouldParseEmbedded(Metadata metadata) {
                        return true;
                    }

                    @Override
                    public void parseEmbedded(InputStream stream, ContentHandler handler, Metadata metadata, boolean outputHtml)
                            throws SAXException, IOException {
                        String path = "/Users/batyastein/NLMatics/tika_project/";
                        Path outputDir = new File(path + "_").toPath();
                        Files.createDirectories(outputDir);

                        Path outputPath = new File(outputDir.toString() + "/" + metadata.get(TikaCoreProperties.RESOURCE_NAME_KEY)).toPath();
                        Files.deleteIfExists(outputPath);
                        Files.copy(stream, outputPath);
                    }
                };
        context.set(EmbeddedDocumentExtractor.class, embeddedDocumentExtractor);
        context.set(AutoDetectParser.class, tikaParser);
        context.set(PDFParserConfig.class, pdfConfig);

        handler.getTransformer().setOutputProperty(OutputKeys.METHOD, "html");
        handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
        handler.getTransformer().setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        handler.setResult(new StreamResult(out));
        ExpandedTitleContentHandler handler1 = new ExpandedTitleContentHandler(handler);
        try {
            tikaParser.parse(new ByteArrayInputStream(bytes), handler1, new Metadata(), context);
        } catch (SAXException | TikaException ex) {
            throw new IOException(ex);
        }
        try {
            FileWriter myWriter = new FileWriter("/Users/batyastein/NLMatics/tika_project/textextractNOimgs.html");
            myWriter.write(out.toString());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        System.out.println(out.toString());

    }

    @Test
    public void getTikaDoc() throws IOException {
        //File file = new File("/Users/reshavabraham/work/data/morgan_stanley_data/res23.pdf");
        // File file = new File("/Users/reshavabraham/work/data/test.pdf");
        Path file = new File("/Users/reshavabraham/scp/original-docs/scp2/125 Greenwich_Deal Intro Update - Financing.pdf").toPath();
        //File file = new File("/Users/reshavabraham/scp/scp2/111_Leroy_OM_FINAL OM.pdf");
        //File file = new File(pathname);
        byte[] bytes = Files.readAllBytes(file);
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
            FileWriter myWriter = new FileWriter("/Users/reshavabraham/work/nlm-tika/out.html");
            myWriter.write(out.toString());
            myWriter.close();
            System.out.println("Successfully wrote to the file.");
        } catch (IOException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
        System.out.println(out.toString());

    }

    @Test
    public void indexDocs() throws IOException {
        File folder = new File("/Users/reshavabraham/scp/original-docs/scp4/");
        File[] listOfFiles = folder.listFiles();
        String currFile = "";
        for (File listOfFile : listOfFiles) {
            if (listOfFile.isFile() && !listOfFile.getName().equals(".DS_Store")) {
                currFile = "/Users/reshavabraham/scp/original-docs/scp4/" + listOfFile.getName();
                Path file = new File(currFile).toPath();
                byte[] bytes = Files.readAllBytes(file);
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
                    FileWriter myWriter = new FileWriter("/Users/reshavabraham/scp/tika-modded-067/scp4/" + listOfFile.getName() + ".html");
                    myWriter.write(out.toString());
                    myWriter.close();
                    System.out.println("Successfully wrote to the file.");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                System.out.println(out.toString());

            } else if (listOfFile.isDirectory()) {
                System.out.println("Directory " + listOfFile.getName());
            }
        }
    }
}
