package benchmarks;


import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.abk.xmlobjectiterable.transformers.RSSBookmarkItem;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@State(Scope.Benchmark)
public class ParsingTransformBenchmarks {


    @Benchmark
    public void testReadRSSItems() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(false);
        XmlPullParser parser = factory.newPullParser();

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("rss-opml.xml");

        assertNotNull("InputStream cannot be null.", inputStream);

        XMLObjectIterable<RSSBookmarkItem> xitr = new XMLObjectIterable.Builder<RSSBookmarkItem>()
                .from(inputStream)
                .withTransform(RSSBookmarkItem.TRANSFORMER)
                .withParser(parser)
                .onNodes(RSSBookmarkItem.PATH)
                .create();

        List<RSSBookmarkItem> samples = Lists.newArrayList(xitr);

        assertTrue("Contains elements.", !samples.isEmpty());
    }
}