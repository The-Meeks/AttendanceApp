package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class LecturerLoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(v -> loginLecturer());
    }

    private void loginLecturer() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

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

        // Disable button during login
        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        buttonLogin.setEnabled(true);
                        buttonLogin.setText("LOGIN");

                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // ✅ Check if user exists in lecturers collection
                                db.collection("lecturers")
                                        .document(user.getUid())
                                        .get()
                                        .addOnSuccessListener((DocumentSnapshot documentSnapshot) -> {
                                            if (documentSnapshot.exists()) {
                                                Toast.makeText(LecturerLoginActivity.this,
                                                        "Login successful", Toast.LENGTH_SHORT).show();

                                                // Open Lecturer Dashboard
                                                Intent intent = new Intent(LecturerLoginActivity.this, LecturerDashboardActivity.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                mAuth.signOut();
                                                Toast.makeText(LecturerLoginActivity.this,
                                                        "Access denied: Not a lecturer",
                                                        Toast.LENGTH_LONG).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(LecturerLoginActivity.this,
                                                    "Error: " + e.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        });
                            }
                        } else {
                            String errorMessage = "Authentication failed";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            Toast.makeText(LecturerLoginActivity.this,
                                    errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Auto login if already authenticated & exists in lecturers collection
            db.collection("lecturers")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            startActivity(new Intent(this, LecturerDashboardActivity.class));
                            finish();
                        }
                    });
        }
    }
}
