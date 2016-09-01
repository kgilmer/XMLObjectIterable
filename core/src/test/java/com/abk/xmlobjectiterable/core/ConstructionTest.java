package com.abk.xmlobjectiterable.core;

import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.google.common.collect.Lists;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Validates required and subsets of builder parameters.
 */
public class ConstructionTest {

    @Test(expected = RuntimeException.class)
    public void testMustSpecifyParser() throws Exception {
        XMLObjectIterable<UsageUnitTest.Sample> xmlItr = new XMLObjectIterable.Builder<UsageUnitTest.Sample>()
                .onNodes("a/b/c")
                .from(UsageUnitTest.SAMPLE_XML)
                .withTransform(new UsageUnitTest.SampleTransformer())
                .create();
    }

    @Test(expected = RuntimeException.class)
    public void testMustSpecifyTransform() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();

        XMLObjectIterable<UsageUnitTest.Sample> xmlItr = new XMLObjectIterable.Builder<UsageUnitTest.Sample>()
                .onNodes("a/b/c")
                .from(UsageUnitTest.SAMPLE_XML)
                .withParser(parser)
                .create();
    }

    @Test
    public void testHappyPath() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();

        XMLObjectIterable<UsageUnitTest.Sample> xmlItr = new XMLObjectIterable.Builder<UsageUnitTest.Sample>()
                .onNodes(UsageUnitTest.XML_PATH)
                .from(UsageUnitTest.SAMPLE_XML)
                .withParser(parser)
                .withTransform(new UsageUnitTest.SampleTransformer())
                .create();
    }

    @Test
    public void testParserSuppliesInput() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(
                new InputStreamReader(
                        new ByteArrayInputStream(UsageUnitTest.SAMPLE_XML.getBytes())));

        XMLObjectIterable<UsageUnitTest.Sample> xmlItr = new XMLObjectIterable.Builder<UsageUnitTest.Sample>()
                .onNodes(UsageUnitTest.XML_PATH)
                .withParser(parser)
                .withTransform(new UsageUnitTest.SampleTransformer())
                .create();

        List<UsageUnitTest.Sample> elements = Lists.newArrayList(xmlItr);

        assertTrue("Iterable returns at least one element.", !elements.isEmpty());
    }
}
