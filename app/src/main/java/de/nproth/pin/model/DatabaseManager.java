/*
 * Changelog
 *
 * 2019-10-29
 * - Create class DatabaseManager
 */

package de.nproth.pin.model;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import de.nproth.pin.NotesProvider.Notes;

/**
 * Provides an interface to the App's database
 */
public final class DatabaseManager {

    private static final String[] SELECTION_ALL_COLS = { Notes._ID, Notes.TEXT, Notes.CREATED, Notes.MODIFIED, Notes.WAKE_UP };


    private DatabaseManager() {}

    public static Pin getPin(Context context, int id) {
        final Uri uri = Uri.withAppendedPath(Notes.NOTES_URI, Integer.toString(id));
        Pin pin = new Pin();

        Cursor cursor = null;

        try {
            cursor = context.getContentResolver().query(uri, SELECTION_ALL_COLS, null, null, null);

            cursor.moveToFirst();

            pin.setId(cursor.getInt(0));
            pin.setText(cursor.getString(1));
            pin.setCreated(cursor.getLong(2));
            pin.setModified(cursor.getLong(3));
            pin.setWakeUp(cursor.getLong(4));

        } catch(Exception e) {
            pin = null;
        } finally {
            if(cursor != null)
                cursor.close();
        }

        return pin;
    }

    public static boolean insertPin(Context context, Pin pin) {
        long created = System.currentTimeMillis();

        ContentValues cv = new ContentValues();
        cv.put(Notes.TEXT, pin.getText());

        cv.put(Notes.CREATED, created);
        cv.put(Notes.MODIFIED, created);
        cv.put(Notes.WAKE_UP, 0);

        Uri uri = context.getContentResolver().insert(Notes.NOTES_URI, cv);

        int id;
        try {
            id = Integer.parseInt(uri.getLastPathSegment());
        } catch(Exception e) {
            return false;
        }

        pin.setId(id);
        pin.setCreated(created);
        pin.setModified(created);
        pin.setWakeUp(0);
        return true;
    }

    public static boolean updatePin(Context context, Pin pin) {
        final long modified = System.currentTimeMillis();
        final int id = pin.getId();
        final Uri uri = Uri.withAppendedPath(Notes.NOTES_URI, Integer.toString(id));

        ContentValues cv = new ContentValues();
        cv.put(Notes.TEXT, pin.getText());
        cv.put(Notes.MODIFIED, modified);
        cv.put(Notes.WAKE_UP, pin.getWakeUp());

        // Both values are constant and can't be updated. For now, do it anyway.
        cv.put(Notes.CREATED, pin.getCreated());
        cv.put(Notes._ID, id);


        int rows = context.getContentResolver().update(uri, cv, null, null);

        // Should update exactly one row (or 0)
        return rows > 0;
    }

    public static boolean deletePin(Context context, Pin pin) {
        final int id = pin.getId();
        final Uri uri = Uri.withAppendedPath(Notes.NOTES_URI, Integer.toString(id));

        int rows = context.getContentResolver().delete(uri, null, null);

        // Should delete exactly one row (or 0)
        return rows > 0;
    }

    public static int deleteAllPins(Context context) {
        final Uri uri = Notes.NOTES_URI;

        int rows = context.getContentResolver().delete(uri, null, null);
        return rows;
    }


    public static boolean updateAllPinsWakeUp(Context context, long wakeUp) {
        final long modified = System.currentTimeMillis();
        final Uri uri = Notes.NOTES_URI;

        ContentValues cv = new ContentValues();
        cv.put(Notes.MODIFIED, modified);
        cv.put(Notes.WAKE_UP, wakeUp);

        int rows = context.getContentResolver().update(uri, cv, null, null);

        // Should update exactly one row (or 0)
        return rows > 0;
    }


    /**
     *
     * @param context
     * @return
     * A Pin array that contains all pins visible at time <code>now</code> in descending order of creation or null if an error occurs.
     */
    public static Pin[] getVisiblePins(Context context) {
        final long now = System.currentTimeMillis();
        Cursor cursor = null;
        Pin[] pins;

        try {
            cursor = context.getContentResolver().query(Notes.NOTES_URI, SELECTION_ALL_COLS, "text IS NOT NULL AND wake_up <= ?",
                    new String[] { Long.toString(now) }, Notes.CREATED + " DESC");

            cursor.moveToFirst();
            pins = new Pin[cursor.getCount()];

            Pin p;
            for(int i = 0; !cursor.isAfterLast(); i++) {
                p = new Pin();
                p.setId(cursor.getInt(0));
                p.setText(cursor.getString(1));
                p.setCreated(cursor.getLong(2));
                p.setModified(cursor.getLong(3));
                p.setWakeUp(cursor.getLong(4));
                pins[i] = p;
                cursor.moveToNext();
            }

        } finally {
            if(cursor != null)
                cursor.close();
        }

        return pins;
    }

    /**
     *
     * @param context
     * @param after
     * @return
     * Array of all pins with <code>after &lt; wake_up &lt;= now</code>
     */
    public static Pin[] getSnoozedPins(Context context, long after) {
        long now = System.currentTimeMillis();
        Cursor cursor = null;
        Pin[] pins;

        try {
            cursor = context.getContentResolver().query(Notes.NOTES_URI, SELECTION_ALL_COLS, "text IS NOT NULL AND ? < wake_up AND wake_up <= ?",
                    new String[] { Long.toString(after), Long.toString(now) }, Notes.CREATED + " DESC");

            cursor.moveToFirst();
            pins = new Pin[cursor.getCount()];

            Pin p;
            for(int i = 0; !cursor.isAfterLast(); i++) {
                p = new Pin();
                p.setId(cursor.getInt(0));
                p.setText(cursor.getString(1));
                p.setCreated(cursor.getLong(2));
                p.setModified(cursor.getLong(3));
                p.setWakeUp(cursor.getLong(4));
                pins[i] = p;
                cursor.moveToNext();
            }

        } finally {
            if(cursor != null)
                cursor.close();
        }

        return pins;
    }
}
