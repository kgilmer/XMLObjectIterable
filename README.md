# XMLObjectIterable

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

```gradle
   repositories { 
        jcenter()
        maven { url "https://jitpack.io" }
   }
   dependencies {
         compile 'com.github.kgilmer:XMLObjectIterable:0.2'
   }
   ```

[![Release](https://jitpack.io/v/kgilmer/XMLObjectIterable.svg)](https://jitpack.io/#kggilmer/XMLObjectIterable)
