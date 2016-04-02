# XMLObjectIterable

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)

<a href="http://www.methodscount.com/?lib=com.github.kgilmer%3AXMLObjectIterable%3A0.5"><img src="https://img.shields.io/badge/Methods and size-core: 75 | deps: 15076 | 11 KB-e91e63.svg"></img></a>

Iterate over POJOs from XML using the XmlPullParser built into Android or supply your own in Java.

## Overview ##

There are a lot of tools and libraries to parse XML data in Java and Android.  This small library is designed for one use case: retrieving lists of XML elements as POJOs in an `Iterable`.  There is no magic; you supply the path to root of the element needed and a type that creates POJO instances based on XML data as passed from an XmlPullParser.

## Usage Example ##
```java
XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
  .from(SAMPLE_XML)
  .pathOf("n1/l2/i1")
  .withTransform(new SampleTransformer())
  .withParser(parser)
  .create();

  for (Sample sample : xitr) {
    // have fun with your POJO!
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
    compile 'com.github.kgilmer:XMLObjectIterable:0.5'
}
```
