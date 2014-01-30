package in.slit.hotori;

import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

public class LoginLoader extends AsyncLoader<String> {
    private String mArg;

    public LoginLoader(Context context, String arg) {
        super(context);
        mArg = arg;
    }

    @Override
    public String loadInBackground() {
        String result = null;
        String value = null;
        try {
            XmlPullParser xmlPullParser = Xml.newPullParser();
            xmlPullParser.setInput(new StringReader(mArg));
            int eventType;
            eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                        xmlPullParser.getName().equals("result")) {
                    result = xmlPullParser.nextText();
                } else if (eventType == XmlPullParser.START_TAG &&
                        xmlPullParser.getName().equals("value")) {
                    value = xmlPullParser.nextText();
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            Log.d("XmlPullParser", "Error");
        }

        if (result.equals("ok")) {
            return value;
        }
        return null;
    }
}
