package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class LecturerDashboardActivity extends AppCompatActivity {

    private Button btnCreateQR, btnViewUnits, btnViewAttendance, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        mAuth = FirebaseAuth.getInstance();

        // Initialize
        btnCreateQR = findViewById(R.id.btnCreateQR);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnLogout = findViewById(R.id.btnLogoutLecturer);

        // Handle button clicks
        btnCreateQR.setOnClickListener(v -> {
            startActivity(new Intent(this, LectureGenerateQRActivity.class));
        });

        btnViewUnits.setOnClickListener(v -> {
            startActivity(new Intent(this, ViewUnitsActivity.class));
       });

       // btnViewAttendance.setOnClickListener(v -> {
         //   startActivity(new Intent(this, ViewAttendanceActivity.class));
       // });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(LecturerDashboardActivity.this, LecturerLoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close dashboard so back press won't return here
        });
    }
}
