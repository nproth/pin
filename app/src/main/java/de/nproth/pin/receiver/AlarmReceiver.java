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

import de.nproth.pin.model.DatabaseManager;
import de.nproth.pin.model.NotificationManager;
import de.nproth.pin.model.Pin;
import de.nproth.pin.model.SettingsManager;
import de.nproth.pin.pinboard.Pinboard;
import de.nproth.pin.pinboard.PinboardService;

/**
 *  Only used on older devices where JobScheduler is not available
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm triggered, starting NotificationService...");
        //update notifications

        long now = System.currentTimeMillis();
        long lastCheck = SettingsManager.getLastSnoozedCheck(context);

        // Wake up all pins with lastCheck < wake_up < now
        Pin[] pins = DatabaseManager.getSnoozedPins(context, lastCheck);
        NotificationManager.wakeUpPins(context, pins);

        SettingsManager.setLastSnoozedCheck(context, now);
    }
}
