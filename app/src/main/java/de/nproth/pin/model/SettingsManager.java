/*
 * Changelog
 *
 * 2019-10-29
 * - Create class NotificationManager
 */

package de.nproth.pin.model;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceManager;

/**
 * Interface to Shared Preferences
 */
public final class SettingsManager {

    private static final String PREFERENCE_PERSISTENT_NOTIFICATIONS = "pref_persistent_notifications";
    private static final String PREFERENCE_SNOOZE_DURATION = "snooze_duration";
    private static final String PREFERENCE_LAST_SNOOZED_CHECK = "last_snoozed_check";

    private static final long DEFAULT_SNOOZE_DURATION = 30 * 60 * 1000; //30min in millis
    private static final boolean DEFAULT_PERSISTENT_NOTIFICATIONS = false;
    private static final long DEFAULT_LAST_SNOOOZED_CHECK = 0;

    private SettingsManager() {}

    public static boolean getPinsFixed(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREFERENCE_PERSISTENT_NOTIFICATIONS, DEFAULT_PERSISTENT_NOTIFICATIONS);
    }

    public static void setPinsFixed(Context context, boolean fixed) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(PREFERENCE_PERSISTENT_NOTIFICATIONS, fixed).apply();
    }

    public static long getSnoozeDuration(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(PREFERENCE_SNOOZE_DURATION, DEFAULT_SNOOZE_DURATION);
    }

    public static void setSnoozeDuration(Context context, long duration) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PREFERENCE_SNOOZE_DURATION, duration).apply();
    }

    public static long getLastSnoozedCheck(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(PREFERENCE_LAST_SNOOZED_CHECK, DEFAULT_LAST_SNOOOZED_CHECK);
    }

    public static void setLastSnoozedCheck(Context context, long check) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(PREFERENCE_LAST_SNOOZED_CHECK, check).apply();
    }
}
