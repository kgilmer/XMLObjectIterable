# XMLObjectIterable

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)

<a href="http://www.methodscount.com/?lib=com.github.kgilmer%3AXMLObjectIterable%3A0.5"><img src="https://img.shields.io/badge/Methods and size-core: 75 | deps: 15076 | 11 KB-e91e63.svg"></img></a>

Simplify XML parsing on Java and Android with this abstraction built on top of `XmlPullParser`.

## Overview ##

This library is designed for one use case: retrieving lists of XML elements as POJOs in an `Iterable`.  There is no magic; you supply the path to root of the element needed and a type that creates POJO instances based on XML data as passed from an XmlPullParser.  The logic of extracting data from the stream of XML events is neatly encapsulated in a `XmlObjectIterable.Transformer<>`.

## Features ##
- Single file
- Memory efficient: Creates POJOs as the XML stream is read. Exit without having to read entire stream.
- The transformer can also filter via API to avoid parsing unneeded elements.
- Utilizes the built-in pull parser provided by Android.

## Usage Example ##
```java
XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
  .from(SAMPLE_XML)
  .onNodes("n1/l2/i1")
  .withTransform(new SampleTransformer())
  .withParser(parser)
  .create();

  for (Sample sample : xitr) {
    // have fun with your POJO!
  }
```

The work of loading the POJO from node scanning is done in this `SampleTransformer`:
```java
class SampleTransformer implements XMLObjectIterable.Transformer<Sample> {

    private String val;

    @Override
    public Optional<Sample> transform() {
        if (val == null) {
            return Optional.absent();
        }

        return Optional.of(new Sample(val));
    }

    @Override
    public void visit(String name, String value, Map<String, String> attribs) {
        if (!Strings.isNullOrEmpty(value)) {
            val = value;
        }
    }

    @Override
    public void reset() {
        val = null;
    }
}
```

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
    compile 'com.github.kgilmer:XMLObjectIterable:0.7'
}
```

# Release Notes #

## Release 0.8.0 ##

- Added JMH benchmarking data.
- Extract XML path specification from transformer.
- Allow client to specify Predicate for event parsing.
- Misc cleanup.

### Benchmark Results ###

```
Benchmark                                     Mode  Cnt    Score    Error  Units
ParsingTransformBenchmarks.testReadRSSItems  thrpt  200  813.907 ± 17.759  ops/s
```

## Release 0.7.0 ##

- Migrated core from Android library to pure Java library.
- Removed Android dependencies from core.
- Created 'Android Example' module for Android usage.
