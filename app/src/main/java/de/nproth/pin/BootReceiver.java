package de.nproth.pin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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

            //Log that boot completed was received
            LifecycleWatcher.informBootReceived();

            //First clean up database
            context.getContentResolver().delete(NotesProvider.Notes.NOTES_URI, "text IS NULL", null);

            //Show pins
            Pinboard.get(context).updateAll();
        }
    }
}
