/*
 * Changelog
 *
 * 2019-10-29
 * - Create class NotificationManager
 */


package de.nproth.pin.model;

import android.app.PendingIntent;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Locale;

import de.nproth.pin.receiver.AlarmReceiver;
import de.nproth.pin.receiver.NotificationJobService;

/**
 * Handles alarms for pins
 */
public final class AlarmManager {

    private static final int JOB_ID = 23156731;//really just a random number

    private AlarmManager() {}


    public static void setAlarm(Context context, long when) {
        //schedule an alarm
        final long now = System.currentTimeMillis();
        final long latency;

        if (when > now)
            latency = when - now;
        else
            latency = 1;//make sure latency is positive


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//use jobScheduler if possible
            JobScheduler sched = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

            JobInfo job = new JobInfo.Builder(JOB_ID, new ComponentName(context, NotificationJobService.class))
                    .setMinimumLatency(latency).build();

            sched.schedule(job);

            Log.d("NotificationService", String.format(Locale.ENGLISH, "Set up job running in ~ %dmin or ~ %ds", latency / 1000 / 60, latency / 1000));

        } else {//Or fallback to Alarm Manager
            android.app.AlarmManager amgr = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            // works also if when is in the past
            amgr.set(android.app.AlarmManager.RTC_WAKEUP, when, PendingIntent.getBroadcast(context, 0, new Intent(context, AlarmReceiver.class), 0));

            long mins = (when - now) / 1000 / 60;
            long secs = (when - now) / 1000;
            Log.d("NotificationService", String.format(Locale.ENGLISH, "Set up alarm triggering on %d UTC in ~ %dmin or ~ %ds", when, mins, secs));
        }
    }
}
