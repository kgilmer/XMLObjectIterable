package com.abk.xmlobjectiterable;

import com.google.common.base.Optional;

import java.util.List;

/**
 * Implementations act on XML traversals in visit() to generate
 * POJOs via transform().
 *
 * @param <T> type of POJO
 */
public interface XMLTransformer<T> {

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
     * @param node    XmlNodeValue
     * @param path
     */
    void visit(XMLElement node, List<String> path);

    /**
     * Called after transform, signals that POJO generation should be
     * reset for next element.
     */
    void reset();

    /**
     *
     * @return true if all required data has been loaded via Transformer.visit().
     */
    boolean canTransform();
}
