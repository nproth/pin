package de.nproth.pin.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.nproth.pin.pinboard.PinboardService;

/**
 *  Only used on older devices where JobScheduler is not available
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("AlarmReceiver", "Alarm triggered, starting NotificationService...");
        //update notifications
        Intent i = new Intent(context, PinboardService.class);
        i.setAction(PinboardService.INTENT_ACTION_WAKE_UP);
        context.startService(i);
    }
}
