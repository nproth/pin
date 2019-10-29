/*
 * Changelog
 *
 * 2019-10-29
 * - Rewrite and move to new model
 */

package de.nproth.pin.receiver;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.preference.PreferenceManager;

import de.nproth.pin.model.DatabaseManager;
import de.nproth.pin.model.NotificationManager;
import de.nproth.pin.model.Pin;
import de.nproth.pin.model.SettingsManager;
import de.nproth.pin.pinboard.Pinboard;
import de.nproth.pin.pinboard.PinboardService;
import de.nproth.pin.receiver.AlarmReceiver;

/**
 * Updates notifications in background using the JobScheduler framework.
 * Replaces {@link AlarmReceiver} on devices running android 5.0+
 */
@TargetApi(21)
public class NotificationJobService extends JobService {

    @Override
    public boolean onStartJob(final JobParameters params) {
        long now = System.currentTimeMillis();
        long lastCheck = SettingsManager.getLastSnoozedCheck(this);

        // Wake up all pins with lastCheck < wake_up < now
        Pin[] pins = DatabaseManager.getSnoozedPins(this, lastCheck);
        NotificationManager.wakeUpPins(this, pins);

        SettingsManager.setLastSnoozedCheck(this, now);

        // We're done here
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
