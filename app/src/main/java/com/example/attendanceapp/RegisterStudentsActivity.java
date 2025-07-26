package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterStudentsActivity extends AppCompatActivity {

        EditText etFullName, etRegNumber, etPhoneNumber, etStudentEmail, etStudentPassword, etCourse;
        Spinner spinnerDepartment;
        Button btnRegisterStudent;

        FirebaseAuth auth;
        FirebaseFirestore firestore;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_register_students);

                // Initialize views
                etFullName = findViewById(R.id.etFullName);
                etRegNumber = findViewById(R.id.etRegNumber);
                etPhoneNumber = findViewById(R.id.etPhoneNumber);
                etStudentEmail = findViewById(R.id.etStudentEmail);
                etStudentPassword = findViewById(R.id.etStudentPassword);
                etCourse = findViewById(R.id.etCourse);
                spinnerDepartment = findViewById(R.id.spinnerStudentDept);
                btnRegisterStudent = findViewById(R.id.btnRegisterStudent);

                // Initialize Firebase
                auth = FirebaseAuth.getInstance();
                firestore = FirebaseFirestore.getInstance();

                // Setup department spinner
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                        this,
                        R.array.departments_array, // Make sure this array is in res/values/strings.xml
                        android.R.layout.simple_spinner_item
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerDepartment.setAdapter(adapter);

                // Register button click listener
                btnRegisterStudent.setOnClickListener(view -> {
                        String name = etFullName.getText().toString().trim();
                        String regNo = etRegNumber.getText().toString().trim();
                        String phone = etPhoneNumber.getText().toString().trim();
                        String email = etStudentEmail.getText().toString().trim();
                        String password = etStudentPassword.getText().toString().trim();
                        String department = spinnerDepartment.getSelectedItem().toString();
                        String course = etCourse.getText().toString().trim();

                        if (name.isEmpty() || regNo.isEmpty() || phone.isEmpty() || email.isEmpty() || password.isEmpty() || course.isEmpty()) {
                                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                                return;
                        }

                        // Create user in Firebase Authentication
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                                String userId = auth.getCurrentUser().getUid();

                                                // Save student details in Firestore
                                                Map<String, Object> studentData = new HashMap<>();
                                                studentData.put("fullName", name);
                                                studentData.put("regNumber", regNo);
                                                studentData.put("phone", phone);
                                                studentData.put("email", email);
                                                studentData.put("course", course);
                                                studentData.put("department", department);
                                                studentData.put("role", "student");

                                                firestore.collection("students").document(userId)
                                                        .set(studentData)
                                                        .addOnSuccessListener(unused -> {
                                                                Toast.makeText(this, "Student registered successfully", Toast.LENGTH_SHORT).show();
                                                                clearForm();
                                                        })
                                                        .addOnFailureListener(e -> Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                                        } else {
                                                Toast.makeText(this, "Auth Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                });
                });
        }

        private void clearForm() {
                etFullName.setText("");
                etRegNumber.setText("");
                etPhoneNumber.setText("");
                etStudentEmail.setText("");
                etStudentPassword.setText("");
                etCourse.setText("");
                spinnerDepartment.setSelection(0);
        }
}
