package com.example.attendanceapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminLoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        initializeViews();

        // Set click listener for login button
        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginAdmin();
            }
        });
    }

    private void initializeViews() {
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);
    }

    private void loginAdmin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        // Validate input fields
        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Email is required");
            editTextEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Password is required");
            editTextPassword.requestFocus();
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Please enter a valid email");
            editTextEmail.requestFocus();
            return;
        }

        // Disable login button during authentication
        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        // Perform Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // Re-enable login button
                        buttonLogin.setEnabled(true);
                        buttonLogin.setText("LOGIN");

                        if (task.isSuccessful()) {
                            // Sign in success
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Check if the logged-in user is the admin
                                String userEmail = user.getEmail();
                                if ("admin@dekut.ac.ke".equals(userEmail)) {
                                    Toast.makeText(AdminLoginActivity.this,
                                            "Login successful", Toast.LENGTH_SHORT).show();

                                    // Clear input fields after successful login
                                    editTextEmail.setText("");
                                    editTextPassword.setText("");
                                } else {
                                    // Sign out if not admin and show error
                                    mAuth.signOut();
                                    Toast.makeText(AdminLoginActivity.this,
                                            "Access denied: Admin credentials required",
                                            Toast.LENGTH_LONG).show();
                                }
                            }
                        } else {
                            // Sign in failed
                            String errorMessage = "Authentication failed";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(AdminLoginActivity.this,
                                    errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && "admin@dekut.ac.ke".equals(currentUser.getEmail())) {
            Toast.makeText(this, "Already logged in as admin", Toast.LENGTH_SHORT).show();
        }
    }
}