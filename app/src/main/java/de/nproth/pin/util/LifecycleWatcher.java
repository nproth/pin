package de.nproth.pin.util;

/**
 * Provides a static interface to check whether the boot completed broadcast was received. This allows the app to inform the user that he must disable some energy saving options
 * for this app in order to work properly if appropriate
 */
public final class LifecycleWatcher {
    private static boolean onBoot = false;

    private static boolean userWarned = false;

    public static void informBootReceived() {
        onBoot = true;
    }

    public static void informUserWarned() {
        userWarned = true;
    }

    public static boolean hasBootReceived() {
        return onBoot;
    }

    public static boolean isUserWarned() {
        return userWarned;
    }

    /**
     * For debugging only
     */
    public static void reset() {
        onBoot = false;
        userWarned = true;
    }
}
