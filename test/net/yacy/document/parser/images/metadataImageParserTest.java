
package net.yacy.document.parser.images;

import java.io.File;
import java.io.FileInputStream;
import net.yacy.cora.document.id.AnchorURL;
import net.yacy.document.Document;
import org.junit.Test;
import static org.junit.Assert.*;

public class metadataImageParserTest {


    /**
     * Test of parse method, of class metadataImageParser.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("metadataImageParser.parse TIF");

        final String testFiles = "YaCyLogo_120ppi.tif";
        final String mimetype = "image/tiff";
        final String charset = null;

        final String filename = "test/parsertest/" + testFiles;
        final File file = new File(filename);

        final AnchorURL url = new AnchorURL("http://localhost/" + filename);
        System.out.println("parse file: " + filename);

        metadataImageParser p = new metadataImageParser();
        final Document[] docs = p.parse(url, mimetype, charset, null, new FileInputStream(file));

        Document doc = docs[0];
        assertEquals("YaCy Logo",doc.dc_title());
        System.out.println(doc.toString());
    }

}
