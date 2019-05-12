package de.nproth.pin.pinboard;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;

import de.nproth.pin.BuildConfig;
import de.nproth.pin.receiver.SnoozeNoteReceiver;

public class PinboardService extends Service {

    public static final String INTENT_ACTION_SNOOZE_PIN = BuildConfig.APPLICATION_ID + ".intent.action.snooze";
    public static final String INTENT_ACTION_WAKE_UP = BuildConfig.APPLICATION_ID + ".intent.action.wake_up";

    public static final String PREFERENCE_PERSISTENT_NOTIFICATIONS = "pref_persistent_notifications";
    public static final String PREFERENCE_SNOOZE_DURATION = "snooze_duration";

    public static final long DEFAULT_SNOOZE_DURATION = 30 * 60 * 1000; //30min in millis
    public static final boolean DEFAULT_PERSISTENT_NOTIFICATIONS = false;

    private Pinboard mPinboard;
    private SharedPreferences mPrefs;

    public class PinboardBinder extends Binder {

        public void update() {
            mPinboard.updateAll(true);
        }

        public long getSnoozeDuration() {
            return mPinboard.getSnoozeDuration();
        }

        public void setSnoozeDuration(long dur) {
            mPinboard.setSnoozeDuration(dur);
            mPrefs.edit().putLong(PREFERENCE_SNOOZE_DURATION, dur).apply();
        }

        public boolean getIsFixed() {
            return mPinboard.getIsFixed();
        }

        public void setIsFixed(boolean fix) {
            mPinboard.setIsFixed(fix);
        }
    }

    public PinboardService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        init();

        if(intent != null && INTENT_ACTION_SNOOZE_PIN.equals(intent.getAction())) {
            int rows = SnoozeNoteReceiver.onSnoozePins(this, mPinboard.getSnoozeDuration(), intent.getData());
        }

        //mPinboard.updateAll(! INTENT_ACTION_WAKE_UP.equals(intent.getAction()));
        mPinboard.updateAll(true);
        return START_STICKY;
    }

    private void init() {
        if(mPinboard == null) {
            mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

            mPinboard = Pinboard.get(this);

            mPinboard.setSnoozeDuration(mPrefs.getLong(PREFERENCE_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION));
            mPinboard.setIsFixed(mPrefs.getBoolean(PREFERENCE_PERSISTENT_NOTIFICATIONS, DEFAULT_PERSISTENT_NOTIFICATIONS));

            mPinboard.updateAll(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        //Committing the changed preference values is deliberately done synchronously (blocking), so that the Service is not destroyed before the transaction is completed.
        mPrefs.edit().putLong(PREFERENCE_SNOOZE_DURATION, mPinboard.getSnoozeDuration())
                .putBoolean(PREFERENCE_PERSISTENT_NOTIFICATIONS, mPinboard.getIsFixed()).commit();
    }

    @Override
    public IBinder onBind(Intent intent) {
        init();
        return new PinboardBinder();
    }
}
