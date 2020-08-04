/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.pdf;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.apache.pdfbox.util.Matrix;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class that overrides the {@link PDFTextStripper} functionality
 * to produce a semi-structured XHTML SAX events instead of a plain text
 * stream.
 */
class PDF2XHTML extends AbstractPDF2XHTML {


    /**
     * This keeps track of the pdf object ids for inline
     * images that have been processed.
     * If {@link PDFParserConfig#getExtractUniqueInlineImagesOnly()
     * is true, this will be checked before extracting an embedded image.
     * The integer keeps track of the inlineImageCounter for that image.
     * This integer is used to identify images in the markup.
     * <p>
     * This is used across the document.  To avoid infinite recursion
     * TIKA-1742, we're limiting the export to one image per page.
     */
    private Map<COSStream, Integer> processedInlineImages = new HashMap<>();
    private AtomicInteger inlineImageCounter = new AtomicInteger(0);

    PDF2XHTML(PDDocument document, ContentHandler handler, ParseContext context, Metadata metadata,
              PDFParserConfig config)
            throws IOException {
        super(document, handler, context, metadata, config);
    }

    /**
     * Converts the given PDF document (and related metadata) to a stream
     * of XHTML SAX events sent to the given content handler.
     *
     * @param document PDF document
     * @param handler  SAX content handler
     * @param metadata PDF metadata
     * @throws SAXException  if the content handler fails to process SAX events
     * @throws TikaException if there was an exception outside of per page processing
     */
    public static void process(
            PDDocument document, ContentHandler handler, ParseContext context, Metadata metadata,
            PDFParserConfig config)
            throws SAXException, TikaException {
        PDF2XHTML pdf2XHTML = null;
        try {
            // Extract text using a dummy Writer as we override the
            // key methods to output to the given content
            // handler.
            if (config.getDetectAngles()) {
                pdf2XHTML = new AngleDetectingPDF2XHTML(document, handler, context, metadata, config);
            } else {
                pdf2XHTML = new PDF2XHTML(document, handler, context, metadata, config);
            }
            config.configure(pdf2XHTML);
            // pdf2XHTML.setSortByPosition(true);
            pdf2XHTML.writeText(document, new Writer() {
                @Override
                public void write(char[] cbuf, int off, int len) {
                }

                @Override
                public void flush() {
                }

                @Override
                public void close() {
                }
            });
        } catch (IOException e) {
            if (e.getCause() instanceof SAXException) {
                throw (SAXException) e.getCause();
            } else {
                throw new TikaException("Unable to extract PDF content", e);
            }
        }
        if (pdf2XHTML.exceptions.size() > 0) {
            //throw the first
            throw new TikaException("Unable to extract PDF content", pdf2XHTML.exceptions.get(0));
        }
    }

    @Override
    public void processPage(PDPage page) throws IOException {
        try {
            super.processPage(page);
        } catch (IOException e) {
            handleCatchableIOE(e);
            endPage(page);
        }
    }

    @Override
    protected void endPage(PDPage page) throws IOException {
        try {
            //writeParagraphEnd();
            try {
                extractImages(page);
            } catch (IOException e) {
                handleCatchableIOE(e);
            }
            super.endPage(page);
        } catch (SAXException e) {
            throw new IOException("Unable to end a page", e);
        } catch (IOException e) {
            handleCatchableIOE(e);
        }
    }

    void extractImages(PDPage page) throws SAXException, IOException {
        if (config.getExtractInlineImages() == false
                && config.getExtractInlineImageMetadataOnly() == false) {
            return;
        }

        ImageGraphicsEngine engine = new ImageGraphicsEngine(page, embeddedDocumentExtractor,
                config, processedInlineImages, inlineImageCounter, xhtml, metadata, context);
        engine.run();
        List<IOException> engineExceptions = engine.getExceptions();
        if (engineExceptions.size() > 0) {
            IOException first = engineExceptions.remove(0);
            if (config.getCatchIntermediateIOExceptions()) {
                exceptions.addAll(engineExceptions);
            }
            throw first;
        }
    }

    /*
        @Override
        protected void writeParagraphStart() throws IOException {
            super.writeParagraphStart();
            try {
                System.out.println();
                xhtml.startElement("div", "style", "position:relative;");
            } catch (SAXException e) {
                throw new IOException("Unable to start a paragraph", e);
            }
        }
    */
    @Override
    protected void writeParagraphStart() throws IOException {
        super.writeParagraphStart();
        System.out.println();
    }

    /*
        @Override
        protected void writeParagraphEnd() throws IOException {
            super.writeParagraphEnd();
            try {
                xhtml.endElement("div");
            } catch (SAXException e) {
                throw new IOException("Unable to end a paragraph", e);
            }
        }

     */
    @Override
    protected void writeParagraphEnd() throws IOException {
        super.writeParagraphEnd();
        System.out.println();
    }

    /*
        @Override
        protected void writeString(String text) throws IOException {
            try {
                //text = text + "tika-hack";
                text = text + "<embed charX>";
                xhtml.characters(text);
            } catch (SAXException e) {
                throw new IOException(
                        "Unable to write a string: " + text, e);
            }
        }

 */

    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        try {
            StringBuilder te = new StringBuilder("newline: ");
            StringBuilder te1 = new StringBuilder();
            StringBuilder testWordStartPos = new StringBuilder();
            ArrayList<String> wordsStartPos = new ArrayList<>(20);
            ArrayList<String> wordsEndPos = new ArrayList<>(20);
            ArrayList<String> wordSpaceDistanceList = new ArrayList<>(20);
            // int wordCount = text.split("\\s+").length;
            // String[] wordsStartPos2 = new String[wordCount];

            float linePositiony = 0;
            float linePositionx = 0;
            String last_char_pos = "[]";
            String last_char_pos_tmp = "";
            te1.append("[");
            String height = "8";
            String y_rel = "";
            String y_bot = "";
            String font_type = "";
            String font_weight = "normal";
            String font_style = "normal";
            String word_start_pos = "";
            //xhtml.startElement("div", "style", "border:3px solid ##ff0000;");
            String s1 = Float.toString(textPositions.get(0).getXDirAdj() * 1);
            String indent = "text-indent:" + s1 + "px;";
            String prev = " ";
            String height1 = "start-font-size:" + Float.toString((float) Math.pow(textPositions.get(0).getHeightDir(), 1)) + "px;";
            String top1 = "top1:" + Float.toString(textPositions.get(0).getYDirAdj()) + "px;";
            //for (TextPosition s : textPositions) {
            for (int i = 0; i < textPositions.size(); i ++) {
                //System.out.println(text.getYDirAdj());
                //xhtml.startElement();
                //xhtml.endElement();
                //Math.pow(s.getHeightDir(),5);
                TextPosition s = textPositions.get(i);
                //System.out.println(textPositions.getClass());
                height = "font-size:" + Float.toString((float) Math.pow(s.getHeightDir(), 1)) + "px;";
                y_rel = "top:" + Float.toString(s.getYDirAdj()) + "px;";
                te1.append("{'char':(").append(s.getWidthDirAdj()).append(", ").append(s.getHeightDir()).append(" ),").append("'line':(").append(s.getXDirAdj()).append(", ").append(s.getYDirAdj()).append(")},");
                linePositiony = s.getYDirAdj();
                linePositionx = s.getXDirAdj();
                // get start of word in format (xCoord, yCoord)
                if (prev.equals(" ")) {
                    String tempWordPos = "(" + linePositionx + "," + linePositiony + ")";
                    testWordStartPos.append("(").append(linePositionx).append(",").append(linePositiony).append(")").append("current char: ").append(s.toString());
                    wordsStartPos.add(tempWordPos);
                }
                if (i+1 < textPositions.size()) {
                    if (textPositions.get(i+1).toString().equals(" ")) {
                        // if next char is a space save get the position of the last char of the word
                        wordsEndPos.add("(" + linePositionx + ", " + linePositiony + ")");
                    }
                }
                prev = s.toString();
                PDFontDescriptor fd = s.getFont().getFontDescriptor();
                font_type = fd.getFontFamily();
                if (font_type == null) {
                    font_type = fd.getFontName();

                    if (font_type.contains("+")) {
                        font_type = font_type.split("\\+")[1];
                    }

                    if (font_type.contains(",")) {
                        String[] arr = font_type.split(",");
                        if (arr[1].toLowerCase().contains("bold")) {
                            font_weight = "bold";
                        }
                        font_type = arr[0];
                    }
                }
                float fw = fd.getFontWeight();
                if (font_weight.equals("normal") && fw >= 100) {
                    font_weight = Float.toString(fw);
                }
                if (fd.getItalicAngle() != 0) {
                    font_style = "italic";
                }
                last_char_pos = "(" + Float.toString(linePositionx) + ", " + Float.toString(linePositiony) + ")";
                te.append(s);
            }
            font_weight = "font-weight:" + font_weight + ";";
            font_style = "font-style:" + font_style + ";";
            font_type = "font-family:" + font_type + ";";
            word_start_pos = "word-start-positions:" + wordsStartPos.toString();
            String word_end_pos = ";word-end-positions:" + wordsEndPos.toString();
            // System.out.println(testWordStartPos.toString());
            //text = text + "tika-hack";
            //text = text + te1 + "]";
            //String val = height + "border: 3px solid #f3AD21;"+y_rel;
            String val = top1+ height1 + height + font_type + font_style + font_weight + y_rel + "position:absolute;" +
                    indent + word_start_pos + ";last-char:" + last_char_pos + word_end_pos;
            //String val = height + y_rel  + indent;
            xhtml.startElement("p", "style", val);
            xhtml.characters(text);
            xhtml.endElement("p");
            //xhtml.endElement("div");
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to write a string: " + text, e);
        }
    }

    /*
        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            // String t = "newline: " + s;
            //System.out.println(textPositions);
            StringBuilder te = new StringBuilder("newline: ");
            StringBuilder te1 = new StringBuilder("");
            float linePositiony = 0;
            float linePositionx = 0;
            te1.append("[");
            for (TextPosition s : textPositions) {
                //System.out.println(text.getYDirAdj());
                te1.append("{'char':(").append(s.getWidthDirAdj()).append(", ").append(s.getHeightDir()).append(" ),").append("'line':(").append(s.getXDirAdj()).append(", ").append(s.getYDirAdj()).append(")},");
                linePositiony = s.getYDirAdj();
                linePositionx = s.getXDirAdj();
                te.append(text);
            }
            System.out.println(te1);
            te1.append("] " + ", {'linepos': (").append(linePositionx).append(", ").append(linePositiony).append(")} ").append(te).append("\n");

        }
    */
    @Override
    protected void writeCharacters(TextPosition text) throws IOException {
        try {
            xhtml.characters(text.getUnicode());
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to write a character: " + text.getUnicode(), e);
        }
    }

    @Override
    protected void writeWordSeparator() throws IOException {
        try {
            xhtml.characters(getWordSeparator());
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to write a space character", e);
        }
    }

    @Override
    protected void writeLineSeparator() throws IOException {
        try {
            xhtml.newline();
        } catch (SAXException e) {
            throw new IOException(
                    "Unable to write a newline character", e);
        }
    }

    class AngleCollector extends PDFTextStripper {
        Set<Integer> angles = new HashSet<>();

        public Set<Integer> getAngles() {
            return angles;
        }

        /**
         * Instantiate a new PDFTextStripper object.
         *
         * @throws IOException If there is an error loading the properties.
         */
        AngleCollector() throws IOException {
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            Matrix m = text.getTextMatrix();
            m.concatenate(text.getFont().getFontMatrix());
            int angle = (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
            angle = (angle + 360) % 360;
            angles.add(angle);
        }
    }

    private static class AngleDetectingPDF2XHTML extends PDF2XHTML {

        private AngleDetectingPDF2XHTML(PDDocument document, ContentHandler handler, ParseContext context, Metadata metadata, PDFParserConfig config) throws IOException {
            super(document, handler, context, metadata, config);
        }

        @Override
        protected void startPage(PDPage page) throws IOException {
            //no-op
        }

        @Override
        protected void endPage(PDPage page) throws IOException {
            //no-op
        }

        @Override
        public void processPage(PDPage page) throws IOException {
            try {
                super.startPage(page);
                detectAnglesAndProcessPage(page);
            } catch (IOException e) {
                handleCatchableIOE(e);
            } finally {
                super.endPage(page);
            }
        }

        private void detectAnglesAndProcessPage(PDPage page) throws IOException {
            //copied and pasted from https://issues.apache.org/jira/secure/attachment/12947452/ExtractAngledText.java
            //PDFBOX-4371
            AngleCollector angleCollector = new AngleCollector(); // alternatively, reset angles
            angleCollector.setStartPage(getCurrentPageNo());
            angleCollector.setEndPage(getCurrentPageNo());
            angleCollector.getText(document);

            int rotation = page.getRotation();
            page.setRotation(0);

            for (Integer angle : angleCollector.getAngles()) {
                if (angle == 0) {
                    try {
                        super.processPage(page);
                    } catch (IOException e) {
                        handleCatchableIOE(e);
                    }
                } else {
                    // prepend a transformation
                    try (PDPageContentStream cs = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.PREPEND, false)) {
                        cs.transform(Matrix.getRotateInstance(-Math.toRadians(angle), 0, 0));
                    }

                    try {
                        super.processPage(page);
                    } catch (IOException e) {
                        handleCatchableIOE(e);
                    }

                    // remove transformation
                    COSArray contents = (COSArray) page.getCOSObject().getItem(COSName.CONTENTS);
                    contents.remove(0);
                }
            }
            page.setRotation(rotation);
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            Matrix m = text.getTextMatrix();
            m.concatenate(text.getFont().getFontMatrix());
            int angle = (int) Math.round(Math.toDegrees(Math.atan2(m.getShearY(), m.getScaleY())));
            if (angle == 0) {
                super.processTextPosition(text);
            }
        }
    }
}

