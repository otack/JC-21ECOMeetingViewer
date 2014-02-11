package in.slit.hotori;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StoreCatalogLoader extends AsyncLoader<Boolean> {
    private String mArg;

    public StoreCatalogLoader(Context context, String arg) {
        super(context);
        mArg = arg;
    }

    @Override
    public Boolean loadInBackground() {
        XmlPullParser xmlPullParser = Xml.newPullParser();
        try {
            xmlPullParser.setInput(new StringReader(mArg));
            int eventType;
            eventType = xmlPullParser.getEventType();
            ContentValues values = new ContentValues();
            int count = 1;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG &&
                        xmlPullParser.getName().equals("file")) {
                    values.clear();
                    String serial = xmlPullParser.getAttributeValue(null, "serial");
                    String title = xmlPullParser.getAttributeValue(null, "title");
                    String name = Const.getFileName(Integer.parseInt(serial), title);
                    String path = Const.getFilePath(getContext(), name);
                    values.put(Book.KEY_CACHED, new File(path).exists() ? "true" : "false");
                    values.put(Book.KEY_SERIAL, serial);
                    values.put(Book.KEY_TITLE, title);
                    values.put(Book.KEY_ID, count++);
                    values.put(Book.KEY_NAME, xmlPullParser.getAttributeValue(null, "name"));
                    Date date;
                    String dateValue = xmlPullParser.getAttributeValue(null, "moddate");
                    StringBuilder builder = new StringBuilder(dateValue).delete(23, 29);
                    SimpleDateFormat baseFormat = new SimpleDateFormat("yyyy-MM-dd'T'H:m:s.S");
                    try {
                        date = baseFormat.parse(new String(builder));
                    } catch (ParseException ex) {
                        throw new RuntimeException("a bad date string.");
                    }
                    values.put(Book.KEY_MODDATE, date.getTime());
                    values.put(Book.KEY_SIZE, xmlPullParser.getAttributeValue(null, "size"));
                    values.put(Book.KEY_VOLATILE, xmlPullParser.getAttributeValue(null, "volatile"));
                    values.put(Book.KEY_CONFIDENTIAL, xmlPullParser.getAttributeValue(null, "confidential"));
                    values.put(Book.KEY_CLASS_NAME, xmlPullParser.getAttributeValue(null, "className"));
                    values.put(Book.KEY_CLASS_ID, xmlPullParser.getAttributeValue(null, "classId"));
                    values.put(Book.KEY_CLASS_CODE, xmlPullParser.getAttributeValue(null, "classCode"));
                    values.put(Book.KEY_SUBJECT, xmlPullParser.getAttributeValue(null, "subject"));
                    getContext().getContentResolver().insert(Book.CONTENT_URI, values);
                }
                eventType = xmlPullParser.next();
            }
        } catch (Exception e) {
            Log.e("XmlPullParser", "Error: " + e);
            return false;
        }
        return true;
    }
}

