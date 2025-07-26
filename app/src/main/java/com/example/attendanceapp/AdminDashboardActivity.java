package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private Button btnRegisterStudents, btnRegisterLecturers, btnRegisterUnits, btnViewAttendance, btnRemoveUsers, btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvWelcome = findViewById(R.id.tvWelcome);
        btnRegisterStudents = findViewById(R.id.btnRegisterStudents);
        btnRegisterLecturers = findViewById(R.id.btnRegisterLecturers);
        btnRegisterUnits = findViewById(R.id.btnRegisterUnits);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnRemoveUsers = findViewById(R.id.btnRemoveUsers);
        btnLogout = findViewById(R.id.btnLogout);

        tvWelcome.setText("Welcome, Admin");

        btnRegisterStudents.setOnClickListener(v -> startActivity(new Intent(this, RegisterStudentsActivity.class)));
        btnRegisterLecturers.setOnClickListener(v -> startActivity(new Intent(this, RegisterLecturersActivity.class)));
        btnRegisterUnits.setOnClickListener(v -> startActivity(new Intent(this, RegisterUnitsActivity.class)));
        btnViewAttendance.setOnClickListener(v -> startActivity(new Intent(this, ViewAttendanceActivity.class)));
        btnRemoveUsers.setOnClickListener(v -> startActivity(new Intent(this, RemoveUserActivity.class)));

        btnLogout.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        });
    }
}
