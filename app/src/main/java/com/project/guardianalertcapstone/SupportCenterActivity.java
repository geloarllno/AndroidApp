package com.project.guardianalertcapstone;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;

public class SupportCenterActivity extends AppCompatActivity {
    private LinearLayout deviceContainer;
    private boolean isInstructionsVisible = false;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support_center);

        // Initialize views
        deviceContainer = findViewById(R.id.deviceContainer);
        TextView deviceName = findViewById(R.id.deviceName);
        TextView instructions = findViewById(R.id.instructions);
        btnBack = findViewById(R.id.btnBack);

        // Set device name
        deviceName.setText("Guardian Eye A100");

        // Set instructions text
        instructions.setText("To connect your Guardian Eye A100 device:\n\n" +
                "1. Power on your Guardian Eye A100 device\n" +
                "2. Wait for the device to initialize\n" +
                "3. On your smartphone, go to WiFi settings\n" +
                "4. Connect to the 'GuardianEye_A100' network\n" +
                "5. Open the Guardian Alert app\n" +
                "6. Go to Settings > Support Center\n" +
                "7. Tap on Guardian Eye A100\n" +
                "8. Follow the on-screen instructions to complete setup\n\n" +
                "Note: Make sure your device is within range of the Guardian Eye A100");

        // Set click listener for device container
        deviceContainer.setOnClickListener(v -> {
            isInstructionsVisible = !isInstructionsVisible;
            instructions.setVisibility(isInstructionsVisible ? View.VISIBLE : View.GONE);
        });

        // Set click listener for back button
        btnBack.setOnClickListener(v -> {
            finish(); // This will close the activity and return to the previous screen
        });
    }
} 