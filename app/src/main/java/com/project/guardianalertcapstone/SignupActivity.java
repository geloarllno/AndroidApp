package com.project.guardianalertcapstone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;
import com.google.firebase.database.*;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.FirebaseException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.TimeUnit;

public class SignupActivity extends AppCompatActivity {

    EditText name, email, password, confirmPassword;
    Button signupBtn;
    TextView loginRedirect;
    CheckBox agreeCheckBox;

    private FirebaseAuth mAuth;
    private DatabaseReference databaseRef;
    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "SignupPrefs";
    private static final String KEY_REGISTERED = "isRegistered";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        confirmPassword = findViewById(R.id.confirmPassword);
        signupBtn = findViewById(R.id.signupBtn);
        loginRedirect = findViewById(R.id.loginRedirect);
        agreeCheckBox = findViewById(R.id.agreeCheckBox);

        mAuth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("Users");
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        signupBtn.setOnClickListener(v -> registerUser());
        loginRedirect.setOnClickListener(v -> startActivity(new Intent(SignupActivity.this, LoginActivity.class)));
        agreeCheckBox.setOnClickListener(this::showTermsDialog);
    }

    private void registerUser() {
        final String userName = name.getText().toString().trim();
        final String userEmail = email.getText().toString().trim();
        final String userPassword = password.getText().toString().trim();
        final String userConfirmPassword = confirmPassword.getText().toString().trim();

        if (!agreeCheckBox.isChecked()) {
            Toast.makeText(this, "You must agree to the Terms and Conditions!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (userName.isEmpty() || userEmail.isEmpty() || userPassword.isEmpty() || userConfirmPassword.isEmpty()) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidEmail(userEmail)) {
            Toast.makeText(this, "Please enter a valid email address!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!userPassword.equals(userConfirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidPassword(userPassword)) {
            Toast.makeText(this, "Password must be 8-12 characters, include at least 1 uppercase letter and a number!", Toast.LENGTH_LONG).show();
            return;
        }

        signupBtn.setEnabled(false); // prevent multiple taps

        // Email verification flow
        Toast.makeText(SignupActivity.this, "Processing email verification...", Toast.LENGTH_SHORT).show();
        mAuth.createUserWithEmailAndPassword(userEmail, userPassword)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Save user data to database
                            String userId = firebaseUser.getUid();
                            User newUser = new User(userId, userName, userEmail);
                            databaseRef.child(userId).setValue(newUser);

                            firebaseUser.sendEmailVerification()
                                    .addOnCompleteListener(verifyTask -> {
                                        if (verifyTask.isSuccessful()) {
                                            Toast.makeText(SignupActivity.this, "Registration Successful! Please verify your email.", Toast.LENGTH_LONG).show();
                                            saveAndRedirectToLogin();
                                        } else {
                                            Toast.makeText(SignupActivity.this, "Failed to send verification email.", Toast.LENGTH_SHORT).show();
                                            signupBtn.setEnabled(true);
                                        }
                                    });
                        }
                    } else {
                        signupBtn.setEnabled(true);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "";
                        if (errorMessage != null && errorMessage.toLowerCase().contains("already in use")) {
                            Toast.makeText(SignupActivity.this, "This email is already registered!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SignupActivity.this, "Registration Failed: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveAndRedirectToLogin() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_REGISTERED, true);
        editor.apply();
        mAuth.signOut();
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void showTermsDialog(View view) {
        final CheckBox checkBox = (CheckBox) view;
        if (checkBox.isChecked()) {
            new AlertDialog.Builder(this)
                    .setTitle("Terms and Conditions" +
                            "Effective Date: May 4, 2026\n" +
                            "Welcome to Guardian Alert. Please read these Terms and Conditions (“Terms”) carefully before using our mobile application and services (the “Service”) developed as part of the Guardian Alert Capstone Project.")
                    .setMessage("Acceptance of Terms\n" +
                            "\n" +
                            "By signing up, using, or accessing Guardian Alert, you agree to be legally bound by these Terms and our Privacy Policy. These Terms may be updated from time to time, and continued use of the Service signifies your acceptance of any modifications.\n" +
                            "\n" +
                            "User Registration and Email Use\n" +
                            "\n" +
                            "To use Guardian Alert, users must register by providing a valid email address and creating a secure password. You agree to:\n" +
                            "\n" +
                            "Provide accurate and up-to-date information.\n" +
                            "\n" +
                            "Maintain the confidentiality of your login credentials.\n" +
                            "\n" +
                            "Promptly update your email address if it changes.\n" +
                            "\n" +
                            "We may use your registered email to:\n" +
                            "\n" +
                            "Verify your identity during account registration.\n" +
                            "\n" +
                            "Send system alerts or important notifications.\n" +
                            "\n" +
                            "Respond to user inquiries and support requests.\n" +
                            "\n" +
                            "All email communication with administrators will be conducted through this registered email only.\n" +
                            "\n" +
                            "System Usage and Alerts\n" +
                            "\n" +
                            "Guardian Alert provides real-time home monitoring and mobile alerts based on motion sensor triggers and security system activity. While the system enhances home safety, you acknowledge that:\n" +
                            "\n" +
                            "It does not automatically contact emergency services.\n" +
                            "\n" +
                            "You are responsible for checking alerts and taking necessary action.\n" +
                            "\n" +
                            "The live footage cannot be downloaded or stored for playback.\n" +
                            "\n" +
                            "Data and Privacy\n" +
                            "\n" +
                            "We prioritize user privacy. Data such as email addresses and motion detection logs are stored securely using Firebase. No video footage is stored or shared, ensuring your privacy.\n" +
                            "\n" +
                            "Please refer to our [Privacy Policy] for full details.\n" +
                            "\n" +
                            "User Responsibilities\n" +
                            "\n" +
                            "As a user, you agree to:\n" +
                            "\n" +
                            "Use the Service only for lawful purposes.\n" +
                            "\n" +
                            "Not interfere with the proper functioning of the system.\n" +
                            "\n" +
                            "Report any issues via the in-app Help Center or email communication.\n" +
                            "\n" +
                            "Misuse of the system or unauthorized access attempts may result in termination of your access.\n" +
                            "\n" +
                            "System Limitations\n" +
                            "\n" +
                            "Guardian Alert is a prototype developed for academic purposes. Limitations include:\n" +
                            "\n" +
                            "No support for iOS devices (Android only).\n" +
                            "\n" +
                            "Compatibility limited to Android 7 and above.\n" +
                            "\n" +
                            "Inability to download video recordings.\n" +
                            "\n" +
                            "Requires manual battery maintenance.\n" +
                            "\n" +
                            "Intellectual Property\n" +
                            "\n" +
                            "All software, content, and designs associated with Guardian Alert remain the intellectual property of the development team. You may not copy, reproduce, or distribute any part of the Service without written permission.\n" +
                            "\n" +
                            "Termination\n" +
                            "\n" +
                            "We reserve the right to terminate or suspend access to the Service at our discretion if you breach these Terms.\n" +
                            "\n" +
                            "Contact\n" +
                            "\n" +
                            "If you have questions about these Terms, please contact us via the Help Center in the app or email our admin team directly.")
                    .setPositiveButton("Agree", (dialog, which) -> checkBox.setChecked(true))
                    .setNegativeButton("Cancel", (dialog, which) -> checkBox.setChecked(false))
                    .setCancelable(false)
                    .show();
        }
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private boolean isValidPassword(String password) {
        return password.matches("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{8,12}$");
    }

    private boolean isValidPhoneNumber(String phone) {
        // Remove any leading '0' for validation
        if (phone.startsWith("0")) {
            phone = phone.substring(1);
        }
        // Check if it's a 10-digit number (after removing leading 0)
        // or 11-digit number (if starting with 0)
        return phone.matches("^\\d{10}$") || phone.matches("^0\\d{10}$");
    }

    public static class User {
        public String userId, name, email;

        public User(String userId, String name, String email) {
            this.userId = userId;
            this.name = name;
            this.email = email;
        }
    }
}
