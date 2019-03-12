package de.nproth.pin.util;

import android.content.Context;

import de.nproth.pin.R;

/**
 * Utility class that converts a timestamp from milliseconds into something human readable
 */
public final class Timespan {

    public final long millis;
    public final Context mContext;

    public Timespan(Context c, long millis) {
        mContext = c;
        this.millis = millis;
    }

    public double inSecs() {
        return millis / 1000.;
    }

    public double inMins() {
        return millis / 60000.;
    }

    public double inHours() {
        return millis / 3600000.;
    }

    public double inDays() {
        return millis / 86400000.;
    }

    public int restDays() {
        return (int) inDays();
    }

    public int restHours() {
        long millis = this.millis % 86400000;
        return (int) (millis / 3600000);
    }

    public int restMins() {
        long millis = this.millis % 3600000;
        return (int) (millis / 60000);
    }

    public int restSecs() {
        long millis = this.millis % 60000;
        return (int) (millis / 1000);
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        if(restDays() > 0)
            str.append(restDays() + mContext.getString(R.string.symbol_time_days) + " ");
        if(restHours() > 0)
            str.append(restHours() + mContext.getString(R.string.symbol_time_hours) + " ");
        if(restMins() > 0)
            str.append(restMins() + mContext.getString(R.string.symbol_time_minutes) + " ");
        if(restSecs() > 0 || str.length() == 0)
            str.append(restSecs() + mContext.getString(R.string.symbol_time_seconds) + " ");
        str.deleteCharAt(str.length() - 1);
        return str.toString();
    }
}
