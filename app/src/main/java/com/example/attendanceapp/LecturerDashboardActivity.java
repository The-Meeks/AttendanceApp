package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LecturerDashboardActivity extends AppCompatActivity {

    private TextView btnCreateQR, btnViewAttendance, btnViewUnits, btnLogoutLecturer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        // Initialize views
        btnCreateQR = findViewById(R.id.btnCreateQR);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnLogoutLecturer = findViewById(R.id.btnLogoutLecturer);

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
