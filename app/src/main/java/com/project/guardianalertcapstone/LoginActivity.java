package com.project.guardianalertcapstone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import java.util.concurrent.TimeUnit;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button loginBtn;
    TextView signupRedirect, forgotPassword, helpCenter;
    ImageView passwordToggle;
    private FirebaseAuth auth;
    private boolean isPasswordVisible = false;
    private String verificationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        signupRedirect = findViewById(R.id.signupRedirect);
        forgotPassword = findViewById(R.id.forgotPassword);
        helpCenter = findViewById(R.id.helpCenter);
        passwordToggle = findViewById(R.id.passwordToggle);

        auth = FirebaseAuth.getInstance();

        signupRedirect.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );

        forgotPassword.setOnClickListener(v -> resetPassword());

        helpCenter.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, HelpCenterActivity.class);
            startActivity(intent);
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        passwordToggle.setOnClickListener(v -> togglePasswordVisibility());

        loginBtn.setOnClickListener(v -> loginUser());

        // 🔔 Request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void loginUser() {
        String input = email.getText().toString().trim();
        String passwordInput = password.getText().toString().trim();

        if (TextUtils.isEmpty(input)) {
            Toast.makeText(this, "Please enter your email or phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isPhoneNumber(input)) {
            startOtpVerification(input);
        } else {
            if (!Patterns.EMAIL_ADDRESS.matcher(input).matches()) {
                Toast.makeText(this, "Invalid email format.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (TextUtils.isEmpty(passwordInput)) {
                Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.signInWithEmailAndPassword(input, passwordInput)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null && user.isEmailVerified()) {
                                Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "Please verify your email before logging in.", Toast.LENGTH_LONG).show();
                                auth.signOut();
                            }
                        } else {
                            Toast.makeText(LoginActivity.this, "Login failed. Check your credentials.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private boolean isPhoneNumber(String input) {
        return input.matches("^\\+?\\d{10,13}$");
    }

    private void startOtpVerification(String phoneNumber) {
        final String formattedNumber;
        if (phoneNumber.startsWith("09")) {
            formattedNumber = "+63" + phoneNumber.substring(1);
        } else if (!phoneNumber.startsWith("+")) {
            formattedNumber = "+63" + phoneNumber;
        } else {
            formattedNumber = phoneNumber;
        }

        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    @Override
                    public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                        auth.signInWithCredential(credential).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                Toast.makeText(LoginActivity.this, "OTP Login successful!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                                finish();
                            } else {
                                Toast.makeText(LoginActivity.this, "OTP Login failed.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onVerificationFailed(@NonNull FirebaseException e) {
                        Toast.makeText(LoginActivity.this, "OTP Verification Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                        super.onCodeSent(verifyId, token);
                        verificationId = verifyId;
                        Toast.makeText(LoginActivity.this, "OTP code sent. Please check your SMS.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(LoginActivity.this, PhoneVerificationActivity.class);
                        intent.putExtra("verificationId", verificationId);
                        intent.putExtra("phoneNumber", formattedNumber);
                        intent.putExtra("isSignupFlow", false);
                        startActivity(intent);
                    }
                })
                .build();

        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private void resetPassword() {
        String emailInput = email.getText().toString().trim();

        if (TextUtils.isEmpty(emailInput)) {
            Toast.makeText(this, "Enter your registered email to reset password.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
            Toast.makeText(this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(emailInput)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(LoginActivity.this, "Reset password link sent to your email.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(LoginActivity.this, "Failed to send reset email. Check your email address.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordToggle.setImageResource(R.drawable.ic_eye_closed);
        } else {
            password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            passwordToggle.setImageResource(R.drawable.ic_eye_open);
        }
        isPasswordVisible = !isPasswordVisible;
        password.setSelection(password.length());
    }
}