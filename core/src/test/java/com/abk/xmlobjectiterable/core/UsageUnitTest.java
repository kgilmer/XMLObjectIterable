package com.abk.xmlobjectiterable.core;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Usage unit tests
 */
public class UsageUnitTest {

    private static final String SAMPLE_XML = "<n1>\n" +
            "    <l2>\n" +
            "        <i1 attrib=\"3\">text1</i1>\n" +
            "        <i1 attrib=\"6\">text2</i1>\n" +
            "        <i1 attrib=\"9\">text3</i1>\n" +
            "    </l2>\n" +
            "</n1>";
    private XmlPullParser parser;

    static class Sample {
        private final String text;

        public Sample(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    static class SampleTransformer implements XMLObjectIterable.Transformer<Sample> {

        private String val;

        @Override
        public Optional<Sample> transform() {
            if (val == null) {
                return Optional.absent();
            }

            return Optional.of(new Sample(val));
        }

        @Override
        public void visit(String name, String value, Map<String, String> attribs) {
            if (!Strings.isNullOrEmpty(value)) {
                val = value;
            }
        }

        @Override
        public void reset() {
            val = null;
        }
    }

    @Before
    public void createParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
    }

    @Test
    public void testReadRSSItems() throws Exception {

        XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
                .from(SAMPLE_XML)
                .pathOf("n1/l2/i1")
                .withTransform(new SampleTransformer())
                .withParser(parser)
                .create();

        List<Sample> samples = Lists.newArrayList(xitr);

        assertTrue("Contains elements.", !samples.isEmpty());
        assertTrue("Elements correct.", samples.toString().equals("[text1, text2, text3]"));
    }

    /**
     * Test that if transformer returns absent,
     * iterable returns data as expected.
     *
     * @throws Exception
     */
    @Test
    public void testSkipBadElement() throws Exception {
        String sampleXML = "<n1>\n" +
                "    <l2>\n" +
                "        <i1 attrib=\"3\">text1</i1>\n" +
                "        <i1 attrib=\"6\">text2</i1>\n" +
                "        <i1 attrib=\"7\"></i1>\n" +
                "        <i1 attrib=\"9\">text3</i1>\n" +
                "    </l2>\n" +
                "</n1>";

        XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
                .from(sampleXML)
                .pathOf("n1/l2/i1")
                .withTransform(new SampleTransformer())
                .withParser(parser)
                .create();

        List<Sample> samples = Lists.newArrayList(xitr);

        assertTrue("Expected element count.", samples.size() == 3);
        assertTrue("Elements correct.", samples.toString().equals("[text1, text2, text3]"));
    }

    @Test
    public void testNoData() throws Exception {
        XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
                .from("")
                .pathOf("n1/l2/i1")
                .withTransform(new SampleTransformer())
                .withParser(parser)
                .create();

        assertTrue("Iterable is empty.", !xitr.iterator().hasNext());
    }
}