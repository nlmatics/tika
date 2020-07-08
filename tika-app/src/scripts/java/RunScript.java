public class TestMe {
    public static void main(String[] args) {
        //File file = new File("/Users/reshavabraham/work/data/morgan_stanley_data/res23.pdf");
        // File file = new File("/Users/reshavabraham/work/data/test.pdf");
        // File file = new File("/Users/reshavabraham/scp/scp2/125 Greenwich_Deal Intro Update - Financing.pdf");
        // File file = new File("/Users/reshavabraham/scp/scp2/111_Leroy_OM_FINAL OM.pdf");
        File file = new File("/Users/reshavabraham/scp/scp2/11 Jane Street_Final OM_Lo Res_Senior.pdf");
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
}