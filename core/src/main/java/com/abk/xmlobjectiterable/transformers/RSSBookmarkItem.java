package com.abk.xmlobjectiterable.transformers;

import com.abk.xmlobjectiterable.XMLElement;
import com.abk.xmlobjectiterable.XMLTransformer;
import com.google.common.base.Optional;

import java.util.List;

/**
 * RSS OPML
 *
 * Example:
 * <opml version="1.1">
 * <head>
 *  <title>NewsBlur Feeds</title>
 *  <dateCreated>2013-12-24 23:58:13.332791</dateCreated>
 *  <dateModified>2013-12-24 23:58:13.332791</dateModified>
 * </head>
 * <body>
 *  <outline text="Frontend" title="Frontend">
 *      <outline htmlUrl="http://www.smashingmagazine.com" text="Smashing Magazine" title="Smashing Magazine" type="rss" version="RSS" xmlUrl="http://rss1.smashingmagazine.com/feed/" />
 *  </outline>
 * </body>
 *
 */
public class RSSBookmarkItem {
    public static final String PATH = "opml/body/outline/outline";
    private final String title;
    private final String htmlUrl;
    private final String type;
    private final String xmlUrl;

    public RSSBookmarkItem(String title, String htmlUrl, String type, String xmlUrl) {
        this.title = title;
        this.htmlUrl = htmlUrl;
        this.type = type;
        this.xmlUrl = xmlUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public String getType() {
        return type;
    }

    public String getXmlUrl() {
        return xmlUrl;
    }

    @Override
    public String toString() {
        return title;
    }

    public static final XMLTransformer<RSSBookmarkItem> TRANSFORMER = new XMLTransformer<RSSBookmarkItem>() {
        public RSSBookmarkItem item;

        @Override
        public Optional<RSSBookmarkItem> transform() {
            return Optional.fromNullable(item);
        }

        @Override
        public void visit(XMLElement value, List<String> path) {
            if (value.getName().equals("outline") && value.getAttribs().containsKey("xmlUrl") && item == null) {
                item = new RSSBookmarkItem(
                        value.getAttribs().get("title"),
                        value.getAttribs().get("htmlUrl"),
                        value.getAttribs().get("type"),
                        value.getAttribs().get("xmlUrl"));
            }
        }

        @Override
        public void reset() {
            item = null;
        }

        @Override
        public boolean canTransform() {
            return item != null;
        }
    };
}
