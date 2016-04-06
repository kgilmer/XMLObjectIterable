package com.abk.xmlobjectiterable.transformers;

import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Usage unit tests
 */
public class RSSBookmarkItemUnitTest {

    private XmlPullParser parser;

    @Before
    public void createParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
    }

    @Test
    public void testReadRSSItems() throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("rss-opml.xml");

        XMLObjectIterable<RSSBookmarkItem> xitr = new XMLObjectIterable.Builder<RSSBookmarkItem>()
                .from(is)
                .withTransform(RSSBookmarkItem.TRANSFORMER)
                .withParser(parser)
                .create();

        List<RSSBookmarkItem> rssItems = Lists.newArrayList(xitr);


        Set<String> titles = new HashSet<>();
        for (RSSBookmarkItem i : rssItems) {
            if (!Strings.isNullOrEmpty(i.getTitle())) {
                assertFalse("Have not already seen title " + i.getTitle(), titles.contains(i.getTitle()));
                titles.add(i.getTitle());
            }
        }
        assertTrue("Item names are unique and all present.", titles.size() == 405);

        assertTrue("Contains 407 elements.", rssItems.size() == 405);
    }



}