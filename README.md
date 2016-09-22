# XMLObjectIterable

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)

<a href="http://www.methodscount.com/?lib=com.github.kgilmer%3AXMLObjectIterable%3A0.5"><img src="https://img.shields.io/badge/Methods and size-core: 75 | deps: 15076 | 11 KB-e91e63.svg"></img></a>

Simplify XML parsing on Java and Android with this abstraction built on top of `XmlPullParser`.

## Overview ##

This library is designed for one use case: transforming XML elements into POJOs as an `Iterable`.  There is no magic; you supply the path to top-level element needed and a type that creates POJO instances based on XML data as passed from an XmlPullParser.  The logic of extracting data from the stream of XML events is neatly encapsulated in a `XMLTransformer<>`.

What's wrong with the XmlPullParser you ask? Well nothing, of course.  But writing parsers that key into the various pull parser events tends to produce messy, unmaintainable code.  XMLObjectIterable is in essence extracting what you'd be doing with those parse events into a discreet interface.

## Features ##

- Space efficient: Creates POJOs as the XML stream is read. Exit without having to read entire stream.
- The transformer can also filter via API to avoid creating unneeded object instances.
- Utilizes the built-in pull parser provided by Android or provide your own in Java.
- Handle complex transformations by holding state in your `XMLTransformer` instances if necessary.
- XML transformers are easily testable in isolation.
- Small API surface area: `XMLObjectIterable.Builder` and `XMLTransformer`.

## Usage Example ##

Let's say you have some information about a library in an XML file:

```xml
<bookstore>
    <book category="COOKING">
        <title lang="en">Everyday Italian</title>
        <author>Giada De Laurentiis</author>
        <year>2005</year>
        <price>30.00</price>
    </book>
    <book category="CHILDREN">
        <title lang="en">Harry Potter</title>
        <author>J K. Rowling</author>
        <year>2005</year>
        <price>29.99</price>
    </book>
    <book category="WEB">
        <title lang="en">XQuery Kick Start</title>
        <author>James McGovern</author>
        <author>Per Bothner</author>
        <author>Kurt Cagle</author>
        <author>James Linn</author>
        <author>Vaidyanathan Nagarajan</author>
        <year>2003</year>
        <price>49.99</price>
    </book>
    <book category="WEB">
        <title lang="en">Learning XML</title>
        <author>Erik T. Ray</author>
        <year>2003</year>
        <price>39.95</price>
    </book>
</bookstore>
```

And it would be <i>great</i> if you were able to just access the books as POJOs.  To do this we can implement an XMLTransformer that creates books from the properties (names, values, and attributes) from the input XML:

```java

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
```

And with this implementation, we can provide an instance and some input XML to XMLObjectIterable, which will provide back an Iterable of the Book POJO:

```java
    final XMLObjectIterable<Book> bookIterable = new XMLObjectIterable.Builder<Book>()
                .onNodes("/bookstore/book")
                .withParser(parser)
                .withTransform(new BookTransformer())
                .from(this.getClass(), "/books.xml")
                .create();
                
    //Consume your Book POJOs with the bookIterable.            
```

And that's about it.  There are unit tests / examples for [RSS](https://github.com/kgilmer/XMLObjectIterable/blob/master/core/src/test/java/com/abk/xmlobjectiterable/transformers/RSSItemUnitTest.java) and Atom feeds as well as donuts.  Yep [donuts](https://github.com/kgilmer/XMLObjectIterable/blob/master/core/src/test/java/com/abk/xmlobjectiterable/transformers/DonutTransformer.java).  Also, have a look at the [XMLTransformer](https://github.com/kgilmer/XMLObjectIterable/blob/master/core/src/main/java/com/abk/xmlobjectiterable/XmlTransformer.java) interface to see what you're getting yourself into.  Finally, there is a very basic Android example [here](https://github.com/kgilmer/XMLObjectIterable/tree/master/AndroidExample).

# Get XMLObjectIterable into your Gradle project

Add it to your build.gradle with:

```gradle
allprojects {
    repositories {
        maven { url "https://jitpack.io" }
    }
}
```

and:

```gradle
dependencies {
    compile 'com.github.kgilmer:XMLObjectIterable:0.9'
}
```

# Release Notes #

## Release 0.9.0 ##

- Major refactoring and simplification of internals.
- Support for complex XML tree parsing.
- Additional tests and examples.
- Cleaned up documentation.

## Release 0.8.0 ##

- Added JMH benchmarking data.
- Extract XML path specification from transformer.
- Allow client to specify Predicate for event parsing.
- Misc cleanup.

## Release 0.7.0 ##

- Migrated core from Android library to pure Java library.
- Removed Android dependencies from core.
- Created 'Android Example' module for Android usage.

# Benchmark Results #

## 0.9.0 ##

```
Benchmark                                     Mode  Cnt    Score    Error  Units
ParsingTransformBenchmarks.testReadRSSItems  thrpt  200  806.297 ± 17.739  ops/s
```

## 0.7.0 ##

```
Benchmark                                     Mode  Cnt    Score    Error  Units
ParsingTransformBenchmarks.testReadRSSItems  thrpt  200  813.907 ± 17.759  ops/s
```
