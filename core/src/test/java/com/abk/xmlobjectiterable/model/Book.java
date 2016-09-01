package com.abk.xmlobjectiterable.model;

import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by kgilmer on 8/14/16.
 */
public class Book {

    public enum CATEGORY {
        COOKING, CHILDREN, WEB;
    };

    private final String title;
    private final List<String> author;
    private final int year;
    private final BigDecimal price;
    private final CATEGORY category;

    public Book(String title, List<String> author, int year, BigDecimal price, CATEGORY category) {
        this.title = title;
        this.author = author;
        this.year = year;
        this.price = price;
        this.category = category;
    }

    public CATEGORY getCategory() {
        return category;
    }

    public String getTitle() {
        return title;
    }

    public ImmutableList<String> getAuthor() {
        return ImmutableList.copyOf(author);
    }

    public int getYear() {
        return year;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
