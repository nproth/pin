/*
 * Changelog
 *
 * 2019-10-29
 * - Rewrite and move to new model
 */

package de.nproth.pin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import de.nproth.pin.NoteActivity;
import de.nproth.pin.NotesProvider;
import de.nproth.pin.model.NotificationManager;
import de.nproth.pin.pinboard.Pinboard;
import de.nproth.pin.pinboard.PinboardService;
import de.nproth.pin.util.LifecycleWatcher;

/**
 * Should receive boot completed intent (does not work on all devices out of the box on some you have to disable battery optimizations for this app, e.g. on Oneplus 6)
 * The user is prompted for this in NoteActivity
 * Cleans the database and displays all notifications
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "received BOOT_COMPLETED");

            //First clean up database
            //TODO remove as pins will be deleted immediately in the future
            context.getContentResolver().delete(NotesProvider.Notes.NOTES_URI, "text IS NULL", null);

            NotificationManager.showAllPins(context);
        }
    }
}
