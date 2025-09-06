package com.project.guardianalertcapstone;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class HelpCenterActivity extends AppCompatActivity {

    private static final String TAG = "HelpCenterActivity";
    private ImageButton backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_center);

        try {
            // Initialize views
            TextView faq1 = findViewById(R.id.faq1);
            TextView faq2 = findViewById(R.id.faq2);
            TextView faq3 = findViewById(R.id.faq3);
            TextView faq4 = findViewById(R.id.faq4);
            backButton = findViewById(R.id.backButton);

            // Set click listeners for FAQ items
            if (faq1 != null) {
                faq1.setOnClickListener(v -> showFaqAnswer("Password Reset", 
                    "To reset your password:\n\n" +
                    "1. Click on 'Forgot Password' on the login screen\n" +
                    "2. Enter your registered email\n" +
                    "3. Follow the instructions sent to your email"));
            }


            if (faq2 != null) {
                faq2.setOnClickListener(v -> showFaqAnswer("Notifications",
                    "If you're not receiving motion alerts:\n\n" +
                    "1. Refresh your notifications\n" +
                    "2. Make sure your device is connected to the android app\n" +
                    "3. Ensure notifications are enabled in app settings\n" +
                    "4. Check your internet connection\n" +
                    "5. Try logging out and back in to refresh the connection"));
            }

            if (faq3 != null) {
                faq3.setOnClickListener(v -> showFaqAnswer("Profile Update",
                    "To update your profile:\n\n" +
                    "1. Go to settings\n" +
                    "2. Click view profile\n" +
                    "3. Make your changes\n" +
                    "4. Save the updates"));
            }

            if (faq4 != null) {
                faq4.setOnClickListener(v -> showFaqAnswer("Account Recovery",
                    "To recover your account:\n\n" +
                    "1. Click on 'Forgot Account' on the login screen\n" +
                    "2. Enter your registered email address\n" +
                    "3. Verify your identity through the email verification code\n" +
                    "4. Follow the account recovery steps\n" +
                    "5. If you need additional help, contact our support team"));
            }

            // Set click listener for back button
            if (backButton != null) {
                backButton.setOnClickListener(v -> {
                    try {
                        finish();
                    } catch (Exception e) {
                        Log.e(TAG, "Error finishing activity", e);
                    }
                });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing help center", e);
            Toast.makeText(this, "An error occurred. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showFaqAnswer(String title, String answer) {
        try {
            new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(answer)
                .setPositiveButton("OK", null)
                .setCancelable(true)
                .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing FAQ answer", e);
            Toast.makeText(this, "Unable to show answer. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            super.onDestroy();
        } catch (Exception e) {
            Log.e(TAG, "Error in onDestroy", e);
        }
    }
} 