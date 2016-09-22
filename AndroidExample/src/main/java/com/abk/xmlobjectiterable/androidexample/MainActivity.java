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

/**
 * Takes some static XML as a String and generates Iterable of Bird and displays it in the activity.
 */
public class MainActivity extends AppCompatActivity {

    /** Source XML */
    private static final String XML = "<birds><bird>turkey</bird><bird>dove</bird><bird>rooster</bird><somethingelse>turtle</somethingelse></birds>";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView label = (TextView) findViewById(R.id.label);
        if (label == null) {
            return;
        }

        try {
            //Entry-point into the API: produce an Iterable using the XMLTransformer.
            XMLObjectIterable<Bird> birds = new XMLObjectIterable.Builder<Bird>()
                    .from(XML)
                    .withParser(getParser())
                    .withTransform(new BirdTransformer())
                    .onNodes("/birds/bird")
                    .create();

            //Load the Birds into a List
            List<Bird> birdList = new ArrayList<>();
            for (Bird b : birds) {
                birdList.add(b);
            }

            //Update the UI
            label.setText(birdList.toString());

        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }

    }

    /** Get a parser */
    public XmlPullParser getParser() throws XmlPullParserException {
        final XmlPullParser parser = Xml.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        return parser;
    }

    /** Transforms the XML into the Iterable */
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
        public void visit(XMLElement xmlNodeValue, List<String> path) {
            if (xmlNodeValue.getName().equals("bird")) {
                this.name = xmlNodeValue.getValue();
            }
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

    /** Our model class for Bird */
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
