package com.project.guardianalertcapstone;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to handle notification snooze functionality
 */
public class NotificationSnoozeUtil {
    private static final String PREF_NAME = "NotificationPrefs";
    private static final String SNOOZE_END_TIME_KEY = "snoozeEndTime";
    private static final String SNOOZE_DURATION_KEY = "snoozeDuration";

    /**
     * Check if notifications are currently snoozed
     * @param context The context
     * @return true if notifications are snoozed, false otherwise
     */
    public static boolean areNotificationsSnoozed(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, 0);
        long snoozeEndTime = preferences.getLong(SNOOZE_END_TIME_KEY, 0);
        
        // If snooze end time is 0 or has passed, notifications are not snoozed
        return snoozeEndTime > System.currentTimeMillis();
    }
    
    /**
     * Get the remaining snooze time in milliseconds
     * @param context The context
     * @return The remaining snooze time in milliseconds, or 0 if not snoozed
     */
    public static long getRemainingSnoozeTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, 0);
        long snoozeEndTime = preferences.getLong(SNOOZE_END_TIME_KEY, 0);
        long currentTime = System.currentTimeMillis();
        
        if (snoozeEndTime > currentTime) {
            return snoozeEndTime - currentTime;
        }
        return 0;
    }
    
    /**
     * Clear the snooze settings
     * @param context The context
     */
    public static void clearSnooze(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(SNOOZE_END_TIME_KEY, 0);
        editor.putLong(SNOOZE_DURATION_KEY, 0);
        editor.apply();
    }
    
    /**
     * Get a human-readable string of the remaining snooze time
     * @param context The context
     * @return A string representation of the remaining snooze time, or empty string if not snoozed
     */
    public static String getSnoozeTimeRemaining(Context context) {
        long remainingMillis = getRemainingSnoozeTime(context);
        
        if (remainingMillis <= 0) {
            return "";
        }
        
        long seconds = remainingMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return hours + " hr " + minutes + " min";
        } else {
            return minutes + " min";
        }
    }
} 