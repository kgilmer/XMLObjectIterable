package com.abk.xmlobjectiterable;

import java.util.Map;

/**
 * Captures the local data of a given node.
 */
public class XMLElement {
    private final String name;
    private String value;
    private final Map<String, String> attribs;

    public XMLElement(String name, String value, Map<String, String> attribs) {
        this.name = name;
        this.value = value;
        this.attribs = attribs;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public Map<String, String> getAttribs() {
        return attribs;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "XmlNodeValue{" +
                "name='" + name + '\'' +
                ", value='" + value + '\'' +
                ", attribs=" + attribs +
                '}';
    }
}
