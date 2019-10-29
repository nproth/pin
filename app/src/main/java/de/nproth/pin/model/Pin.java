/*
 * Changelog
 *
 * 2019-10-29
 * - Create class Pin
 */

package de.nproth.pin.model;

import android.content.Context;
import android.net.Uri;

import de.nproth.pin.NotesProvider;

/**
 * Represents a pin's data
 */
public class Pin {

    private int mId = -1;
    private String mText;
    private long mCreated;
    private long mModified;
    private long mWakeup = 0;


    /**
     * Creates a new, empty pin
     */
    public Pin() {

    }

    /**
     * @return The pins id in the database or -1 if this pin is not backed by a database entry.
     */
    public int getId() {
        return mId;
    }

    /**
     * @return Returns the time of creation in milliseconds since January 1,1970 UTC.
     */
    public long getCreated() {
        return mCreated;
    }

    /**
     * @return Returns the time this pins database entry was last modified in milliseconds since January 1,1970 UTC or 0.
     * @deprecated
     * The corresponding database attribute will probably be removed in a future update as it is not needed for any functionality.
     * Although it might currently be in use.
     */
    @Deprecated
    public long getModified() {
        return mModified;
    }

    /**
     * @return Returns the time this pin was last scheduled to wake up in milliseconds since January 1,1970 UTC or 0.
     */
    public long getWakeUp() {
        return mWakeup;
    }

    /**
     * @return
     * Text of this pin.
     */
    public String getText() {
        return mText;
    }


    /**
     * Changes a pins text attribute
     */
    public void setText(String text) {
        mText = text;
    }

    void setId(int id) {
        mId = id;
    }

    void setCreated(long created) {
        mCreated = created;
    }

    void setModified(long modified) {
        mModified = modified;
    }

    void setWakeUp(long wakeup) {
        mWakeup = wakeup;
    }

    /**
     * Snooze or hide a pin from the notification area to be woken up and made visible again later.
     * This method changes the database entry, notification and schedules the pins wake up.
     * @param duration
     * Time span in milliseconds the pin will remain hidden
     */
    public void snooze(Context context, long duration) {
        long now = System.currentTimeMillis();

        if(duration <= 0)
            return;

        setWakeUp(now + duration);
        setModified(now);

        DatabaseManager.updatePin(context, this);
        NotificationManager.hidePin(context, this);
        AlarmManager.setAlarm(context, getWakeUp());
    }

    /**
     * Snoozes a pin for the current default snooze duration.
     * See also {@link Pin#snooze(Context, long)}.
     */
    public void snooze(Context context) {
        snooze(context, SettingsManager.getSnoozeDuration(context));
    }

    public static void snoozeAllPins(Context c, long dur) {
        long wakeup = System.currentTimeMillis() + dur;
        DatabaseManager.updateAllPinsWakeUp(c, wakeup);
        NotificationManager.hideAllPins(c);
        AlarmManager.setAlarm(c, wakeup);
    }

    public static void snoozeAllPins(Context c) {
        snoozeAllPins(c, SettingsManager.getSnoozeDuration(c));
    }
}
