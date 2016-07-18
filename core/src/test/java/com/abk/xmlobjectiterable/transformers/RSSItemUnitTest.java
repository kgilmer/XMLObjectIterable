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

import static org.junit.Assert.assertTrue;

/**
 * Usage unit tests
 */
public class RSSItemUnitTest {

    private XmlPullParser parser;

    @Before
    public void createParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
    }

    @Test
    public void testReadRSSItems() throws Exception {

        InputStream is = this.getClass().getClassLoader().getResourceAsStream("rss.xml");

        XMLObjectIterable<RSSItem> xitr = new XMLObjectIterable.Builder<RSSItem>()
                .from(is)
                .withTransform(RSSItem.RSS_TRANSFORMER)
                .onNodes(RSSItem.RSS_PATH)
                .withParser(parser)
                .create();

        List<RSSItem> rssItems = Lists.newArrayList(xitr);

        assertTrue("Contains 30 elements.", rssItems.size() == 30);
        Set<String> titles = new HashSet<>();
        for (RSSItem i : rssItems) {
            if (!Strings.isNullOrEmpty(i.getTitle())) {
                titles.add(i.getTitle());
            }
        }
        assertTrue("Item names are unique and all present.", titles.size() == 30);
    }

}