import models.iggy.zap.fx.EnvelopeType;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayOutputStream;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

public class FormatTest {

    private String sampleName = "sample.xml";

    private JAXBContext context;

    @Before
    public void setUp() throws Exception {
        context = JAXBContext.newInstance(EnvelopeType.class);
    }

    @Test
    public void testJaxbFindsClasses() throws Exception {
        assertNotNull (context);
    }

    @Test
    public void testNonEmpty() throws Exception {
        EnvelopeType env = context.createUnmarshaller().unmarshal(new StreamSource(getClass().getResourceAsStream(sampleName)), EnvelopeType.class).getValue();
        assertNotNull("Should not be empty", env);

    }

    @Test
    public void testExpectedCurrencies() throws Exception {
        EnvelopeType env = context.createUnmarshaller().unmarshal(new StreamSource(getClass().getResourceAsStream(sampleName)), EnvelopeType.class).getValue();
        assertEquals("Should be number of currencies", 33, env.getCube().getCube().get(0).getCube().size());

    }
}
