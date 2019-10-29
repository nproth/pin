/*
 * Changelog
 *
 * 2019-10-29
 * - Rewrite and move to new model
 */

package de.nproth.pin.receiver;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import de.nproth.pin.NotesProvider;
import de.nproth.pin.model.DatabaseManager;
import de.nproth.pin.model.NotificationManager;
import de.nproth.pin.model.Pin;
import de.nproth.pin.pinboard.Pinboard;
import de.nproth.pin.pinboard.PinboardService;

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

        switch(mUris.match(data)) {
            case NOTES_ITEM:
                //Check if last path segment is really just a valid id (probably redundant)
                String idstring = data.getLastPathSegment();

                if(TextUtils.isEmpty(idstring) || !TextUtils.isDigitsOnly(idstring)) {
                    Log.e("DeleteNoteReceiver", String.format("Could not delete note item: invalid id '%s' in uri '%s'", idstring, data.toString()));
                    return;
                }

                int id = Integer.parseInt(idstring);

                Pin pin = DatabaseManager.getPin(context, id);
                NotificationManager.hidePin(context, pin);
                DatabaseManager.deletePin(context, pin);
                break;

            case NOTES_LIST:
                NotificationManager.hideAllPins(context);
                DatabaseManager.deleteAllPins(context);
                return;

            default:
                Log.e("DeleteNoteReceiver", String.format("Could not snooze note: invalid uri '%s'", data.toString()));
                return;
        }
    }

}
