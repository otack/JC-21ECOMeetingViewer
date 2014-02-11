package in.slit.hotori;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class BookProvider extends ContentProvider {
    private static final int BOOKS = 1;
    private static final int BOOKS_ID = 2;

    private static final UriMatcher URI_MATCHER;

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI(Book.AUTHORITY, "books", BOOKS);
        URI_MATCHER.addURI(Book.AUTHORITY, "books/#", BOOKS_ID);
    }
    private BookOpenHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new BookOpenHelper(getContext());
        return true;
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        if (URI_MATCHER.match(uri) != BOOKS) {
            throw new IllegalArgumentException("Unknown URI *** " + uri);
        }

        ContentValues values;
        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        long rowID = db.replace(BookOpenHelper.TABLE_NAME, "NULL", values);
        if (rowID > 0) {
            Uri newUri = ContentUris.withAppendedId(
                    Book.CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }
        throw new SQLException("Failed to insert row into " + uri);

    }

    @Override
    public int delete(Uri uri, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                count = db.delete(BookOpenHelper.TABLE_NAME, " " +
                        Book.KEY_ID + " like '%'", null);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int count;
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                count = db.update(BookOpenHelper.TABLE_NAME, values, where, whereArgs);
                break;

            case BOOKS_ID:
                String id = uri.getPathSegments().get(1);
                count = db.update(BookOpenHelper.TABLE_NAME, values,
                        Book.KEY_ID + "=" + id
                                + (!TextUtils.isEmpty(where) ? " AND (" + where + ')'
                                : ""), whereArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(BookOpenHelper.TABLE_NAME);

        Log.d("query", uri.toString());
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                break;
            case BOOKS_ID:
                qb.appendWhere(Book.KEY_ID + "="
                        + uri.getPathSegments().get(1));
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        String limit = uri.getQueryParameter("limit");
        if (uri.getQueryParameter("distinct") != null) {
            qb.setDistinct(true);
        }
        if (TextUtils.isEmpty(sortOrder)) {
            sortOrder = Book.KEY_ID + " DESC";
        }

        SQLiteDatabase db = mHelper.getReadableDatabase();
        Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder, limit);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case BOOKS:
                return Book.CONTENT_TYPE;
            case BOOKS_ID:
                return Book.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }
}