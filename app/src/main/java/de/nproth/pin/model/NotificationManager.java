/*
 * Changelog
 *
 * 2019-10-29
 * - Create class NotificationManager
 */

package de.nproth.pin.model;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import de.nproth.pin.NoteActivity;
import de.nproth.pin.NotesProvider;
import de.nproth.pin.NotesProvider.Notes;
import de.nproth.pin.R;
import de.nproth.pin.receiver.DeleteNoteReceiver;
import de.nproth.pin.receiver.SnoozeNoteReceiver;
import de.nproth.pin.util.Timespan;

/**
 * Provides an high level interface to pins in the systems notification area
 */
public final class NotificationManager {

    /**
     * Only used prior to version 1.1
     */
    @Deprecated
    private static final String CHANNEL_ID = "de.nproth.pin.notes";

    private static final String PIN_CHANNEL_ID = "de.nproth.pin.pin";
    private static final String WAKE_CHANNEL_ID = "de.nproth.pin.wake_up";

    private static final int SUMMARY_ID = 0;

    private static final String NOTES_GROUP = "de.nproth.pin.NOTES_GROUP";


    private NotificationManager() {}

    /**
     * Creates notification for this pin and notifies user about it.
     * @param context
     * @param pin
     */
    public static void wakeUpPin(Context context, Pin pin) {
        initChannels(context);
        updateNotification(context, pin, true);
        updateGroupSummary(context, DatabaseManager.getVisiblePins(context));
    }


    /**
     * Show or update a pin notification. If the pin was previously snoozed, use {@link NotificationManager#wakeUpPin(Context, Pin)}
     * instead to ensure that the user will be notified.
     * @param context
     * @param pin
     */
    public static void showPin(Context context, Pin pin) {
        initChannels(context);
        updateNotification(context, pin, false);
        updateGroupSummary(context, DatabaseManager.getVisiblePins(context));
    }


    public static void hidePin(Context context, Pin pin) {
        int id = pin.getId();
        if(id == 0) // The group summary has id 0 and should not be touched. This is also probably the result of a mistake.
            return;

        NotificationManagerCompat nmgr = NotificationManagerCompat.from(context);
        nmgr.cancel(id);
    }


    public static void wakeUpPins(Context context, Pin[] pins) {
        initChannels(context);

        for(Pin p : pins)
            updateNotification(context, p, true);

        updateGroupSummary(context, DatabaseManager.getVisiblePins(context));
    }

    public static void showAllPins(Context context) {
        initChannels(context);

        Pin[] pins = DatabaseManager.getVisiblePins(context);
        for(Pin p : pins)
            updateNotification(context, p, false);

        updateGroupSummary(context, pins);
    }


    public static void hideAllPins(Context context) {
        NotificationManagerCompat nmgr = NotificationManagerCompat.from(context);
        nmgr.cancelAll();
    }


    /**
     * Initializes notification channels, necessary on Android 8.0+.
     * @param context
     */
    private static void initChannels(Context context) {
        android.app.NotificationManager nmgr = (android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        //Create notification Channels, only needed when running on android 8.0+

        //We need two different priorities: we want newly created pins to be pushed silently (anything else is unbearably annoying) but we DO want the user to be notified when a snoozed notification wakes up
        //On pre android 8.0 this is possible by using different priorities but on >=Oreo only one importance per channel is supported => use 2 channels
        //Also I really hope that notifications from two different channels can be grouped and summarized.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Delete old channel used in version <= 1.0.1
            nmgr.deleteNotificationChannel(CHANNEL_ID);
            //create two new channels
            NotificationChannel cpin = new NotificationChannel(PIN_CHANNEL_ID, context.getString(R.string.channel_name_pin), android.app.NotificationManager.IMPORTANCE_LOW);
            cpin.setShowBadge(false);
            cpin.setDescription(context.getString(R.string.channel_description_pin));

            nmgr.createNotificationChannel(cpin);

            NotificationChannel cwake = new NotificationChannel(WAKE_CHANNEL_ID, context.getString(R.string.channel_name_wake), android.app.NotificationManager.IMPORTANCE_DEFAULT);
            cwake.setShowBadge(false);
            cwake.setDescription(context.getString(R.string.channel_description_wake));

            nmgr.createNotificationChannel(cwake);
        }
    }


    private static void updateNotification(Context context, Pin pin, boolean wakeup) {
        final int id = pin.getId();
        final Uri data = Uri.withAppendedPath(NotesProvider.Notes.NOTES_URI, Long.toString(id));
        final long snoozedur = SettingsManager.getSnoozeDuration(context);

        //TODO
        Intent idelete = null, isnooze = null, iedit = null, iactivity = new Intent(context, NoteActivity.class);

        idelete = new Intent(context, DeleteNoteReceiver.class);
        idelete.setData(data);

        isnooze = new Intent(context, SnoozeNoteReceiver.class);
        isnooze.setData(data);

        iedit = new Intent(context, NoteActivity.class);
        iedit.setData(data);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, wakeup? WAKE_CHANNEL_ID : PIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_pin_statusbar)
                .setContentTitle(pin.getText())
                .setWhen(pin.getCreated())
                .setPriority(wakeup? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setOnlyAlertOnce(true)
                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                //Add actions to delete or snooze note, don't use icons here
                .addAction(0, context.getString(R.string.action_delete), PendingIntent.getBroadcast(context, 0, idelete, 0))
                .addAction(0, context.getString(R.string.action_snooze, new Timespan(context, snoozedur).toString()), PendingIntent.getBroadcast(context, 0, isnooze, 0))
                .addAction(0, context.getString(R.string.action_edit), PendingIntent.getActivity(context, 0, iedit, 0))
                .setContentIntent(PendingIntent.getActivity(context, 0, iactivity, 0))//show NoteActivity when user clicks on note.
                .setCategory(NotificationCompat.CATEGORY_REMINDER);

        //Make notes persistent
        if(SettingsManager.getPinsFixed(context))
            builder.setOngoing(true);

        isnooze.setAction(SnoozeNoteReceiver.ACTION_NOTIFICATION_DISMISSED);
        builder.setDeleteIntent(PendingIntent.getBroadcast(context, 0, isnooze, 0));//snooze notification when the user dismisses it

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            builder.setGroup(NOTES_GROUP);

        Notification note = builder.build();

        ((android.app.NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, note);//use cursor's _id column as id for our notification
    }


    private static void updateGroupSummary(Context context, Pin[] pins) {

        NotificationManagerCompat nmgr = NotificationManagerCompat.from(context);

        //on pre N devices our notification actions are no longer accessible when they are summarized. Just keep distinct notifications on older devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            int count = pins.length;

            if (count > 0) {

                long oldest = pins[count - 1].getCreated();

                //Create group summary
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                style.setBigContentTitle(context.getResources().getQuantityString(R.plurals.notification_summary_title, count, count));

                for(int i = 0; i < count; i++) // XXX Inbox style only supports up to 5 lines it says in the documentation, but I think more lines do also work
                    style.addLine(pins[i].getText());

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, PIN_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_pin_statusbar)
                        .setStyle(style)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setWhen(oldest)
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                        .setContentIntent(PendingIntent.getActivity(context, 0, new Intent(context, NoteActivity.class), 0))//show NoteActivity when user clicks on note.
                        .setGroup(NOTES_GROUP)
                        .setGroupSummary(true)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER);

                if (SettingsManager.getPinsFixed(context))
                    builder.setOngoing(true);

                nmgr.notify(SUMMARY_ID, builder.build());
            } else
                nmgr.cancel(SUMMARY_ID);
        }
    }
}
