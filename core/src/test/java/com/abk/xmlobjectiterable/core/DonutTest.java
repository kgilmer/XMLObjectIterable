package com.abk.xmlobjectiterable.core;

import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.abk.xmlobjectiterable.model.Book;
import com.abk.xmlobjectiterable.model.Donut;
import com.abk.xmlobjectiterable.transformers.BookTransformer;
import com.abk.xmlobjectiterable.transformers.DonutTransformer;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by kgilmer on 9/3/16.
 */
public class DonutTest {

    private XmlPullParser parser;

    @Before
    public void createParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
    }

    @Test
    public void testParseDonuts() throws Exception {
        final XMLObjectIterable<Donut> donutIterable = new XMLObjectIterable.Builder<Donut>()
                .onNodes("/items/item")
                .withParser(parser)
                .withTransform(new DonutTransformer())
                .from(this.getClass(), "/donuts.xml")
                .create();

        List<Donut> donutList = new ArrayList<>();

        Iterables.addAll(donutList, donutIterable);
        assertTrue("6 donuts in XML", donutList.size() == 6);
        assertTrue("Names in order",
                donutList.get(0).getName().equals("Cake")
                        && donutList.get(1).getName().equals("Raised")
                        && donutList.get(2).getName().equals("Buttermilk")
                        && donutList.get(3).getName().equals("Bar")
                        && donutList.get(4).getName().equals("Twist")
                        && donutList.get(5).getName().equals("Filled"));

        assertTrue("Filling count", donutList.get(0).getFilling().size() == 0
                && donutList.get(1).getFilling().size() == 0
                && donutList.get(2).getFilling().size() == 0
                && donutList.get(3).getFilling().size() == 3
                && donutList.get(4).getFilling().size() == 0
                && donutList.get(5).getFilling().size() == 4);

    }
}
