package de.nproth.pin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import de.nproth.pin.NoteActivity;
import de.nproth.pin.NotesProvider;
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

            //Log that boot completed was received
            LifecycleWatcher.informBootReceived();

            //First clean up database
            context.getContentResolver().delete(NotesProvider.Notes.NOTES_URI, "text IS NULL", null);

            //Show pins
            //On newer Android versions apps are no longer allowed to start services when they are in background
            //context.startService(new Intent(context, PinboardService.class));

            //As a fix invoke Pinboard directly. Nevertheless this is bad style because it circumvents the encapsulation of Pinboard through its service
            Pinboard.get(context).updateAll(true);
        }
    }
}
