package de.nproth.pin;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import de.nproth.pin.util.Timespan;

public class SnoozeNoteReceiver extends BroadcastReceiver {

    public static final String ACTION_NOTIFICATION_DISMISSED = "de.nproth.pin.ACTION_NOTIFICATION_DISMISSED";

    private static final int NOTES_ITEM = 1;
    private static final int NOTES_LIST = 2;

    private UriMatcher mUris = new UriMatcher(UriMatcher.NO_MATCH);
    {
        mUris.addURI(NotesProvider.AUTHORITIES, "notes/#", NOTES_ITEM);
        mUris.addURI(NotesProvider.AUTHORITIES, "notes", NOTES_LIST);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Uri data = intent.getData();

        if(data == null) {
            Log.e("SnoozeNoteReceiver", "Could not snooze note: uri is null");
            return;
        }

        final long snoozeDuration = PreferenceManager.getDefaultSharedPreferences(context).getLong(NoteActivity.PREFERENCE_SNOOZE_DURATION, NoteActivity.DEFAULT_SNOOZE_DURATION);

        ContentValues cv = new ContentValues();
        long snoozed = System.currentTimeMillis();

        //cv.put(NotesProvider.Notes.SNOOZED, snoozed);
        cv.put(NotesProvider.Notes.MODIFIED, snoozed);

        cv.put(NotesProvider.Notes.WAKE_UP, snoozed + snoozeDuration);

        switch(mUris.match(data)) {
            case NOTES_ITEM:
                //Check if last path segment is really just a valid id (probably redundant)
                String idstring = data.getLastPathSegment();

                if(TextUtils.isEmpty(idstring) || !TextUtils.isDigitsOnly(idstring)) {
                    Log.e("SnoozeNoteReceiver", String.format("Could not snooze note item: invalid id '%s' in uri '%s'", idstring, data.toString()));
                    return;
                }
                //continue to next
            case NOTES_LIST:
                int rows = context.getContentResolver().update(data, cv, "text IS NOT NULL", null);//snooze either on note item or all notes which are not marked as deleted (text is not null)
                Log.d("SnoozeNoteProvider", String.format("Snoozed %d rows", rows));
                if(rows > 0)
                    Pinboard.get(context).updateChanged();

                //show a toast if the user had dismissed this pin to indicate that this note will pop up again after some delay
                Toast.makeText(context, context.getResources().getString(R.string.toast_pin_dismissed, new Timespan(context, snoozeDuration).toString()), Toast.LENGTH_SHORT).show();

                return;
            default:
                Log.e("SnoozeNoteReceiver", String.format("Could not snooze note: invalid uri '%s'", data.toString()));
                return;
        }
    }

}
