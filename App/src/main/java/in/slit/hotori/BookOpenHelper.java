package in.slit.hotori;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class BookOpenHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "catalog.db";
    public static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "books";

    private static final String CREATE_BOOKS_TABLE_SQL = "CREATE TABLE "
            + TABLE_NAME + "("
            + Book.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + Book.KEY_SERIAL + " INTEGER UNIQUE NOT NULL,"
            + Book.KEY_TITLE + " TEXT,"
            + Book.KEY_NAME + " TEXT,"
            + Book.KEY_MODDATE + " TEXT,"
            + Book.KEY_SIZE + " INTEGER,"
            + Book.KEY_VOLATILE + " INTEGER,"
            + Book.KEY_CONFIDENTIAL + " INTEGER,"
            + Book.KEY_CLASS_NAME + " TEXT,"
            + Book.KEY_CLASS_ID + " TEXT,"
            + Book.KEY_CLASS_CODE + " TEXT,"
            + Book.KEY_SUBJECT + " TEXT,"
            + Book.KEY_CACHED + " INTEGER"
            + ")";
    private static final String DROP_BOOKS_TABLE_SQL = "DROP TABLE IF EXISTS "
            + TABLE_NAME;

    public BookOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_BOOKS_TABLE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_BOOKS_TABLE_SQL);
        onCreate(db);
    }

}