package com.project.guardianalertcapstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private LinearLayout viewProfile;
    private LinearLayout snoozeNotifications;
    private Button logoutButton;
    private static final String TAG = "SettingsFragment";
    private SharedPreferences preferences;
    private static final String PREF_NAME = "NotificationPrefs";
    private static final String SNOOZE_DURATION_KEY = "snoozeDuration";
    private static final String SNOOZE_END_TIME_KEY = "snoozeEndTime";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Initialize SharedPreferences
        preferences = requireActivity().getSharedPreferences(PREF_NAME, 0);

        viewProfile = view.findViewById(R.id.viewProfile);
        snoozeNotifications = view.findViewById(R.id.snoozeNotifications);
        logoutButton = view.findViewById(R.id.btnLogout);
        LinearLayout supportCenter = view.findViewById(R.id.supportCenter);

        viewProfile.setOnClickListener(v -> startActivity(new Intent(getActivity(), ViewProfileActivity.class)));

        snoozeNotifications.setOnClickListener(v -> showSnoozeDialog());

        supportCenter.setOnClickListener(v -> {
            Log.d(TAG, "Support center clicked");
            try {
                Intent intent = new Intent(requireActivity(), SupportCenterActivity.class);
                startActivity(intent);
                Log.d(TAG, "Successfully started SupportCenterActivity");
            } catch (Exception e) {
                Log.e(TAG, "Error starting SupportCenterActivity: " + e.getMessage());
                Toast.makeText(requireContext(), "Error opening Support Center screen", Toast.LENGTH_SHORT).show();
            }
        });

        logoutButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        return view;
    }

    private void showSnoozeDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_snooze_notifications, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(dialogView);
        
        RadioGroup snoozeOptions = dialogView.findViewById(R.id.snoozeOptions);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnSave = dialogView.findViewById(R.id.btnSave);
        
        // Get the current snooze duration
        long currentSnoozeDuration = preferences.getLong(SNOOZE_DURATION_KEY, 0);
        if (currentSnoozeDuration > 0) {
            // Check the appropriate radio button based on saved duration
            int radioButtonId = getRadioButtonIdForDuration(currentSnoozeDuration);
            if (radioButtonId != -1) {
                snoozeOptions.check(radioButtonId);
            }
        }
        
        AlertDialog dialog = builder.create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnSave.setOnClickListener(v -> {
            int selectedId = snoozeOptions.getCheckedRadioButtonId();
            if (selectedId != -1) {
                // Get the snooze duration in milliseconds
                long snoozeDurationMillis = getDurationForRadioButtonId(selectedId);
                long snoozeEndTime = System.currentTimeMillis() + snoozeDurationMillis;
                
                // Save the snooze settings
                SharedPreferences.Editor editor = preferences.edit();
                editor.putLong(SNOOZE_DURATION_KEY, snoozeDurationMillis);
                editor.putLong(SNOOZE_END_TIME_KEY, snoozeEndTime);
                editor.apply();
                
                // Get the selected option text
                RadioButton selectedButton = dialogView.findViewById(selectedId);
                String durationText = selectedButton.getText().toString();
                
                Toast.makeText(requireContext(), 
                    getString(R.string.notifications_snoozed, durationText), 
                    Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private int getRadioButtonIdForDuration(long duration) {
        // 15 minutes
        if (duration == 15 * 60 * 1000) {
            return R.id.snooze15min;
        }
        // 1 hour
        else if (duration == 60 * 60 * 1000) {
            return R.id.snooze1hour;
        }
        // 8 hours
        else if (duration == 8 * 60 * 60 * 1000) {
            return R.id.snooze8hours;
        }
        // 24 hours
        else if (duration == 24 * 60 * 60 * 1000) {
            return R.id.snooze24hours;
        }
        return -1;
    }
    
    private long getDurationForRadioButtonId(int radioButtonId) {
        // Convert durations to milliseconds
        if (radioButtonId == R.id.snooze15min) {
            return 15 * 60 * 1000; // 15 minutes
        } else if (radioButtonId == R.id.snooze1hour) {
            return 60 * 60 * 1000; // 1 hour
        } else if (radioButtonId == R.id.snooze8hours) {
            return 8 * 60 * 60 * 1000; // 8 hours
        } else if (radioButtonId == R.id.snooze24hours) {
            return 24 * 60 * 60 * 1000; // 24 hours
        }
        return 0;
    }
}
