package in.slit.hotori;

import android.content.Context;

public class Const {
    public static final String BASE_URI = "http://em.jc-21.jp";
    public static final String DEFAULT_LOGIN_URI_APPEND = "/ios/login";
    public static final String DEFAULT_LOGIN_URI = BASE_URI + "/ios/login";
    public static final String DEFAULT_CATALOG_URI = BASE_URI + "/ios/catalog";
    public static final String DEFAULT_DOWNLOAD_URI = BASE_URI + "/ios/document";

    public static final int LOGIN_MODE = 10;
    public static final int LOGIN_MODE_ONLINE = 11;
    public static final int LOGIN_MODE_OFFLINE = 12;

    public static final int LOADER_BINARY = 91;
    public static final int LOADER_RAW = 92;
    public static final int LOADER_LOGIN = 93;
    public static final int LOADER_FETCH_CATALOG = 94;
    public static final int LOADER_STORE_CATALOG = 95;
    public static final int LOADER_CURSOR = 96;

    public static final String BUNDLE_ID = "id";
    public static final String BUNDLE_PASS = "pass";
    public static final String BUNDLE_URI = "uri";
    public static final String BUNDLE_RAW = "raw";
    public static final String BUNDLE_QUERY = "query";
    public static final String BUNDLE_FILENAME = "fileName";
    public static final String BUNDLE_FILEPATH = "filePath";
    public static final String BUNDLE_FILESIZE = "fileSize";

    public static final String PREF_DEFAULT_LOGIN_MODE = "default_login_mode";
    public static final String PREF_LOGIN_URI = "login_url";
    public static final String PREF_ID = "id";
    public static final String PREF_PASS = "pass";
    public static final String PREF_TOKEN = "token";
    public static final String PREF_NAME = "app";

    public static final String NULL = "null";

    public static final String TEMPFILE_NAME_APPEND = ".partial";

//    public static final String DB_NAME = "app.db";
//    public static final String DB_CATALOG_TABLE_NAME = "books";

    public static String getFileName(int serial, String name) {
        StringBuilder fileName = new StringBuilder(String.valueOf(serial))
                .append("_")
                .append(name);
        return new String(fileName);
    }

    public static String getFilePath(Context context, int serial, String name) {
        StringBuilder filePath = new StringBuilder(context.getFilesDir().getAbsolutePath())
                .append("/")
                .append(getFileName(serial, name));
        return new String(filePath);
    }

    public static String getFilePath(Context context, String fileName) {
        StringBuilder filePath = new StringBuilder(context.getFilesDir().getAbsolutePath())
                .append("/")
                .append(fileName);
        return new String(filePath);
    }
}

// http://em.jc-21.jp/ios/catalog?token=3ca13ca9-79c8-4db5-a94f-360ca93d677f
// http://em.jc-21.jp/ios/attachment?token=3ca13ca9-79c8-4db5-a94f-360ca93d677f&file=2172
// http://em.jc-21.jp/ios/document?token=3ca13ca9-79c8-4db5-a94f-360ca93d677f&serial=2270