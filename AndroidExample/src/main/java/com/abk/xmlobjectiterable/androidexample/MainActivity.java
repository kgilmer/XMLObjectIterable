package com.abk.xmlobjectiterable.androidexample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Xml;
import android.widget.TextView;
import com.abk.xmlobjectiterable.XMLObjectIterable;
import com.abk.xmlobjectiterable.XMLElement;
import com.abk.xmlobjectiterable.XMLTransformer;
import com.google.common.base.Optional;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String XML = "<a><b>turkey</b><b>dove</b><b>rooster</b><c/></a>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView label = (TextView) findViewById(R.id.label);
        if (label == null) {
            return;
        }

        try {

            XMLObjectIterable<Bird> birds = new XMLObjectIterable.Builder<Bird>()
                    .from(XML)
                    .withParser(getParser())
                    .withTransform(new BirdTransformer())
                    .onNodes("/a/b")
                    .create();

            List<Bird> birdList = new ArrayList<>();
            for (Bird b : birds) {
                birdList.add(b);
            }
            label.setText(birdList.toString());

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

    }

    public XmlPullParser getParser() throws XmlPullParserException {
        final XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        return parser;
    }

    public static class BirdTransformer implements XMLTransformer<Bird> {

        private String name;

        @Override
        public Optional<Bird> transform() {
            if (name != null) {
                return Optional.of(new Bird(name));
            }

            return Optional.absent();
        }

        @Override
        public void visit(XMLElement xmlNodeValue) {
            if (name == null) {
                throw new IllegalStateException("Unexpected duplicate name.");
            }

            this.name = xmlNodeValue.getValue();
        }

        @Override
        public void reset() {
            name = null;
        }

        @Override
        public boolean canTransform() {
            return name != null;
        }
    }

    public static class Bird {
        private final String bird;

        public Bird(final String bird) {
            this.bird = bird;
        }

        public String getName() {
            return bird;
        }

        @Override
        public String toString() {
            return bird;
        }
    }
}
