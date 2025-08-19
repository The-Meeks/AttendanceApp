package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private Button btnRegisterStudents, btnRegisterLecturers, btnRegisterUnits,
            btnAllocateUnits, btnEnrollStudents, btnStatistics,
            btnPromotion, btnRemoveUsers, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize buttons
        btnRegisterStudents = findViewById(R.id.btnRegisterStudents);
        btnRegisterLecturers = findViewById(R.id.btnRegisterLecturers);
        btnRegisterUnits = findViewById(R.id.btnRegisterUnits);
        btnAllocateUnits = findViewById(R.id.btnAllocateUnits);
        btnEnrollStudents = findViewById(R.id.btnEnrollStudents);

        btnPromotion = findViewById(R.id.btnPromotion);
        btnRemoveUsers = findViewById(R.id.btnRemoveUsers);
        btnLogout = findViewById(R.id.btnLogout);

        // Set click listeners
        btnRegisterStudents.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterStudentsActivity.class)));

        btnRegisterLecturers.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterLecturersActivity.class)));

        btnRegisterUnits.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterUnitsActivity.class)));

        btnAllocateUnits.setOnClickListener(v ->
                startActivity(new Intent(this, AllocateUnitsActivity.class)));

        btnEnrollStudents.setOnClickListener(v ->
                startActivity(new Intent(this, EnrollCourseActivity.class)));



        btnPromotion.setOnClickListener(v ->
                startActivity(new Intent(this, PromoteCourseActivity.class)));

        btnRemoveUsers.setOnClickListener(v ->
                startActivity(new Intent(this, RemoveUserActivity.class)));

        btnLogout.setOnClickListener(v -> {
            // Go back to login screen
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish(); // close dashboard so user can’t return with back button
        });
    }
}
