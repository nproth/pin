package de.nproth.pin;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/**
 * Called from notification action button.
 * Marks pin as deleted and makes it disappear.
 */
public class DeleteNoteReceiver extends BroadcastReceiver {

    private final int NOTES_ITEM = 1;
    private final int NOTES_LIST = 2;

    private UriMatcher mUris = new UriMatcher(UriMatcher.NO_MATCH);
    {
        mUris.addURI(NotesProvider.AUTHORITIES, "notes/#", NOTES_ITEM);
        mUris.addURI(NotesProvider.AUTHORITIES, "notes", NOTES_LIST);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Uri data = intent.getData();

        if(data == null) {
            Log.e("DeleteNoteReceiver", "Could not delete note: uri is null");
            return;
        }

        /*
         * For some reason I forgot the pins must not be deleted from the database right away.
         * I think it had something to do with the _id value in the database which is needed to reference visible notifications.
         * So we still need it in order to hide the deleted pins.
         *
         * The deleted pins are cleaned up on every boot (if the system allows us to catch the boot completed intent. For more information see BootReceiver)
         */

        ContentValues cv = new ContentValues();

        cv.putNull(NotesProvider.Notes.TEXT);//setting text to NULL marks note as deleted, it will be removed at next boot
        cv.put(NotesProvider.Notes.MODIFIED, System.currentTimeMillis());

        switch(mUris.match(data)) {
            case NOTES_ITEM:
                //Check if last path segment is really just a valid id (probably redundant)
                String idstring = data.getLastPathSegment();

                if(TextUtils.isEmpty(idstring) || !TextUtils.isDigitsOnly(idstring)) {
                    Log.e("DeleteNoteReceiver", String.format("Could not delete note item: invalid id '%s' in uri '%s'", idstring, data.toString()));
                    return;
                }
                //continue to next
            case NOTES_LIST:
                int rows = context.getContentResolver().update(data, cv, "text IS NOT NULL", null);//delete either on note item or all notes which are not marked as deleted already (text is not null)
                Log.d("DeleteNoteProvider", String.format("Deleted %d rows", rows));
                if(rows > 0)
                    Pinboard.get(context).updateChanged();
                return;
            default:
                Log.e("DeleteNoteReceiver", String.format("Could not snooze note: invalid uri '%s'", data.toString()));
                return;
        }
    }

}
