package com.project.guardianalertcapstone;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class DashboardActivity extends AppCompatActivity {
    private static final String TAG = "DashboardActivity";
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        FirebaseApp.initializeApp(this);

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }
        
        // Subscribe to notification topic for this device
        subscribeToNotificationTopics();
        
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Load HomeFragment by default, unless we're coming from a notification
        if (savedInstanceState == null) {
            if (getIntent().getBooleanExtra("notification", false)) {
                // Coming from notification, load NotificationFragment
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationFragment())
                    .commit();
                bottomNavigationView.setSelectedItemId(R.id.nav_alerts);
            } else {
                // Normal app start, load HomeFragment
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            }
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;

            if (item.getItemId() == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_camera) {
                selectedFragment = new CameraFragment();
            } else if (item.getItemId() == R.id.nav_alerts) {
                selectedFragment = new NotificationFragment();
            } else if (item.getItemId() == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, selectedFragment)
                    .commit();
            }

            return true;
        });
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle notification click when app is already running
        if (intent.getBooleanExtra("notification", false)) {
            // Navigate to notification fragment
            bottomNavigationView.setSelectedItemId(R.id.nav_alerts);
        }
    }
    
    private void subscribeToNotificationTopics() {
        // Subscribe to general topic
        FirebaseMessaging.getInstance().subscribeToTopic("general_alerts")
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Log.d(TAG, "Subscribed to general_alerts topic");
                } else {
                    Log.e(TAG, "Failed to subscribe to general_alerts topic", task.getException());
                }
            });
            
        // You can add user-specific subscriptions here if needed
    }
}