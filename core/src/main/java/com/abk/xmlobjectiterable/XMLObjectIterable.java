package com.abk.xmlobjectiterable;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
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
 * <p/>
 * See http://github.com/kgilmer/XMLObjectIterable for details.
 */
public final class XMLObjectIterable<T> implements Iterable<T> {

    /**
     * Builder for the XMLObjectIterable
     *
     * @param <T> type of POJO
     */
    public static final class Builder<T> {
        private InputStream is;
        private XMLTransformer<T> transformer;
        private XmlPullParser pullParser;
        private List<String> rootNodePath;

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
            if (rootNodePath != null) {
                throw new RuntimeException("Must specify only one xml path or transform predicate.");
            }
            this.rootNodePath = Splitter
                    .on('/')
                    .omitEmptyStrings()
                    .trimResults()
                    .splitToList(xmlPath);
            return this;
        }

        /**
         * @param path list of xml elements which define root of node to transform.
         * @return Builder
         */
        public Builder<T> onNodes(List<String> path) {
            if (rootNodePath != null) {
                throw new RuntimeException("Must specify only one xml path or transform predicate.");
            }

            this.rootNodePath = path;
            return this;
        }

        /**
         * Read XML from a String.
         * <p/>
         * One call to from() is required.
         * <p/>
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
         * <p/>
         * NOTE: This data will be ignored if
         * parser passed already has input set.
         *
         * @param clazz        class containing correct classloader.
         * @param resourcePath path to resource
         * @return builder
         */
        public Builder<T> from(final Class<?> clazz, final String resourcePath) {
            this.is = clazz.getResourceAsStream(resourcePath);
            Preconditions.checkNotNull(this.is, "Failed to load resource: " + resourcePath);
            return this;
        }

        /**
         * Defines the Transformer that will generate
         * POJOs for each matched path element.
         * <p/>
         * Required
         * <p/>
         * NOTE: This data will be ignored if
         * parser passed already has input set.
         *
         * @param transformer Transformer instance
         * @return builder
         */
        public Builder<T> withTransform(final XMLTransformer<T> transformer) {
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

            return new XMLObjectIterable<>(pullParser, is, transformer, rootNodePath);
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

        private static final int DEPTH_OUTSIDE = 0;
        private static final int DEPTH_AT_ROOT = 1;
        private static final int DEPTH_INSIDE = 2;

        private final XmlPullParser parser;
        private final InputStream inputStream;
        private final XMLTransformer<T> transformer;
        private final List<String> rootNodePath;
        private Deque<XMLElement> nodeValueStack = new LinkedList<>();
        private Deque<String> nodeNameStack = new LinkedList<>();

        /**
         * @param parser         pull parser initialized with input.
         * @param is             inputStream of XML
         * @param rootNodePath   Predicate to determine of transformer shall be called on given node
         * @param transformer    instance of a transformer that generates the POJOs.
         */
        public PullParserIterable(final XmlPullParser parser, final InputStream is, final List<String> rootNodePath, final XMLTransformer<T> transformer) {
            this.parser = parser;
            this.inputStream = is;
            this.rootNodePath = rootNodePath;
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
                 *
                 * @return optional POJO
                 */
                private Optional<T> getNext() {
                    int nextTokenType;

                    try {
                        while ((nextTokenType = parser.next()) != XmlPullParser.END_DOCUMENT) {
                            switch (nextTokenType) {
                                case XmlPullParser.START_TAG:
                                    nodeNameStack.addLast(parser.getName());
                                    nodeValueStack.addLast(new XMLElement(parser.getName(), null, loadAttribs(parser)));
                                    break;
                                case XmlPullParser.TEXT:
                                    nodeValueStack.peekLast().setValue(parser.getText());
                                    break;
                                case XmlPullParser.END_TAG:
                                    final int depth = getNodeDepth(rootNodePath, nodeNameStack);
                                    nodeNameStack.removeLast();
                                    final XMLElement lastNode = nodeValueStack.removeLast();

                                    switch (depth) {
                                        case DEPTH_AT_ROOT:
                                            transformer.visit(lastNode);
                                            if (transformer.canTransform()) {
                                                final Optional<T> val = transformer.transform();
                                                transformer.reset();

                                                return val;
                                            }
                                            break;
                                        case DEPTH_INSIDE:
                                            transformer.visit(lastNode);
                                            break;
                                        default:
                                            break;
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

        private int getNodeDepth(List<String> rootNodePath, Deque<String> nodeNameStack) {
            if (nodeNameStack.size() < rootNodePath.size()) {
                return DEPTH_OUTSIDE;
            }

            if (nodeNameStack.hashCode() == rootNodePath.hashCode()) {
                return DEPTH_AT_ROOT;
            }

            return DEPTH_INSIDE;
        }

        /**
         * Load an XML element's attributes into a map
         *
         * @param parser          parser at node start
         * @return map of attribs.
         */
        private static Map<String, String> loadAttribs(final XmlPullParser parser) {
            Map<String, String> attribs = new HashMap<>();
            final int attribCount = parser.getAttributeCount();

            for (int index = 0; index < attribCount; ++index) {
                attribs.put(parser.getAttributeName(index), parser.getAttributeValue(index));
            }
            return attribs;
        }
    }

    private final XMLTransformer<T> transformer;
    private final InputStream is;
    private final XmlPullParser parser;
    private List<String> rootNodePath;

    private XMLObjectIterable(final XmlPullParser pullParser, final InputStream is, final XMLTransformer<T> transformer, final List<String> rootNodePath) {
        this.is = is;
        this.transformer = transformer;
        this.parser = pullParser;
        this.rootNodePath = rootNodePath;
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

        final PullParserIterable<T> iterable =
                new PullParserIterable<>(parser, is, rootNodePath, transformer);

        return iterable.iterator();
    }
}