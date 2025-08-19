package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class RegisterStudentsActivity extends AppCompatActivity {

    private EditText etFullName, etRegNumber, etPhoneNumber, etEmail, etPassword;
    private Spinner spinnerDept, spinnerCourse, spinnerYear, spinnerSemester;
    private Button btnRegister;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_students);

        etFullName = findViewById(R.id.etFullName);
        etRegNumber = findViewById(R.id.etRegNumber);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        etEmail = findViewById(R.id.etStudentEmail);
        etPassword = findViewById(R.id.etStudentPassword);
        spinnerDept = findViewById(R.id.spinnerStudentDept);
        spinnerCourse = findViewById(R.id.spinnerStudentCourse);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnRegister = findViewById(R.id.btnRegisterStudent);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // setup department spinner
        SpinnerUtils.setupDepartmentSpinner(this, spinnerDept);

        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                SpinnerUtils.setupCourseSpinner(RegisterStudentsActivity.this, spinnerCourse, spinnerDept.getSelectedItem().toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // setup year of study spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Year", "1", "2", "3", "4"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // setup semester spinner
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Semester", "1", "2"});
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semAdapter);

        btnRegister.setOnClickListener(v -> registerStudent());
    }

    private void registerStudent() {
        String fullName = etFullName.getText().toString().trim();
        String regNumber = etRegNumber.getText().toString().trim();
        String phone = etPhoneNumber.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String department = spinnerDept.getSelectedItem().toString();
        String course = spinnerCourse.getSelectedItem().toString();
        String yearSelected = spinnerYear.getSelectedItem().toString();
        String semesterSelected = spinnerSemester.getSelectedItem().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (yearSelected.equals("Select Year") || semesterSelected.equals("Select Semester")) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        int yearOfStudy = Integer.parseInt(yearSelected);
        int semester = Integer.parseInt(semesterSelected);

        mAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener(authResult -> {
            String uid = mAuth.getCurrentUser().getUid();
            HashMap<String, Object> student = new HashMap<>();
            student.put("full_name", fullName);
            student.put("reg_number", regNumber);
            student.put("phone_number", phone);
            student.put("email", email);
            student.put("department", department);
            student.put("course", course);
            student.put("year_of_study", yearOfStudy);
            student.put("semester", semester);
            student.put("uid", uid);

            db.collection("students").document(uid).set(student)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Student registered", Toast.LENGTH_SHORT).show();
                        etFullName.setText("");
                        etRegNumber.setText("");
                        etPhoneNumber.setText("");
                        etEmail.setText("");
                        etPassword.setText("");
                        spinnerYear.setSelection(0);
                        spinnerSemester.setSelection(0);
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e -> Toast.makeText(this, "Auth failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
