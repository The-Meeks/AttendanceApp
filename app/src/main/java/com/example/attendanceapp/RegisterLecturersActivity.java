package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterLecturersActivity extends AppCompatActivity {

    private EditText etLecturerName, etLecturerPhone, etLecturerPFNumber, etLecturerEmail, etLecturerPassword;
    private Spinner spinnerLecturerDept;
    private Button btnRegisterLecturer;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_lecturers);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize Views
        etLecturerName = findViewById(R.id.etLecturerName);
        etLecturerPhone = findViewById(R.id.etLecturerPhone);
        etLecturerPFNumber = findViewById(R.id.etLecturerPFNumber);
        etLecturerEmail = findViewById(R.id.etLecturerEmail);
        etLecturerPassword = findViewById(R.id.etLecturerPassword);
        spinnerLecturerDept = findViewById(R.id.spinnerLecturerDept);
        btnRegisterLecturer = findViewById(R.id.btnRegisterLecturer);

        // Set up department spinner
        String[] departments = {"Computer Science", "Information Technology"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerLecturerDept.setAdapter(adapter);

        // Button Click Listener
        btnRegisterLecturer.setOnClickListener(v -> registerLecturer());
    }

    private void registerLecturer() {
        String fullName = etLecturerName.getText().toString().trim();
        String phone = etLecturerPhone.getText().toString().trim();
        String pfNumber = etLecturerPFNumber.getText().toString().trim();
        String email = etLecturerEmail.getText().toString().trim();
        String password = etLecturerPassword.getText().toString().trim();
        String department = spinnerLecturerDept.getSelectedItem().toString();

        // Basic validation
        if (fullName.isEmpty() || phone.isEmpty() || pfNumber.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Register with Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Save lecturer info to Firestore
                            Map<String, Object> lecturerData = new HashMap<>();
                            lecturerData.put("uid", user.getUid());
                            lecturerData.put("full_name", fullName);
                            lecturerData.put("phone_number", phone);
                            lecturerData.put("pf_number", pfNumber);
                            lecturerData.put("email", email);
                            lecturerData.put("department", department);
                            lecturerData.put("role", "lecturer");

                            db.collection("lecturers")
                                    .document(user.getUid())
                                    .set(lecturerData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, "Lecturer registered successfully", Toast.LENGTH_SHORT).show();
                                        clearFields();
                                    })
                                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void clearFields() {
        etLecturerName.setText("");
        etLecturerPhone.setText("");
        etLecturerPFNumber.setText("");
        etLecturerEmail.setText("");
        etLecturerPassword.setText("");
        spinnerLecturerDept.setSelection(0);
    }
}
