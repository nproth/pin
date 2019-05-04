package de.nproth.pin.pinboard;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import de.nproth.pin.receiver.AlarmReceiver;
import de.nproth.pin.receiver.DeleteNoteReceiver;
import de.nproth.pin.NoteActivity;
import de.nproth.pin.NotesProvider;
import de.nproth.pin.receiver.NotificationJobService;
import de.nproth.pin.R;
import de.nproth.pin.receiver.SnoozeNoteReceiver;
import de.nproth.pin.util.Timespan;

import static de.nproth.pin.pinboard.PinboardService.PREFERENCE_SNOOZE_DURATION;

/**
 * Updates notifications when pins are added / snoozed / deleted
 */
public final class Pinboard {

    private static Pinboard Me;

    /**
     * Only used prior to version 1.1
    */
    @Deprecated
    private static final String CHANNEL_ID = "de.nproth.pin.notes";

    private static final String PIN_CHANNEL_ID = "de.nproth.pin.pin";
    private static final String WAKE_CHANNEL_ID = "de.nproth.pin.wake_up";

    private static final int SUMMARY_ID = 0;

    private static final String NOTES_GROUP = "de.nproth.pin.NOTES_GROUP";

    private static final int JOB_ID = 23156731;//really just a random number


    private final Context mContext;
    private final NotificationManagerCompat mNotify;
    private final NotificationManager mNManager;
    private final AlarmManager mAlarm;
    private final JobScheduler mScheduler;


    private long mSnoozeDuration = 30 * 60 * 1000;//30min in millis
    private boolean mFixed = false;


    private long mLastChecked = 0;

    private Pinboard(@NonNull Context ctx) {
        mContext = ctx;

        mNManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotify = NotificationManagerCompat.from(mContext);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mScheduler = (JobScheduler) mContext.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            mAlarm = null;
        }
        else {
            mAlarm = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
            mScheduler = null;
        }

        //Create notification Channels, only needed when running on android 8.0+

        //We need two different priorities: we want newly created pins to be pushed silently (anything else is unbearably annoying) but we DO want the user to be notified when a snoozed notification wakes up
        //On pre android 8.0 this is possible by using different priorities but on >=Oreo only one importance per channel is supported => use 2 channels
        //Also I really hope that notifications from two different channels can be grouped and summarized.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Delete old channel used in version <= 1.0.1
            mNManager.deleteNotificationChannel(CHANNEL_ID);
            //create two new channels
            NotificationChannel cpin = new NotificationChannel(PIN_CHANNEL_ID, mContext.getString(R.string.channel_name_pin), NotificationManager.IMPORTANCE_LOW);
            cpin.setShowBadge(false);
            cpin.setDescription(mContext.getString(R.string.channel_description_pin));
            mNManager.createNotificationChannel(cpin);

            NotificationChannel cwake = new NotificationChannel(WAKE_CHANNEL_ID, mContext.getString(R.string.channel_name_wake), NotificationManager.IMPORTANCE_DEFAULT);
            cwake.setShowBadge(false);
            cwake.setDescription(mContext.getString(R.string.channel_description_wake));
            mNManager.createNotificationChannel(cwake);
        }
    }

    private void update(long timeNow, boolean silent, String where, String... wargs) {

        long currentCheck = timeNow;
        long alarmTime = 0;//0 means that no alarm should be set

        Intent idelete = null, isnooze = null, iedit = null, iactivity = new Intent(mContext, NoteActivity.class);
        Cursor db = null;

        try {

            //query all rows that were modified or created since we last checked the db, sorted by time of creation
            db = mContext.getContentResolver().query(NotesProvider.Notes.NOTES_URI, new String[]{NotesProvider.Notes._ID, NotesProvider.Notes.TEXT, NotesProvider.Notes.CREATED, NotesProvider.Notes.MODIFIED, NotesProvider.Notes.WAKE_UP},
                    where, wargs, NotesProvider.Notes.CREATED + " DESC");

            Log.d("NotificationService", String.format("%d notes changed, updating notifications...", db.getCount()));

            db.moveToFirst();

            while (!db.isAfterLast()) {

                if (db.isNull(1)) { //check if notification is marked as deleted (text is NULL) and remove the corresponding notification
                    mNotify.cancel(db.getInt(0));//notifications are indexed by their note's _id values
                } else {

                    //check if note is snoozed
                    long wake_up = db.getLong(4);
                    //TODO if a wake_up event is scheduled here, set wake_up time to 0.
                    if (wake_up > currentCheck) {//keep snoozing
                        mNotify.cancel(db.getInt(0));//notifications are indexed by their note's _id values
                        if(alarmTime == 0 || alarmTime > wake_up)
                            alarmTime = wake_up;//schedule alarm to wake this service the next time a note 'wakes up'

                    } else {//else update and show notification

                        boolean wokeUp = wake_up > mLastChecked;

                        //Prepare Pending Intents to snooze or delete notification or edit the note
                        idelete = new Intent(mContext, DeleteNoteReceiver.class);
                        idelete.setData(Uri.withAppendedPath(NotesProvider.Notes.NOTES_URI, Long.toString(db.getLong(0))));

                        isnooze = new Intent(mContext, SnoozeNoteReceiver.class);
                        isnooze.setData(Uri.withAppendedPath(NotesProvider.Notes.NOTES_URI, Long.toString(db.getLong(0))));

                        iedit = new Intent(mContext, NoteActivity.class);
                        iedit.setData(Uri.withAppendedPath(NotesProvider.Notes.NOTES_URI, Long.toString(db.getLong(0))));

                        if(wokeUp)
                            Log.d("Pinboard", "Pushing notification on wake_up channel...");

                        //create a notification here (previously created notifications that are still visible are just updated)
                        //Newly created notifications are pushed silently. Pins that woke up are published on the second channel and make a noise.
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, (wokeUp && !silent)? WAKE_CHANNEL_ID : PIN_CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_pin_statusbar)
                                .setContentTitle(db.getString(1))//use note's text as notification's headline
                                .setWhen(db.getLong(2))//XXX hope this method accepts UTC timestamps; it seemingly does; show time of creation here utilising the db's 'created' column
                                .setPriority((wokeUp && !silent)? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                                .setOnlyAlertOnce(true)
                                .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                                //Add actions to delete or snooze note, don't use icons here
                                .addAction(0, mContext.getString(R.string.action_delete), PendingIntent.getBroadcast(mContext, 0, idelete, 0))
                                .addAction(0, mContext.getString(R.string.action_snooze, new Timespan(mContext, mSnoozeDuration).toString()), PendingIntent.getBroadcast(mContext, 0, isnooze, 0))
                                .addAction(0, mContext.getString(R.string.action_edit), PendingIntent.getActivity(mContext, 0, iedit, 0))
                                .setContentIntent(PendingIntent.getActivity(mContext, 0, iactivity, 0))//show NoteActivity when user clicks on note.
                                .setCategory(NotificationCompat.CATEGORY_REMINDER);
                        //Make notes persistent
                        if(mFixed)
                            builder.setOngoing(true);

                        isnooze.setAction(SnoozeNoteReceiver.ACTION_NOTIFICATION_DISMISSED);
                        builder.setDeleteIntent(PendingIntent.getBroadcast(mContext, 0, isnooze, 0));//snooze notification when the user dismisses it

                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                            builder.setGroup(NOTES_GROUP);

                        Notification note = builder.build();

                        //And fire new / updated notification
                        mNotify.notify(db.getInt(0), note);//use cursor's _id column as id for our notification
                    }
                }

                db.moveToNext();
            }
        } catch (Exception e) {
            Log.e("NotificationService", "Unable to create Notifications", e);
        } finally {
            if (db != null)
                db.close();
        }

        //schedule an alarm
        if (alarmTime > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//use jobScheduler if possible

                long latency = alarmTime - System.currentTimeMillis();
                JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(mContext, NotificationJobService.class))
                        .setMinimumLatency(latency).build();
                mScheduler.schedule(job);
                Log.d("NotificationService", String.format("Set up job running in ~ %dmin or ~ %ds", latency / 1000 / 60, latency / 1000));

            } else {//Or fallback to Alarm Manager
                mAlarm.set(AlarmManager.RTC_WAKEUP, alarmTime, PendingIntent.getBroadcast(mContext, 0, new Intent(mContext, AlarmReceiver.class), 0));

                long mins = (alarmTime - currentCheck) / 1000 / 60;
                long secs = (alarmTime - currentCheck) / 1000;
                Log.d("NotificationService", String.format("Set up alarm triggering on %d UTC in ~ %dmin or ~ %ds", alarmTime, mins, secs));
            }
        }

        mLastChecked = currentCheck;


        //on pre N devices our notification actions are no longer accessible when they are summarized. Just keep distinct notifications on older devices.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //Now build a summary notification for the group
            //query all visible texts from other notifications
            db = null;
            try {
                db = mContext.getContentResolver().query(NotesProvider.Notes.NOTES_URI, new String[]{NotesProvider.Notes.TEXT, NotesProvider.Notes.CREATED, NotesProvider.Notes.WAKE_UP},
                        "text IS NOT NULL AND wake_up <= ?", new String[]{Long.toString(currentCheck)}, NotesProvider.Notes.CREATED + " DESC");

                int count = db.getCount();

                if (count > 0) {
                    //Create group summary
                    NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                    style.setBigContentTitle(mContext.getResources().getQuantityString(R.plurals.notification_summary_title, count, count));


                    long when = currentCheck;
                    String str;

                    db.moveToFirst();
                    while (!db.isAfterLast()) {//XXX Inbox style only supports up to 5 lines it says in the documentation, but I think more lines do also work
                        str = db.getString(0);
                        style.addLine(str);
                        when = Math.min(when, db.getLong(1));//find oldest reminder
                        db.moveToNext();
                    }

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, PIN_CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_pin_statusbar)
                            .setStyle(style)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setWhen(when)
                            .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                            .setContentIntent(PendingIntent.getActivity(mContext, 0, new Intent(mContext, NoteActivity.class), 0))//show NoteActivity when user clicks on note.
                            .setGroup(NOTES_GROUP)
                            .setGroupSummary(true)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER);

                    if(mFixed)
                        builder.setOngoing(true);

                    mNotify.notify(SUMMARY_ID, builder.build());
                } else
                    mNotify.cancel(SUMMARY_ID);

            } catch (Exception e) {

            } finally {
                if (db != null)
                    db.close();
            }
        }
    }

    public void updateAll() {
        updateAll(false);
    }

    public void updateAll(boolean silent) {
        //query all rows
        update(System.currentTimeMillis(), silent, null);
    }

    public void updateVisible(boolean silent) {
        long now = System.currentTimeMillis();
        String sNow = Long.toString(now);
        //query all rows that are neither snoozed nor deleted and thus visible
        update(now, silent, "text IS NOT NULL AND wake_up <= ?", sNow);
    }

    public void updateVisible() {
        updateVisible(false);
    }

    public void updateChanged() {
        long now = System.currentTimeMillis();
        String sLastCheck = Long.toString(mLastChecked);
        //query all rows that were modified or created since we last checked the db, sorted by time of creation
        update(now, false, "modified >= ? OR wake_up >= ?", sLastCheck, sLastCheck);
        //mLastChecked = now;
    }

    public long getSnoozeDuration() {
        return mSnoozeDuration;
    }

    public void setSnoozeDuration(long dur) {
        mSnoozeDuration = dur;
        updateVisible();
    }

    public boolean getIsFixed() {
        return mFixed;
    }

    public void setIsFixed(boolean fix) {
        mFixed = fix;
        updateVisible();
    }

    /**
     * Saves the snooze duration in shared preferences
     */
    public void destroy() {
        PreferenceManager.getDefaultSharedPreferences(mContext).edit().putLong(PREFERENCE_SNOOZE_DURATION, getSnoozeDuration()).commit();
    }

    public static Pinboard get(Context ctx) {
        if(ctx == null)
            throw new NullPointerException("Cannot acquire instance of singleton 'Pinboard': Context is NULL ");
        if(Me == null)
            Me = new Pinboard(ctx.getApplicationContext());
        Me.setSnoozeDuration(PreferenceManager.getDefaultSharedPreferences(ctx).getLong(PREFERENCE_SNOOZE_DURATION, PinboardService.DEFAULT_SNOOZE_DURATION));
        return Me;
    }
}
