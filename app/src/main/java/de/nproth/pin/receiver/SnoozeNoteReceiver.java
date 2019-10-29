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
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import de.nproth.pin.NotesProvider;
import de.nproth.pin.R;
import de.nproth.pin.model.DatabaseManager;
import de.nproth.pin.model.Pin;
import de.nproth.pin.model.SettingsManager;
import de.nproth.pin.pinboard.Pinboard;
import de.nproth.pin.pinboard.PinboardService;
import de.nproth.pin.util.Timespan;

import static de.nproth.pin.pinboard.PinboardService.PREFERENCE_SNOOZE_DURATION;

public class SnoozeNoteReceiver extends BroadcastReceiver {

    public static final String ACTION_NOTIFICATION_DISMISSED = "de.nproth.pin.ACTION_NOTIFICATION_DISMISSED";

    private static final int NOTES_ITEM = 1;
    private static final int NOTES_LIST = 2;

    private static final UriMatcher mUris = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        mUris.addURI(NotesProvider.AUTHORITIES, "notes/#", NOTES_ITEM);
        mUris.addURI(NotesProvider.AUTHORITIES, "notes", NOTES_LIST);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final long now = System.currentTimeMillis();
        final Uri data = intent.getData();

        switch(mUris.match(data)) {
            case NOTES_ITEM:
                //Check if last path segment is really just a valid id (probably redundant)
                String idstring = data.getLastPathSegment();

                if(TextUtils.isEmpty(idstring) || !TextUtils.isDigitsOnly(idstring)) {
                    Log.e("SnoozeNoteReceiver", String.format(Locale.ENGLISH, "Could not snooze note item: invalid id '%s' in uri '%s'", idstring, data.toString()));
                    return;
                }

                int id = Integer.parseInt(idstring);

                Pin pin = DatabaseManager.getPin(context, id);
                pin.snooze(context);
                break;

            case NOTES_LIST:

                long dur = SettingsManager.getSnoozeDuration(context);
                Pin.snoozeAllPins(context, dur);
                //show a toast if the user had dismissed this pin to indicate that this note will pop up again after some delay
                Toast.makeText(context, context.getResources().getString(R.string.toast_pin_dismissed, new Timespan(context, dur).toString()), Toast.LENGTH_SHORT).show();

                break;
            default:
                Log.e("SnoozeNoteReceiver", String.format(Locale.ENGLISH, "Could not snooze note: invalid uri '%s'", data.toString()));
        }
    }

}
