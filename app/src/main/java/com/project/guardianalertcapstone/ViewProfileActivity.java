package com.project.guardianalertcapstone;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ViewProfileActivity extends AppCompatActivity {

    private TextView userEmail;
    private EditText editUsername;
    private Button btnSaveUsername, btnChangeEmail, btnChangePassword;
    private ImageButton btnBack;
    private FirebaseAuth auth;
    private DatabaseReference databaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        // Initialize views
        userEmail = findViewById(R.id.userEmail);
        editUsername = findViewById(R.id.editUsername);
        btnSaveUsername = findViewById(R.id.btnSaveUsername);
        btnChangeEmail = findViewById(R.id.btnChangeEmail);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnBack = findViewById(R.id.btnBack);

        auth = FirebaseAuth.getInstance();
        FirebaseUser user = auth.getCurrentUser();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");

        if (user != null) {
            userEmail.setText(user.getEmail());

            btnSaveUsername.setOnClickListener(v -> {
                String newUsername = editUsername.getText().toString().trim();
                if (!TextUtils.isEmpty(newUsername)) {
                    String userId = user.getUid();
                    databaseRef.child(userId).child("name").setValue(newUsername);
                    Toast.makeText(ViewProfileActivity.this, "Username updated!", Toast.LENGTH_SHORT).show();
                }
            });

        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_SHORT).show();
        }

        btnChangeEmail.setOnClickListener(v -> startActivity(new Intent(ViewProfileActivity.this, ChangeEmailActivity.class)));
        btnChangePassword.setOnClickListener(v -> startActivity(new Intent(ViewProfileActivity.this, ChangePasswordActivity.class)));

        // Back button functionality
        btnBack.setOnClickListener(v -> {
            finish(); // Close ViewProfileActivity and return to the previous fragment
        });
    }
}
