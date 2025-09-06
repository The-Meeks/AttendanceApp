package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnScanQR, btnViewAttendance, btnViewUnits, btnLogout;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();

        // UI references
        tvWelcome = findViewById(R.id.tvWelcomeStudent);
        btnScanQR = findViewById(R.id.btnScanQR);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnLogout = findViewById(R.id.btnLogoutStudent);

        // Show student email
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            tvWelcome.setText("Welcome, Student");
        }

        // Button actions
        btnScanQR.setOnClickListener(v -> {
            startActivity(new Intent(StudentDashboardActivity.this, StudentScanQRActivity.class));
       });

        btnViewAttendance.setOnClickListener(v -> {
           startActivity(new Intent(StudentDashboardActivity.this, StudentViewAttendanceActivity.class));
       });

        btnViewUnits.setOnClickListener(v -> {
            startActivity(new Intent(StudentDashboardActivity.this, StudentUnitsActivity.class));
        });

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(StudentDashboardActivity.this, StudentLoginActivity.class));
            finish();
        });
    }
}
