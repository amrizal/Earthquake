package com.example.amrizalzainuddin.earthquake;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.SQLException;
import android.net.Uri;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by amrizal.zainuddin on 14/7/2015.
 */
public class EarthquakeProvider extends ContentProvider {

    public static final Uri CONTENT_URI = Uri.parse("content://com.example.amrizalzainuddin.earthquakeprovider/earthquakes");
    public static final Uri LIVE_FOLDER_URI = Uri.parse("content://com.example.amrizalzainuddin.provider.Earthquake/live_folder");

    //column names
    public static final String KEY_ID = "_id";
    public static final String KEY_DATE = "date";
    public static final String KEY_DETAILS = "details";
    public static final String KEY_SUMMARY = "summary";
    public static final String KEY_LOCATION_LAT = "latitude";
    public static final String KEY_LOCATION_LNG = "longitude";
    public static final String KEY_MAGNITUDE = "magnitude";
    public static final String KEY_LINK = "link";

    EarthquakeDatabaseHelper dbHelper;

    @Override
    public boolean onCreate() {
        Context context = getContext();

        dbHelper = new EarthquakeDatabaseHelper(context,
                EarthquakeDatabaseHelper.DATABASE_NAME, null,
                EarthquakeDatabaseHelper.DATABASE_VERSION);

        return true;
    }

    //create the constant used to differentiate between the different URI
    private static final int QUAKES = 1;
    private static final int QUAKE_ID = 2;
    private static final int SEARCH = 3;
    private static final int LIVE_FOLDER = 4;

    private static final HashMap<String, String> SEARCH_PROJECTION_MAP;
    static {
        SEARCH_PROJECTION_MAP = new HashMap<String, String>();
        SEARCH_PROJECTION_MAP.put(SearchManager.SUGGEST_COLUMN_TEXT_1, KEY_SUMMARY +
        " AS " + SearchManager.SUGGEST_COLUMN_TEXT_1);
        SEARCH_PROJECTION_MAP.put("_id", KEY_ID +
        " AS " + "_id");
    }

    static final HashMap<String, String> LIVE_FOLDER_PROJECTION;
    static {
        LIVE_FOLDER_PROJECTION = new HashMap<String, String>();
        LIVE_FOLDER_PROJECTION.put(LiveFolders._ID, KEY_ID + " AS " + LiveFolders._ID);
        LIVE_FOLDER_PROJECTION.put(LiveFolders.NAME, KEY_DETAILS + " AS " + LiveFolders.NAME);
        LIVE_FOLDER_PROJECTION.put(LiveFolders.DESCRIPTION, KEY_DATE + " AS " + LiveFolders.DESCRIPTION);

    }

    private static final UriMatcher uriMatcher;

    //Allocate the UriMatcher object, where a URI ending in 'earthquakes' will
    //correspond to a request for all earthquakes, and 'earthquakes' with a
    //trailing '/[rowID]' will represent a single earthquake row
    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider", "earthquakes", QUAKES);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider", "earthquakes/#", QUAKE_ID);
        uriMatcher.addURI("com.example.amrizalzainuddin.provider.Earthquake", "live_folder", LIVE_FOLDER);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider",
                SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider",
                SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider",
                SearchManager.SUGGEST_URI_PATH_SHORTCUT, SEARCH);
        uriMatcher.addURI("com.example.amrizalzainuddin.earthquakeprovider",
                SearchManager.SUGGEST_URI_PATH_SHORTCUT + "/*", SEARCH);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE);

        //if this is a row query, limit the result set to the passed in row
        switch (uriMatcher.match(uri)){
            case QUAKE_ID: qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
                break;
            case SEARCH: qb.appendWhere(KEY_SUMMARY + " LIKE \"%" +
                            uri.getPathSegments().get(1) + "%\"");
                        qb.setProjectionMap(SEARCH_PROJECTION_MAP);
                        break;
            case LIVE_FOLDER: qb.setProjectionMap(LIVE_FOLDER_PROJECTION);
                        break;
            default: break;
        }

        //if no sort order is specified, sort by date/time
        String orderBy;
        if(TextUtils.isEmpty(sortOrder)){
            orderBy = KEY_DATE;
        }else{
            orderBy = sortOrder;
        }

        //apply the query to th underlying database
        Cursor c = qb.query(database, projection, selection, selectionArgs, null, null, orderBy);

        //register the contexts ContentResolver to be notified if
        //the cursor result set changes
        c.setNotificationUri(getContext().getContentResolver(), uri);

        //return a cursor to the query result
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (uriMatcher.match(uri)){
            case QUAKES|LIVE_FOLDER: return "vnd.android.cursor.dir/vnd.amrizalzainuddin.earthquake";
            case QUAKE_ID: return "vnd.android.cursor.item/vnd.amrizalzainuddin.earthquake";
            case SEARCH: return SearchManager.SUGGEST_MIME_TYPE;
            default:throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri _uri, ContentValues _values) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        //insert the new row. The call to database.insert will return the row number
        //if it is successful
        long rowID = database.insert(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, "quake", _values);

        //return a URI to the newly inserted row on success
        if(rowID > 0){
            Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
            getContext().getContentResolver().notifyChange(uri, null);
            return uri;
        }

        throw new SQLException("Failed to insert new row into " + _uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int count;
        switch (uriMatcher.match(uri)){
            case QUAKES:
                count = database.delete(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE, where, whereArgs);
                break;
            case QUAKE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.delete(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,
                        KEY_ID + "="
                        + segment
                        + (!TextUtils.isEmpty(where) ? " AND ("
                        + where + ')' : ""), whereArgs);
                break;
            default: throw new IllegalArgumentException("Unsupported URI: " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        int count;
        switch (uriMatcher.match(uri)) {
            case QUAKES:
                count = database.update(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,
                        values, where, whereArgs);
                break;
            case QUAKE_ID:
                String segment = uri.getPathSegments().get(1);
                count = database.update(EarthquakeDatabaseHelper.EARTHQUAKE_TABLE,
                        values, KEY_ID
                                + "=" + segment
                                + (!TextUtils.isEmpty(where) ? " AND ("
                                + where + ')' : ""), whereArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    //helper class for opening, creating, and managing database version control
    private static  class EarthquakeDatabaseHelper extends SQLiteOpenHelper{

        private static final String TAG = "EarthquakeProvider";

        private static final String DATABASE_NAME = "earthquake.db";
        private static final int DATABASE_VERSION = 1;
        private static final String EARTHQUAKE_TABLE = "earthquakes";

        private static final String DATABASE_CREATE =
                "create table " + EARTHQUAKE_TABLE + " ("
                + KEY_ID + " integer primary key autoincrement, "
                + KEY_DATE + " INTEGER, "
                + KEY_DETAILS + " TEXT, "
                + KEY_SUMMARY + " TEXT, "
                + KEY_LOCATION_LAT + " FLOAT, "
                + KEY_LOCATION_LNG + " FLOAT, "
                + KEY_MAGNITUDE + " FLOAT, "
                + KEY_LINK + " TEXT);";

        //the underlying database
        private SQLiteDatabase earthquakeDB;

        public EarthquakeDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(DATABASE_CREATE);

        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");

            db.execSQL("DROP TABLE IF EXISTS " + EARTHQUAKE_TABLE);
            onCreate(db);
        }
    }
}
