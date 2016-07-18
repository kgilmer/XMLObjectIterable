package com.abk.xmlobjectiterable;

//import android.util.Xml;

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
 * Transform and iterate over XML elements as POJOs.
 *
 * See http://github.com/kgilmer/XMLObjectIterable for details.
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
         * <p/>
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
     * Captures mutable state during XML stream scanning.
     */
    public static class XmlTraversalState {
        private int event;
        private String lastNodeName;
        private Map<String, String> lastNodeAttribs;

        public XmlTraversalState(int event, String lastNodeName, Map<String, String> lastNodeAttribs) {
            this.event = event;
            this.lastNodeName = lastNodeName;
            this.lastNodeAttribs = lastNodeAttribs;
        }
    }

    /**
     * Determines if a given XML event should be considered
     * for transformation into a POJO and subsequently passed
     * back to the client as data.
     *
     * This implementation only evaluates node names and depth.
     */
    private final class XmlPathNodeEvaluator implements Predicate<XmlTraversalState> {
        private static final String PATH_SEPARATOR = "/";
        private final List<String> xmlPath;
        private final List<String> nodeStack;

        /**
         * @param xPath example "node1/node2/"
         */
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
        private String xmlPath;
        private Predicate<XmlTraversalState> transformPredicate;

        /**
         * Read XML from an InputStream.
         * <p/>
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

        public Builder<T> onNodes(String xmlPath) {
            if (transformPredicate != null) {
                throw new RuntimeException("Must specify only one xml path or transform predicate.");
            }
            this.xmlPath = xmlPath;
            return this;
        }

        public Builder<T> transformPredicate(Predicate<XmlTraversalState> predicate) {
            if (xmlPath != null) {
                throw new RuntimeException("Must specify only one xml path or transform predicate.");
            }

            this.transformPredicate = predicate;
            return this;
        }

        /**
         * Read XML from a String.
         * <p/>
         * One call to from() is required.
         *
         * NOTE: This data will be ignored if
         * parser passed already has input set.
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
         * <p/>
         * One call to from() is required.
         *
         * NOTE: This data will be ignored if
         * parser passed already has input set.
         *
         * @param clazz        class containing correct classloader.
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
         * <p/>
         * Required
         *
         * NOTE: This data will be ignored if
         * parser passed already has input set.
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
            Preconditions.checkNotNull(transformer, "Must call withTransform() on builder.");
            Preconditions.checkNotNull(pullParser, "Must set a XmlPullParser instance.");

            return new XMLObjectIterable<>(pullParser, is, transformer, transformPredicate, xmlPath);
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

        private static final XmlTraversalState END_NODE_STATE =
                new XmlTraversalState(XmlPullParser.END_TAG, null, null);

        /**
         * PullParser can scan for the next valid node
         * based on xmlPath or create a POJO if parsing
         * nodes that are part of the POJO.
         */
        private static final int SCAN_MODE = 1;
        private static final int LOAD_MODE = 2;

        private final XmlPullParser parser;
        private final InputStream inputStream;
        private final Predicate<XmlTraversalState> parsePredicate;
        private final Transformer<T> transformer;
        private int traversalMode = SCAN_MODE;

        /**
         * @param parser         pull parser initialized with input.
         * @param is             inputStream of XML
         * @param parsePredicate Predicate to determine of transformer shall be called on given node
         * @param transformer    instance of a transformer that generates the POJOs.
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
                 * of the document.
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
                            if (traversalMode == SCAN_MODE) {
                                xts.event = nextTokenType;
                                switch (nextTokenType) {
                                    case XmlPullParser.START_TAG:
                                        lastNodeName = parser.getName();
                                        loadAttribs(parser, lastNodeAttribs);
                                        break;
                                    case XmlPullParser.END_TAG:
                                        lastNodeAttribs.clear();
                                        break;
                                }

                                xts.lastNodeAttribs = lastNodeAttribs;
                                xts.lastNodeName = lastNodeName;
                                if (parsePredicate.apply(xts)) {
                                    traversalMode = LOAD_MODE;
                                    nestLevel++;
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
                                    traversalMode = SCAN_MODE;
                                    //Cause the xml path to remove the last element
                                    //Signal to the predicate that an element has been
                                    //retrieved.
                                    parsePredicate.apply(END_NODE_STATE);

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
         *
         * @param parser          parser at node start
         * @param lastNodeAttribs map of attribs.
         */
        private static void loadAttribs(final XmlPullParser parser, final Map<String, String> lastNodeAttribs) {
            final int attribCount = parser.getAttributeCount();

            for (int index = 0; index < attribCount; ++index) {
                lastNodeAttribs.put(parser.getAttributeName(index), parser.getAttributeValue(index));
            }
        }
    }

    private final Transformer<T> transformer;
    private Predicate<XmlTraversalState> transformPredicate;
    private final InputStream is;
    private final XmlPullParser parser;
    private String xmlPath;

    private XMLObjectIterable(final XmlPullParser pullParser, final InputStream is, final Transformer<T> transformer, final Predicate<XmlTraversalState> transformPredicate, String xmlPath) {
        this.is = is;
        this.transformer = transformer;
        this.transformPredicate = transformPredicate;
        this.parser = pullParser;
        this.xmlPath = xmlPath;
    }

    @Override
    public Iterator<T> iterator() {
        // If inputStream was specified in Builder, set it on the parser.
        if (is != null) {
            try {
                parser.setInput(is, null);
            } catch (final XmlPullParserException e) {
                throw new RuntimeException("Failed to read stream.", e);
            }
        }

        // If transformPredicate was not specified in the builder use
        // the default XML node path predicate.
        if (transformPredicate == null && xmlPath != null) {
            transformPredicate = new XmlPathNodeEvaluator(xmlPath);
        } else if (transformPredicate == null) {
            throw new RuntimeException("Must specify XML path or transform predicate in builder.");
        }

        final PullParserIterable<T> iterable =
                new PullParserIterable<T>(parser, is, transformPredicate, transformer);

        return iterable.iterator();
    }
}