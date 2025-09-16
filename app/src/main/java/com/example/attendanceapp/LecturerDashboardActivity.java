package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LecturerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeLecturer, btnCreateQR, btnViewAttendance, btnViewUnits, btnLogoutLecturer;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        tvWelcomeLecturer = findViewById(R.id.tvWelcomeLecturer); // 🔹 Add this TextView in your XML
        btnCreateQR = findViewById(R.id.btnCreateQR);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnLogoutLecturer = findViewById(R.id.btnLogoutLecturer);

        // Display lecturer's name from Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("lecturers").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("full_name");
                            if (fullName != null) {
                                tvWelcomeLecturer.setText("Welcome, " + fullName);
                            } else {
                                tvWelcomeLecturer.setText("Welcome, Lecturer");
                            }
                        }
                    })
                    .addOnFailureListener(e -> tvWelcomeLecturer.setText("Welcome"));
        }

        // Set click listeners
        btnCreateQR.setOnClickListener(v ->
                startActivity(new Intent(this, LectureGenerateQRActivity.class)));

        btnViewAttendance.setOnClickListener(v ->
                startActivity(new Intent(this, LecturerViewAttendanceActivity.class)));

        btnViewUnits.setOnClickListener(v ->
                startActivity(new Intent(this, ViewUnitsActivity.class)));

        btnLogoutLecturer.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LecturerLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }
}
