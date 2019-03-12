package de.nproth.pin;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;

/**
 * Updates notifications in background using the JobScheduler framework.
 * Replaces {@link AlarmReceiver} on devices running android 5.0+
 */
@TargetApi(21)
public class NotificationJobService extends JobService {

    @Override
    public boolean onStartJob(final JobParameters params) {
        Pinboard.get(this).updateChanged();
        return false;//work is done at this point
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
