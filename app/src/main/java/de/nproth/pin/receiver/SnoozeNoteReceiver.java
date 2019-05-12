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
import android.widget.Toast;

import de.nproth.pin.NotesProvider;
import de.nproth.pin.R;
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

        Uri data = intent.getData();

        if(data == null) {
            Log.e("SnoozeNoteReceiver", "Could not snooze note: uri is null");
            return;
        }

        Intent i = new Intent(context, PinboardService.class);
        i.setData(data);
        i.setAction(PinboardService.INTENT_ACTION_SNOOZE_PIN);

        //context.startService(i);
        Pinboard pin = Pinboard.get(context).setSnoozeDuration(PreferenceManager.getDefaultSharedPreferences(context).getLong(PREFERENCE_SNOOZE_DURATION, PinboardService.DEFAULT_SNOOZE_DURATION));
        onSnoozePins(context, pin.getSnoozeDuration(), data);
        pin.updateAll(true);
    }

    /**
     * Called from {@link PinboardService} when this receiver's onReceive method was called and passed an appropriate intent.
     * This cannot be called directly as the snoozeDuration setting is needed which is held by the service and can only be obtained through binding.
     * Binding a service is not possible in a BroadcastReceiver so just start the service and let it take care of the work (and call this method)
     * @return number of updated pins
     */
    public static int onSnoozePins(Context context, long snoozeDuration, Uri data) {
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
                    return 0;
                }
                //continue to next
            case NOTES_LIST:
                int rows = context.getContentResolver().update(data, cv, "text IS NOT NULL", null);//snooze either on note item or all notes which are not marked as deleted (text is not null)
                Log.d("SnoozeNoteProvider", String.format("Snoozed %d rows", rows));

                //show a toast if the user had dismissed this pin to indicate that this note will pop up again after some delay
                Toast.makeText(context, context.getResources().getString(R.string.toast_pin_dismissed, new Timespan(context, snoozeDuration).toString()), Toast.LENGTH_SHORT).show();

                return rows;
            default:
                Log.e("SnoozeNoteReceiver", String.format("Could not snooze note: invalid uri '%s'", data.toString()));
                return 0;
        }
    }

}
