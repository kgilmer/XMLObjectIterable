# XMLObjectIterable

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)

Iterate over POJOs from XML.

## Usage Example ##
```java
XMLObjectIterable<Sample> xitr = new XMLObjectIterable.Builder<Sample>()
  .from(SAMPLE_XML)
  .pathOf("n1/l2/i1")
  .withTransform(new SampleTransformer())
  .withParser(parser)
  .create();
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
    compile 'com.github.kgilmer:XMLObjectIterable:0.3'
}
```
