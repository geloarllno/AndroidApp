package com.project.guardianalertcapstone;

import android.app.AlertDialog;
import android.os.Bundle;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment {
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private ArrayList<NotificationModel> notificationList;
    private FirebaseFirestore firestore;
    private TextView noNotificationsText;
    private TextView snoozeBannerText;
    private Button cancelSnoozeButton;
    private View snoozeBanner;
    private String currentUid;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ListenerRegistration notificationListener;
    private static final String TAG = "NotificationFragment";
    
    // Broadcast receiver to handle notification updates
    private final BroadcastReceiver notificationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (GuardianFirebaseMessagingService.ACTION_NEW_NOTIFICATION.equals(intent.getAction())) {
                // Refresh notifications when a new one arrives
                if (!NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
                    loadNotifications();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        
        // For Android 13+ we need to request notification permission
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2);
            }
        }

        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        // Set up SwipeRefreshLayout
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::loadNotifications);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        recyclerView = view.findViewById(R.id.recyclerView_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        recyclerView.setAdapter(adapter);

        // Initialize the snooze banner
        snoozeBanner = view.findViewById(R.id.snooze_banner);
        snoozeBannerText = view.findViewById(R.id.snooze_banner_text);
        cancelSnoozeButton = view.findViewById(R.id.btn_cancel_snooze);
        
        if (snoozeBanner != null) {
            updateSnoozeBanner();
        }

        // Test notification button
        ImageButton btnTestNotification = view.findViewById(R.id.btn_test_notification);
        btnTestNotification.setOnClickListener(v -> {
            if (NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
                Toast.makeText(requireContext(), 
                    getString(R.string.notifications_snoozed_toast), 
                    Toast.LENGTH_SHORT).show();
            } else {
                String currentTime = android.text.format.DateFormat.format("HH:mm:ss", new java.util.Date()).toString();
                NotificationTestUtil.sendTestNotification(
                    requireContext(),
                    getString(R.string.motion_detected),
                    getString(R.string.motion_detected_message, currentTime)
                );
                Toast.makeText(requireContext(), getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show();
            }
        });

        ImageButton btnDeleteNotifications = view.findViewById(R.id.btn_delete_notifications);
        btnDeleteNotifications.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle(R.string.confirm_delete_title)
                    .setMessage(R.string.confirm_delete_message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> {
                        swipeRefreshLayout.setRefreshing(true);
                        FirebaseFirestore.getInstance().collection("motion_alerts")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        String docId = doc.getId();
                                        if (docId.startsWith("alert_")) {
                                            firestore.collection("motion_alerts").document(docId).delete()
                                                    .addOnSuccessListener(aVoid -> Log.d("DeleteNotif", "Deleted: " + docId))
                                                    .addOnFailureListener(e -> Log.e("DeleteNotif", "❌ Failed to delete " + docId, e));
                                        }
                                    }

                                    // Remove from RecyclerView list
                                    notificationList.removeIf(model -> model.getDocId().startsWith("alert_"));
                                    adapter.notifyDataSetChanged();
                                    
                                    // Show the empty state if needed
                                    if (notificationList.isEmpty()) {
                                        noNotificationsText.setText(R.string.no_notifications);
                                        noNotificationsText.setVisibility(View.VISIBLE);
                                    }
                                    swipeRefreshLayout.setRefreshing(false);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("DeleteNotif", "❌ Failed to fetch documents", e);
                                    swipeRefreshLayout.setRefreshing(false);
                                });
                    })
                    .setNegativeButton(R.string.no, null)
                    .show();
        });

        ImageButton btnMarkAllRead = view.findViewById(R.id.btn_mark_all_read);
        btnMarkAllRead.setOnClickListener(v -> {
            adapter.markAllAsRead();
        });

        noNotificationsText = view.findViewById(R.id.no_notifications_text);
        noNotificationsText.setVisibility(View.VISIBLE);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = (currentUser != null) ? currentUser.getUid() : "anonymous";

        firestore = FirebaseFirestore.getInstance();
        loadNotifications();

        // Register for local broadcasts (used for notification updates)
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(
                notificationReceiver,
                new IntentFilter(GuardianFirebaseMessagingService.ACTION_NEW_NOTIFICATION)
        );

        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateSnoozeBanner();
        loadNotifications(); // Reload notifications on resume to reflect any snooze changes
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // Remove the listener to prevent memory leaks
        if (notificationListener != null) {
            notificationListener.remove();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Unregister the broadcast receiver
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(notificationReceiver);
    }
    
    private void updateSnoozeBanner() {
        // Only proceed if the views are available
        if (snoozeBanner == null || snoozeBannerText == null || cancelSnoozeButton == null) {
            Log.e(TAG, "Snooze banner views not found");
            return;
        }
        
        if (NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
            // Show the snooze banner with remaining time
            snoozeBanner.setVisibility(View.VISIBLE);
            String remainingTime = NotificationSnoozeUtil.getSnoozeTimeRemaining(requireContext());
            snoozeBannerText.setText(getString(R.string.notifications_snoozed, remainingTime));
            
            cancelSnoozeButton.setOnClickListener(v -> {
                NotificationSnoozeUtil.clearSnooze(requireContext());
                snoozeBanner.setVisibility(View.GONE);
                loadNotifications(); // Reload notifications
                Toast.makeText(requireContext(), getString(R.string.snooze_canceled), Toast.LENGTH_SHORT).show();
            });
        } else {
            snoozeBanner.setVisibility(View.GONE);
        }
    }

    private void loadNotifications() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        
        notificationList.clear();
        
        // If notifications are snoozed, don't load them
        if (NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
            noNotificationsText.setText(R.string.notifications_currently_snoozed);
            noNotificationsText.setVisibility(View.VISIBLE);
            adapter.notifyDataSetChanged();
            if (swipeRefreshLayout != null) {
                swipeRefreshLayout.setRefreshing(false);
            }
            return;
        }
        
        CollectionReference alertsRef = firestore.collection("motion_alerts");

        alertsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        noNotificationsText.setText(R.string.no_notifications);
                        noNotificationsText.setVisibility(View.VISIBLE);
                        if (swipeRefreshLayout != null) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                        return;
                    }

                    for (DocumentSnapshot doc : snapshots) {
                        addNotificationFromDocument(doc);
                    }
                    
                    adapter.notifyDataSetChanged();
                    noNotificationsText.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "❌ Failed to load notifications.", e);
                    noNotificationsText.setText("Error loading notifications");
                    noNotificationsText.setVisibility(View.VISIBLE);
                    if (swipeRefreshLayout != null) {
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
                
        // Also set up a listener for real-time updates
        setupRealtimeNotificationListener();
    }
    
    private void setupRealtimeNotificationListener() {
        // Remove any existing listener
        if (notificationListener != null) {
            notificationListener.remove();
        }
        
        // Only setup listener if notifications are not snoozed
        if (NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
            return;
        }
        
        CollectionReference alertsRef = firestore.collection("motion_alerts");
        
        notificationListener = alertsRef.orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "❌ Listen failed.", e);
                        return;
                    }
                    
                    // If notifications are snoozed, don't process updates
                    if (NotificationSnoozeUtil.areNotificationsSnoozed(requireContext())) {
                        return;
                    }

                    if (snapshots == null || snapshots.isEmpty()) {
                        return;
                    }

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot doc = dc.getDocument();
                            
                            // Check if we already have this notification
                            boolean exists = false;
                            for (NotificationModel model : notificationList) {
                                if (model.getDocId().equals(doc.getId())) {
                                    exists = true;
                                    break;
                                }
                            }
                            
                            if (!exists) {
                                addNotificationFromDocument(doc);
                                adapter.notifyItemInserted(0);
                                noNotificationsText.setVisibility(View.GONE);
                                
                                // Scroll to the top to show the new notification
                                if (!notificationList.isEmpty()) {
                                    recyclerView.smoothScrollToPosition(0);
                                }
                            }
                        }
                    }
                });
    }
    
    private void addNotificationFromDocument(DocumentSnapshot doc) {
        String docId = doc.getId();
        String timestamp = doc.getString("timestamp");
        String title = doc.getString("title");
        String message = doc.getString("message");
        
        if (timestamp == null || timestamp.isEmpty()) timestamp = "Unknown Time";
        
        String displayMessage = (title != null) ? title : "Motion Alert";
        if (message != null && !message.isEmpty()) {
            displayMessage = message;
        }

        String base64Image = "";
        try {
            if (doc.contains("image")) {
                base64Image = doc.getString("image");
            }
        } catch (Exception ex) {
            Log.e("Firestore", "❌ Error processing image data", ex);
        }

        List<String> readBy = (List<String>) doc.get("readBy");
        boolean isRead = readBy != null && readBy.contains(currentUid);

        notificationList.add(0, new NotificationModel(docId, timestamp, displayMessage, base64Image, isRead));
    }
}