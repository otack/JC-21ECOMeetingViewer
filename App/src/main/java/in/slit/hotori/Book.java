package in.slit.hotori;

import android.net.Uri;
import android.provider.BaseColumns;

public final class Book implements BaseColumns {
    public static final String AUTHORITY = "in.slit.hotori.provider.books";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/books");

    public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.in.slit.hotori.books";
    public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.in.slit.hotori.books";

    public static final String KEY_ID = "_id";
    public static final String KEY_SERIAL = "serial";
    public static final String KEY_TITLE = "title";
    public static final String KEY_NAME = "name";
    public static final String KEY_MODDATE = "moddate";
    public static final String KEY_SIZE = "size";
    public static final String KEY_VOLATILE = "volatile";
    public static final String KEY_CONFIDENTIAL = "confidential";
    public static final String KEY_CLASS_NAME = "class_name";
    public static final String KEY_CLASS_ID = "class_id";
    public static final String KEY_CLASS_CODE = "class_code";
    public static final String KEY_SUBJECT = "subject";
    public static final String KEY_CACHED = "cached";
}
