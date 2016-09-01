package com.abk.xmlobjectiterable.core;

import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.abk.xmlobjectiterable.model.Book;
import com.abk.xmlobjectiterable.transformers.BookTransformer;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Created by kgilmer on 8/14/16.
 */
public class BookTest {

    private XmlPullParser parser;

    @Before
    public void createParser() throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        parser = factory.newPullParser();
    }

    @Test
    public void testParseBooks() throws Exception {
        final XMLObjectIterable<Book> bookIterable = new XMLObjectIterable.Builder<Book>()
                .onNodes("/bookstore/book")
                .withParser(parser)
                .withTransform(new BookTransformer())
                .from(this.getClass(), "/books.xml")
                .create();

        List<Book> bookList = new ArrayList<>();

        Iterables.addAll(bookList, bookIterable);
        assertTrue("4 books in XML", bookList.size() == 4);
        assertTrue(bookList.get(0).getTitle().equals("Everyday Italian"));
        assertTrue(bookList.get(1).getTitle().equals("Harry Potter"));
        assertTrue(bookList.get(2).getAuthor().size() == 5);
    }
}
