package com.abk.xmlobjectiterable.transformers;

import com.abk.xmlobjectiterable.XMLElement;
import com.abk.xmlobjectiterable.XMLTransformer;
import com.abk.xmlobjectiterable.model.Book;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by kgilmer on 8/14/16.
 */
public class BookTransformer implements XMLTransformer<Book> {
    private String category;
    private String title;
    private List<String> authors = new ArrayList<>();
    private String year;
    private String price;

    @Override
    public Optional<Book> transform() {
        if (!canTransform()) {

            return Optional.absent();
        }

        try {
            final Book book = new Book(
                    title,
                    Lists.newArrayList(authors),
                    Integer.parseInt(year),
                    BigDecimal.valueOf(Double.parseDouble(price)),
                    Book.CATEGORY.valueOf(category)
            );

            return Optional.of(book);
        } catch (RuntimeException e) {
            //Log this somewhere.
            return Optional.absent();
        }
    }

    @Override
    public void visit(XMLElement xmlNodeValue, List<String> path) {
        final String name = xmlNodeValue.getName();
        final String value = xmlNodeValue.getValue();
        final Map<String, String> attribs = xmlNodeValue.getAttribs();

        if (name.equals("book")) {
            this.category = attribs.get("category");
        }

        if (name.equals("title")) {
            this.title = value;
        }

        if (name.equals("author")) {
            authors.add(value);
        }

        if (name.equals("year")) {
            this.year = value;
        }

        if (name.equals("price")) {
            this.price = value;
        }
    }

    @Override
    public void reset() {
        this.category = null;
        this.authors.clear();
        this.title = null;
        this.price = null;
        this.year = null;
    }

    @Override
    public boolean canTransform() {
        return !(title == null ||
                year == null ||
                price == null ||
                category == null ||
                authors.isEmpty());
    }
}
