package com.abk.xmlobjectiterable;

import android.util.Xml;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.io.Closeables;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Transform regular XML elements into POJOs.
 */
public final class XMLObjectIterable<T> implements Iterable<T> {

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

        /**
         * Defines the xml element which signifies
         * the root of the POJO to be created.
         *
         * Required
         *
         * @return path path of root element
         */
        String getPath();
    }

    public static class XmlTraversalState {
        int event;
        String lastNodeName;
        Map<String, String> lastNodeAttribs;

        public XmlTraversalState(int event, String lastNodeName, Map<String, String> lastNodeAttribs) {
            this.event = event;
            this.lastNodeName = lastNodeName;
            this.lastNodeAttribs = lastNodeAttribs;
        }
    }

    /**
     * TODO
     */
    private final class XmlPathNodeEvaluator implements Predicate<XmlTraversalState> {
        private static final String PATH_SEPARATOR = "/";
        private final List<String> xmlPath;
        private final List<String> nodeStack;

        public XmlPathNodeEvaluator(String xPath) {
            this.xmlPath = Splitter
                    .on(PATH_SEPARATOR)
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(xPath);
            this.nodeStack = new ArrayList<>(xmlPath.size());
        }

        @Override
        public boolean apply(XmlTraversalState input) {
            switch (input.event) {
                case XmlPullParser.START_TAG:
                    nodeStack.add(parser.getName());
                    break;
                case XmlPullParser.END_TAG:
                    nodeStack.remove(nodeStack.size() - 1);
                    break;
            }

            return nodeStack.hashCode() == xmlPath.hashCode();
        }
    }

    /**
     * Builder for the XMLObjectIterable
     *
     * @param <T> type of POJO
     */
    public static final class Builder<T> {
        private InputStream is;
        private Transformer<T> transformer;
        private XmlPullParser pullParser;

        /**
         * Read XML from an InputStream.
         *
         * One call to from() is required.
         *
         * @param is InputStream
         * @return builder
         */
        public Builder<T> from(final InputStream is) {
            this.is = is;
            Preconditions.checkNotNull(this.is, "InputStream cannot be null.");
            return this;
        }

        /**
         * Read XML from a String.
         *
         * One call to from() is required.
         *
         * @param xml String of XML document
         * @return builder
         */
        public Builder<T> from(final String xml) {
            this.is = new ByteArrayInputStream(xml.getBytes());
            Preconditions.checkNotNull(this.is, "InputStream cannot be null.");
            return this;
        }

        /**
         * Read XML from a classloader.
         *
         * One call to from() is required.
         *
         * @param clazz class containing correct classloader.
         * @param resourcePath path to resource
         * @return builder
         */
        public Builder<T> from(final Class<?> clazz, final String resourcePath) {
            this.is = clazz.getResourceAsStream(resourcePath);
            Preconditions.checkNotNull(this.is, "InputStream cannot be null.");
            return this;
        }

        /**
         * Defines the Transformer that will generate
         * POJOs for each matched path element.
         *
         * Required
         *
         * @param transformer Transformer instance
         * @return builder
         */
        public Builder<T> withTransform(final Transformer<T> transformer) {
            this.transformer = transformer;
            return this;
        }

        /**
         * Creates the iterable.
         * Will throw a RuntimeException if insufficient
         * input state is supplied.
         *
         * @return XMLObjectIterable
         */
        public XMLObjectIterable<T> create() {
            Preconditions.checkNotNull(is, "Must call from() on builder.");
            Preconditions.checkNotNull(transformer, "Must call withTransform() on builder.");

            return new XMLObjectIterable<>(pullParser, is, transformer);
        }

        public Builder<T> withParser(final XmlPullParser parser) {
            this.pullParser = parser;
            return this;
        }
    }

    /**
     * Iterates over XML document with a pull parser.
     *
     * @param <T>
     */
    private static final class PullParserIterable<T> implements Iterable<T> {

        private static final String PATH_SEPARATOR = "/";

        /**
         * PullParser can scan for the next valid node
         * based on xmlPath or create a POJO if parsing
         * nodes that are part of the POJO.
         */
        enum TraversalMode {
            SCAN, POPULATE;
        }

        private final XmlPullParser parser;
        private InputStream inputStream;
        private final Predicate<XmlTraversalState> parsePredicate;
        //private final List<String> xmlPath;
        private final Transformer<T> transformer;
        //private final List<String> nodeStack = new ArrayList<String>();
        private TraversalMode traversalMode = TraversalMode.SCAN;

        /**
         * @param parser      pull parser initialized with input.
         * @param is          inputStream of XML
         * @param parsePredicate Predicate to determine of transformer shall be called on given node
         * @param transformer instance of a transformer that generates the POJOs.
         */
        public PullParserIterable(final XmlPullParser parser, final InputStream is, final Predicate<XmlTraversalState> parsePredicate, final Transformer<T> transformer) {
            this.parser = parser;
            this.inputStream = is;
            this.parsePredicate = parsePredicate;
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

                    final Optional<T> nextOpt = loop();

                    if (nextOpt != null) {
                        next = nextOpt.get();
                        return true;
                    }

                    //No data, close stream.
                    Closeables.closeQuietly(inputStream);

                    return false;
                }

                @Override
                public T next() {
                    if (next == null) {
                        final Optional<T> nextOpt = loop();

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

                /**
                 * Scan the input until the transformer
                 * returns a POJO or we reach the end
                 * of the doucment.
                 *
                 * @return optional POJO
                 */
                private Optional<T> loop() {
                    Optional<T> nextOpt;
                    do {
                        nextOpt = getNext();
                    } while (nextOpt != null && !nextOpt.isPresent());

                    return nextOpt;
                }

                /**
                 * Parse the input XML and create a POJO
                 * when the next xmlPath element is found.
                 * @return optional POJO
                 */
                private Optional<T> getNext() {
                    int nextTokenType;
                    int nestLevel = 0;
                    String lastNodeName = null;
                    String lastNodeText = null;
                    final Map<String, String> lastNodeAttribs = new HashMap<>();
                    XmlTraversalState xts = new XmlTraversalState(-1, null, null);

                    try {
                        while ((nextTokenType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            if (traversalMode == TraversalMode.SCAN) {
                                xts.event = nextTokenType;
                                //TODO call nodeEval
                                switch (nextTokenType) {
                                    case XmlPullParser.START_TAG:
                                        lastNodeName = parser.getName();
                                        //nodeStack.add(parser.getName());
                                        loadAttribs(parser, lastNodeAttribs);
                                        break;
                                    case XmlPullParser.END_TAG:
                                        //nodeStack.remove(nodeStack.size() - 1);
                                        lastNodeAttribs.clear();
                                        break;
                                }

                                xts.lastNodeAttribs = lastNodeAttribs;
                                xts.lastNodeName = lastNodeName;
                                if (parsePredicate.apply(xts)) {
                                    traversalMode = TraversalMode.POPULATE;
                                    //lastNodeName = nodeStack.get(nodeStack.size() - 1);
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
                                    //nodeStack.remove(nodeStack.size() - 1);

                                    final Optional<T> val = transformer.transform();
                                    transformer.reset();

                                    return val;
                                }
                            }
                        }
                    } catch (XmlPullParserException | IOException e) {
                        throw new RuntimeException("Error while parsing XML.", e);
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
        private static void loadAttribs(final XmlPullParser parser, final Map<String, String> lastNodeAttribs) {
            final int attribCount = parser.getAttributeCount();

            for (int index = 0; index < attribCount; ++index) {
                lastNodeAttribs.put(parser.getAttributeName(index), parser.getAttributeValue(index));
            }
        }

        /**
         * @param l1 list 1
         * @param l2 list 2
         * @return true if lists contain same elements, false otherwise
         */
        private boolean listsEqual(final List<String> l1, final List<String> l2) {
            return l1.hashCode() == l2.hashCode();
        }
    }

    private final String xmlPath;
    private final Transformer<T> transformer;
    private final InputStream is;
    private final XmlPullParser parser;

    private XMLObjectIterable(final XmlPullParser pullParser, final InputStream is, final Transformer<T> transformer) {
        this.is = is;
        this.transformer = transformer;
        this.xmlPath = transformer.getPath();
        this.parser = pullParser;
    }

    @Override
    public Iterator<T> iterator() {

        XmlPullParser pullParser = parser;
        if (pullParser == null) {
            pullParser = createDefaultParser(is);
        } else {
            try {
                pullParser.setInput(is, null);
            } catch (final XmlPullParserException e) {
                throw new RuntimeException("Failed to read stream.", e);
            }
        }

        final PullParserIterable<T> iterable = new PullParserIterable<T>(pullParser, is, new XmlPathNodeEvaluator(xmlPath), transformer);

        return iterable.iterator();
    }

    private static XmlPullParser createDefaultParser(final InputStream inputStream) {
        try {
            final XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(inputStream, null);

            return parser;
        } catch (final Exception e) {
            throw new RuntimeException("Failed to create XmlPullParser.", e);
        }
    }
}