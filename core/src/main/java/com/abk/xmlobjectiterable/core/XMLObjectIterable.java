package com.abk.xmlobjectiterable.core;

import android.util.Xml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Transform regular XML elements into POJOs.
 */
public class XMLObjectIterable<T> implements Iterable<T> {

    /**
     * Implementations act on XML traversals in visit() to generate
     * POJOs via transform().
     *
     * @param <T> type of POJO
     */
    public interface Transformer<T> {

        /**
         * @return instance of POJO or absent of
         * required data not available.
         */
        Optional<T> transform();

        /**
         * Callback to populate POJO with
         * values from XML element.
         *
         * This method may be called multiple times
         * before transform() is called, depending
         * on the XML structure.
         *
         * @param name    xml element name
         * @param value   xml element value
         * @param attribs map of xml attributes
         */
        void visit(String name, String value, Map<String, String> attribs);

        /**
         * Called after transform, signals that POJO generation should be
         * reset for next element.
         */
        void reset();
    }

    /**
     * Builder for the XMLObjectIterable
     *
     * @param <T> type of POJO
     */
    public static class Builder<T> {
        private InputStream is;
        private String xpath;
        private Transformer<T> transformer;
        private XmlPullParser pullParser;

        public Builder<T> from(final InputStream is) {
            Preconditions.checkNotNull(is, "InputStream cannot be null.");
            this.is = is;
            return this;
        }

        public Builder<T> from(String sampleXml) {
            this.is = new ByteArrayInputStream(sampleXml.getBytes());
            return this;
        }

        public Builder<T> pathOf(final String xpath) {
            this.xpath = xpath;
            return this;
        }

        public Builder<T> withTransform(final Transformer<T> transformer) {
            this.transformer = transformer;
            return this;
        }

        public XMLObjectIterable<T> create() {
            Preconditions.checkNotNull(is, "Must call from() on builder.");
            Preconditions.checkNotNull(xpath, "Must call pathOf() on builder.");
            Preconditions.checkNotNull(transformer, "Must call withTransform() on builder.");

            return new XMLObjectIterable<>(pullParser, is, xpath, transformer);
        }

        public Builder<T> withParser(XmlPullParser parser) {
            this.pullParser = parser;
            return this;
        }
    }

    /**
     * Iterates over XML document with a pull parser.
     *
     * @param <T>
     */
    private static class PullParserIterable<T> implements Iterable<T> {

        enum TraversalMode {
            SCAN, POPULATE;
        }

        private final XmlPullParser parser;
        private final List<String> xpath;
        private final Transformer<T> transformer;
        private final List<String> nodeStack = new ArrayList<String>();
        private TraversalMode traversalMode = TraversalMode.SCAN;

        /**
         * @param parser      pull parser initialized with input.
         * @param xpath       '/' separated string of xml elements from which POJOs are initialized
         *                    For example 'rss/channel/item' for Items in an RSS feed.
         * @param transformer instance of a transformer that generates the POJOs.
         */
        public PullParserIterable(final XmlPullParser parser, final String xpath, final Transformer<T> transformer) {
            this.parser = parser;
            this.xpath = new ArrayList<>();
            final String[] nodes = xpath.split("/");
            for (final String node : nodes) {
                this.xpath.add(node.trim());
            }
            this.transformer = transformer;
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                T next = null;

                @Override
                public boolean hasNext() {
                    if (next != null) {
                        throw new IllegalStateException("next expected to be null.");
                    }

                    Optional<T> nextOpt = loop();

                    if (nextOpt != null) {
                        next = nextOpt.get();
                        return true;
                    }

                    return false;
                }

                @Override
                public T next() {
                    if (next == null) {
                        Optional<T> nextOpt = loop();

                        if (nextOpt != null) {
                            next = nextOpt.get();
                        } else {
                            throw new IllegalStateException("No data.");
                        }
                    }

                    final T nv = next;
                    next = null;

                    return nv;
                }

                private Optional<T> loop() {
                    Optional<T> nextOpt;
                    do {
                        nextOpt = getNext();
                    } while (nextOpt != null && !nextOpt.isPresent());

                    return nextOpt;
                }

                private Optional<T> getNext() {

                    int nextTokenType;
                    int nestLevel = 0;
                    String lastNodeName = null;
                    String lastNodeText = null;
                    Map<String, String> lastNodeAttribs = new HashMap<>();

                    try {
                        while ((nextTokenType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            if (traversalMode == TraversalMode.SCAN) {
                                switch (nextTokenType) {
                                    case XmlPullParser.START_TAG:
                                        nodeStack.add(parser.getName());
                                        loadAttribs(parser, lastNodeAttribs);
                                        break;
                                    case XmlPullParser.END_TAG:
                                        nodeStack.remove(nodeStack.size() - 1);
                                        lastNodeAttribs.clear();
                                        break;
                                }

                                if (listsEqual(xpath, nodeStack)) {
                                    traversalMode = TraversalMode.POPULATE;
                                    lastNodeName = nodeStack.get(nodeStack.size() - 1);
                                    nestLevel++;
                                    //return transformer.apply(parser);
                                }
                            } else {
                                switch (nextTokenType) {
                                    case XmlPullParser.START_TAG:
                                        lastNodeName = parser.getName();
                                        loadAttribs(parser, lastNodeAttribs);
                                        nestLevel++;
                                        break;
                                    case XmlPullParser.TEXT:
                                        lastNodeText = parser.getText();
                                        break;
                                    case XmlPullParser.END_TAG:
                                        if (lastNodeName != null) {
                                            transformer.visit(lastNodeName, lastNodeText, lastNodeAttribs);
                                        }
                                        nestLevel--;
                                        lastNodeName = null;
                                        lastNodeText = null;
                                        lastNodeAttribs.clear();
                                        break;
                                }

                                if (nestLevel == 0) {
                                    //Returned to the root of the XPATH, should be
                                    //able to construct the POJO
                                    traversalMode = TraversalMode.SCAN;
                                    nodeStack.remove(nodeStack.size() - 1);

                                    final Optional<T> val = transformer.transform();
                                    transformer.reset();

                                    return val;
                                }
                            }
                        }
                    } catch (XmlPullParserException | IOException e) {
                        e.printStackTrace();
                    }

                    return null;
                }

                @Override
                public void remove() {
                    throw new RuntimeException("Unsupported operation.");
                }
            };
        }

        /**
         * Load an XML element's attributes into a map
         * @param parser parser at node start
         * @param lastNodeAttribs map of attribs.
         */
        private static void loadAttribs(XmlPullParser parser, Map<String, String> lastNodeAttribs) {
            final int attribCount = parser.getAttributeCount();

            for (int index = 0; index < attribCount; ++index) {
                lastNodeAttribs.put(parser.getAttributeName(index), parser.getAttributeValue(index));
            }
        }

        private Map<String, String> getAttribs(final XmlPullParser parser) {
            //TODO: Implement
            return Collections.emptyMap();
        }

        private boolean listsEqual(final List<String> l1, final List<String> l2) {
            return l1.hashCode() == l2.hashCode();
        }
    }

    private final String xpath;
    private final Transformer<T> transformer;
    private final InputStream is;
    private final XmlPullParser parser;

    private XMLObjectIterable(XmlPullParser pullParser, final InputStream is, final String xpath, final Transformer<T> transformer) {
        this.is = is;
        this.xpath = xpath;
        this.transformer = transformer;
        this.parser = pullParser;
    }

    @Override
    public Iterator<T> iterator() {

        XmlPullParser pullParser = parser;
        if (pullParser == null) {
            pullParser = createParser(is);
        } else {
            try {
                pullParser.setInput(is, null);
            } catch (XmlPullParserException e) {
                throw new RuntimeException("Failed to read stream.", e);
            }
        }

        final PullParserIterable<T> iterable = new PullParserIterable<T>(pullParser, xpath, transformer);

        return iterable.iterator();
    }

    private static XmlPullParser createParser(final InputStream inputStream) {
        try {
            final XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            return parser;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create XmlPullParser.", e);
        }
    }
}