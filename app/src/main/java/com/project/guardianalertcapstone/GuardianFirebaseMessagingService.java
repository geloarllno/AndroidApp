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

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class GuardianFirebaseMessagingService extends FirebaseMessagingService {
    public static final String ACTION_NEW_NOTIFICATION = "com.project.guardianalertcapstone.NEW_NOTIFICATION";
    private static final String TAG = "GuardianFCM";
    private static final String CHANNEL_ID = "guardian_alert_channel";
    private static final String CHANNEL_NAME = "Guardian Alerts";
    private static final String CHANNEL_DESC = "Security alerts from Guardian Alert system";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            
            String title = remoteMessage.getNotification().getTitle();
            String body = remoteMessage.getNotification().getBody();
            
            // Save notification to Firestore first
            saveNotificationToFirestore(title, body);
            
            // Then show notification if not snoozed
            if (!NotificationSnoozeUtil.areNotificationsSnoozed(this)) {
                sendNotification(title, body);
            } else {
                Log.d(TAG, "Notifications are snoozed. Not showing notification UI.");
            }
            
            // Send local broadcast regardless of snooze status
            // This allows the app to update the notification list even if notifications are snoozed
            sendLocalBroadcast(title, body);
        }

        // Check if message contains a data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            
            // Always process data payload to save to Firestore
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String imageUrl = remoteMessage.getData().get("image_url");
            
            // Save to Firestore
            saveNotificationToFirestore(title, body, imageUrl);
            
            // Only show notification if not snoozed
            if (!NotificationSnoozeUtil.areNotificationsSnoozed(this)) {
                if (title != null && body != null) {
                    sendNotification(title, body);
                }
            } else {
                Log.d(TAG, "Notifications are snoozed. Not showing data payload notification.");
            }
            
            // Send local broadcast regardless of snooze status
            sendLocalBroadcast(title, body);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        Log.d(TAG, "Refreshed token: " + token);
        // Save the token to Firestore for this user
        saveTokenToFirestore(token);
    }
    
    private void saveTokenToFirestore(String token) {
        // If user is logged in, save their token
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("token", token);
            tokenData.put("updatedAt", new Date());
            
            db.collection("users").document(userId)
                .collection("tokens").document(token)
                .set(tokenData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "FCM Token saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving FCM token", e));
        }
    }
    
    /**
     * Send a local broadcast to notify the app that a new notification has arrived
     */
    private void sendLocalBroadcast(String title, String body) {
        Intent intent = new Intent(ACTION_NEW_NOTIFICATION);
        intent.putExtra("title", title);
        intent.putExtra("body", body);
        intent.putExtra("timestamp", System.currentTimeMillis());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Local broadcast sent for new notification");
    }

    private void sendNotification(String title, String messageBody) {
        Intent intent = new Intent(this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("notification", true);  // To open the notifications tab
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title != null ? title : "Guardian Alert")
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

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
     * Save a notification to Firestore
     */
    private void saveNotificationToFirestore(String title, String message) {
        saveNotificationToFirestore(title, message, null);
    }
    
    /**
     * Save a notification to Firestore with optional image URL
     */
    private void saveNotificationToFirestore(String title, String message, String imageUrl) {
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
        
        // Add image URL if available
        if (imageUrl != null && !imageUrl.isEmpty()) {
            notificationData.put("image", imageUrl);
        }
        
        // Initialize an empty readBy array
        notificationData.put("readBy", new ArrayList<>());
        
        db.collection("motion_alerts")
                .document(docId)
                .set(notificationData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Notification saved to Firestore"))
                .addOnFailureListener(e -> Log.e(TAG, "Error saving notification to Firestore", e));
    }
} 