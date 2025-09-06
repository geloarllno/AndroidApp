package com.project.guardianalertcapstone;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Utility class for testing notifications
 */
public class NotificationTestUtil {
    private static final String TAG = "NotificationTestUtil";
    private static final String CHANNEL_ID = "guardian_test_channel";
    private static final String CHANNEL_NAME = "Guardian Test Alerts";
    private static final String CHANNEL_DESC = "Test alerts from Guardian Alert system";

    /**
     * Send a test notification
     * @param context Context
     * @param title Notification title
     * @param message Notification message
     */
    public static void sendTestNotification(Context context, String title, String message) {
        // Always save to Firestore regardless of snooze status
        // This ensures notifications always appear in the notification list
        saveNotificationToFirestore(title, message);
        
        // Send local broadcast to update the UI immediately
        sendLocalBroadcast(context, title, message);
        
        // Only show the actual notification if not snoozed
        if (NotificationSnoozeUtil.areNotificationsSnoozed(context)) {
            Log.d(TAG, "Notifications are snoozed. Not showing test notification.");
            return;
        }
        
        Intent intent = new Intent(context, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("notification", true);
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
        }

        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, notificationBuilder.build());
    }
    
    /**
     * Send a local broadcast to update the notification list
     */
    private static void sendLocalBroadcast(Context context, String title, String message) {
        Intent intent = new Intent(GuardianFirebaseMessagingService.ACTION_NEW_NOTIFICATION);
        intent.putExtra("title", title);
        intent.putExtra("body", message);
        intent.putExtra("timestamp", System.currentTimeMillis());
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        Log.d(TAG, "Local broadcast sent for test notification");
    }
    
    /**
     * Save a test notification to Firestore
     * @param title Notification title
     * @param message Notification message
     */
    private static void saveNotificationToFirestore(String title, String message) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Create a timestamp in the format expected by your app
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        
        // Generate a document ID
        String docId = "alert_" + System.currentTimeMillis();
        
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", message);
        notificationData.put("timestamp", timestamp);
        
        // Initialize an empty readBy array for marking notifications as read
        notificationData.put("readBy", new ArrayList<>());
        
        db.collection("motion_alerts")
                .document(docId)
                .set(notificationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification to Firestore", e));
    }
} 