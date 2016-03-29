# XMLObjectIterable

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)

Iterate over POJOs from XML using the XmlPullParser built into Android or supply your own in Java.

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
