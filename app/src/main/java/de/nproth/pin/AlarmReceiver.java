package de.nproth.pin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 *  Only used on older devices where JobScheduler is not available
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm triggered, starting NotificationService...");
        Pinboard.get(context).updateChanged();//updated all notifications whose state changed in the meantime
    }
}
