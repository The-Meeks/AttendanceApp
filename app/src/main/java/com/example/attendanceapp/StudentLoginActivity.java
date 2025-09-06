package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentLoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_login);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonLogin = findViewById(R.id.buttonLogin);

        buttonLogin.setOnClickListener(view -> loginStudent());
    }

    private void loginStudent() {
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

        buttonLogin.setEnabled(false);
        buttonLogin.setText("Logging in...");

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            buttonLogin.setEnabled(true);
            buttonLogin.setText("Login");

            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // 🔹 Check in students collection
                    db.collection("students")
                            .document(user.getUid())
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                if (documentSnapshot.exists()) {
                                    // ✅ Student found
                                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, StudentDashboardActivity.class));
                                    finish();
                                } else {
                                    // ❌ Not a student
                                    Toast.makeText(this, "Access denied. Not a student.", Toast.LENGTH_LONG).show();
                                    FirebaseAuth.getInstance().signOut();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            });
                }
            } else {
                String errorMessage = "Authentication failed";
                if (task.getException() != null) {
                    errorMessage = task.getException().getMessage();
                }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // 🔹 Double-check in students collection before auto-login
            db.collection("students")
                    .document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            startActivity(new Intent(this, StudentDashboardActivity.class));
                            finish();
                        } else {
                            FirebaseAuth.getInstance().signOut(); // Not a student → logout
                        }
                    });
        }
    }
}
