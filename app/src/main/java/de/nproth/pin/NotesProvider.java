package de.nproth.pin;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;

/**
 * Content Provider of this app, stores pins in database
 */
public class NotesProvider extends ContentProvider {

    public static final String AUTHORITIES = BuildConfig.APPLICATION_ID + ".notes";
    public static final String CONTENT_URI = "content://" + AUTHORITIES;

    public static final class Notes implements BaseColumns {
        public static final Uri NOTES_URI = Uri.parse(CONTENT_URI + "/notes");

        //public static final String _ID = "_id"; in BaseColumns
        public static final String TEXT = "text";//TEXT note's content. If a notes text is set to NULL the note is marked as deleted and will be removed from the database on next boot
        public static final String CREATED = "created";//INT time of note's creation as UTC timestamp
        /**
         * CAUTION: DO NOT USE! Not present in database (since DB_VERSION 3) after a fresh install
         */
        @Deprecated
        public static final String SNOOZED = "snoozed";//INT last time the note was 'snoozed' as UTC timestamp

        public static final String WAKE_UP = "wake_up";//INT time a snoozed note should wake up as UTC timestamp
        public static final String MODIFIED = "modified";//INT last time the note was modified as UTC timestamp, e.g. it's text changed, it was snoozed or deleted
    }

    private final int NOTES_LIST = 1;
    private final int NOTES_ITEM = 2;

    /** CHANGELOG:
     * 1 - initial db
     * 2 - add column 'wake_up'
     * 3 - remove column 'snoozed'
     */
    private final int DB_VERSION = 3;

    private UriMatcher mUris = new UriMatcher(UriMatcher.NO_MATCH);
    {
        mUris.addURI(AUTHORITIES, "notes", NOTES_LIST);
        mUris.addURI(AUTHORITIES, "notes/#", NOTES_ITEM);
    }

    private SQLiteOpenHelper mDbHelper;

    public NotesProvider() {
    }

    @Override
    public boolean onCreate() {
        mDbHelper = new SQLiteOpenHelper(getContext(), "notes.db", null, DB_VERSION) {
            @Override
            public void onCreate(SQLiteDatabase db) {
                db.beginTransaction();
                try {
                    db.execSQL("CREATE TABLE notes (_id INTEGER PRIMARY KEY AUTOINCREMENT, text TEXT, created INT NOT NULL, wake_up INT NOT NULL DEFAULT 0, modified INT NOT NULL DEFAULT 0);");//NULL text means that the note was deleted
                    db.setTransactionSuccessful();
                } catch(SQLException sql) {
                    Log.e("Notesprovider", "Could not create database", sql);
                }
                finally {
                    db.endTransaction();
                }

                Log.d("NotesProvider", "Database 'notes.db' created");
            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
                db.beginTransaction();
                try {
                    switch(oldVersion) {
                        case 1:
                            db.execSQL("ALTER TABLE notes ADD COLUMN wake_up INT NOT NULL DEFAULT 0");
                        case 2:
                            //ALTER TABLE notes DELETE COLUMN snoozed; - not supported on android so just reset this column and stop using it. Not present after a fresh install
                            ContentValues cv = new ContentValues();
                            cv.put("snoozed", 0);
                            db.update("notes", cv, null, null);//set 'snoozed' to default value in all rows
                        default:
                    }
                    //do upgrade here
                    db.setTransactionSuccessful();
                } catch(SQLException sql) {
                    Log.e("NotesProvider", "Could not upgrade database", sql);
                }
                finally {
                    db.endTransaction();
                }

                Log.d("NotesProvider", "Database 'notes.db' upgraded");
            }
        };
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {

        switch(mUris.match(uri)) {
            default:
                Log.e("NotesProvider", String.format("Could not delete rows from table 'notes': invalid uri '%s'", uri.toString()));
                return 0;

            case NOTES_ITEM:
                String id = uri.getLastPathSegment();
                // check if id is really just a number else SQL injection attacks might be possible
                if(!TextUtils.isDigitsOnly(id)) {
                    Log.e("NoteProvider", String.format("Invalid id in uri for delete op: '%s' in '%s' is not a numeric id", id, uri.toString()));
                    return 0;
                }
                if(!TextUtils.isEmpty(selection))
                    selection = "(" + selection + ") AND _id = " + id;
                else
                    selection = "_id = " + id;
                //continue to next case
            case NOTES_LIST:
                try {
                    int cols = mDbHelper.getReadableDatabase().delete("notes", selection, selectionArgs);

                    getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/notes"), null);
                    return cols;
                } catch(SQLException sql) {
                    Log.e("NoteProvider", "Could not delete rows from table 'notes'", sql);
                    return 0;
                }
        }
    }

    @Override
    public String getType(Uri uri) {
        switch(mUris.match(uri)) {
            case NOTES_ITEM: return "vnd.android.cursor.item/vnd.nproth.notes";
            case NOTES_LIST: return "vnd.android.cursor.dir/vnd.nproth.notes";
            default:
                Log.e("NotesProvider", String.format("Unknown type for Uri '%s'",uri.toString()));
                return null;
        }
    }

    @Override
    public synchronized Uri insert(Uri uri, ContentValues values) {
        switch(mUris.match(uri)) {

            case NOTES_LIST:
                try {
                    long id = mDbHelper.getWritableDatabase().insertOrThrow("notes", null, values);
                    Uri item = Uri.parse(CONTENT_URI + "/notes/" + Long.toString(id));
                    getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/notes"), null);
                    return item;

                } catch(SQLException sql) {
                    Log.e("NotesProvider", "Could not insert new entry into table 'notes'", sql);
                    return null;
                }

            case NOTES_ITEM://invalid
            default:
                Log.e("NotesProvider", String.format("Invalid uri '%s': Could not insert new entry into table 'notes'", uri.toString()));
                return null;
        }
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        if(TextUtils.isEmpty(sortOrder))
            sortOrder = "_id ASC";

        switch(mUris.match(uri)) {
            default:
                Log.e("NotesProvider", String.format("Could not query table 'notes': invalid uri '%s'", uri.toString()));
                return null;

            case NOTES_ITEM:
                String id = uri.getLastPathSegment();
                // check if id is really just a number else SQL injection attacks might be possible
                if(!TextUtils.isDigitsOnly(id)) {
                    Log.e("Noteprovider", String.format("Invalid id in uri for query: '%s' in '%s' is not a numeric id", id, uri.toString()));
                    return null;
                }
                if(!TextUtils.isEmpty(selection))
                    selection = "(" + selection + ") AND _id = " + id;
                else
                    selection = "_id = " + id;
                //continue to next case
            case NOTES_LIST:
                try {
                    Cursor c = mDbHelper.getReadableDatabase().query("notes", projection, selection, selectionArgs, null, null, sortOrder);
                    if(c == null)
                        Log.e("NoteProvider", String.format("Cursor for query '%s' on table 'notes' is null", selection));
                    return c;
                } catch(SQLException sql) {
                    Log.e("NoteProvider", "Could not query table 'notes'", sql);
                    return null;
                }
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        String id = null;

        switch(mUris.match(uri)) {
            default:
                Log.e("NotesProvider", String.format("Could not update table 'notes': invalid uri '%s'", uri.toString()));
                return 0;

            case NOTES_ITEM:
                id = uri.getLastPathSegment();
                // check if id is really just a number else SQL injection attacks might be possible
                if(!TextUtils.isDigitsOnly(id)) {
                    Log.e("NotesProvider", String.format("Invalid id in uri for update: '%s' in '%s' is not a numeric id", id, uri.toString()));
                    return 0;
                }
                if(!TextUtils.isEmpty(selection))
                    selection = "(" + selection + ") AND _id = " + id;
                else
                    selection = "_id = " + id;
                //continue to next case
            case NOTES_LIST:
                try {
                    int cols = mDbHelper.getReadableDatabase().update("notes", values, selection, selectionArgs);

                    if(cols > 0)
                        if(id == null)
                            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/notes"), null);
                        else
                            getContext().getContentResolver().notifyChange(Uri.parse(CONTENT_URI + "/notes/" + id), null);//We know that only this column was updated

                    return cols;
                } catch(SQLException sql) {
                    Log.e("NotesProvider", "Could not update table 'notes'", sql);
                    return 0;
                }
        }
    }


}
