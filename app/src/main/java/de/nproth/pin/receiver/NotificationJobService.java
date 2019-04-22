package de.nproth.pin.receiver;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;

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
        //update notifications
        Intent i = new Intent(this, PinboardService.class);
        i.setAction(PinboardService.INTENT_ACTION_WAKE_UP);
        startService(i);
        return false;//work is done at this point
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
