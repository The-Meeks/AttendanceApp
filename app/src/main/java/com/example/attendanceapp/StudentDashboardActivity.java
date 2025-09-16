package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome, btnScanQR, btnViewAttendance, btnViewUnits, btnLogout;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI references
        tvWelcome = findViewById(R.id.tvWelcomeStudent);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnLogout = findViewById(R.id.btnLogoutStudent);

        // Display student's name from Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("students").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("full_name");
                            if (fullName != null) {
                                tvWelcome.setText("Welcome, " + fullName);
                            } else {
                                tvWelcome.setText("Welcome, Student");
                            }
                        }
                    })
                    .addOnFailureListener(e -> tvWelcome.setText("Welcome"));
        }

        // Button actions
        btnScanQR.setOnClickListener(v ->
                startActivity(new Intent(this, StudentScanQRActivity.class)));

        btnViewAttendance.setOnClickListener(v ->
                startActivity(new Intent(this, StudentViewAttendanceActivity.class)));

        btnViewUnits.setOnClickListener(v ->
                startActivity(new Intent(this, StudentUnitsActivity.class)));

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, StudentLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
