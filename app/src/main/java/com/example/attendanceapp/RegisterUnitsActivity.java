package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUnitsActivity extends AppCompatActivity {

    private EditText etUnitCode, etUnitName;
    private Spinner spinnerUnitDept, spinnerSemester, spinnerYear;
    private Button btnRegisterUnit;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_units);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        etUnitCode = findViewById(R.id.etUnitCode);
        etUnitName = findViewById(R.id.etUnitName);
        spinnerUnitDept = findViewById(R.id.spinnerUnitDept);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerYear = findViewById(R.id.spinnerYear);
        btnRegisterUnit = findViewById(R.id.btnRegisterUnit);

        // Set up department spinner
        String[] departments = {"Select Department","Computer Science", "Information Technology"};
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerUnitDept.setAdapter(deptAdapter);

        // Set up semester spinner
        String[] semesters = {"Select Semester","Semester 1", "Semester 2"};
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, semesters);
        spinnerSemester.setAdapter(semAdapter);

        // Set up year of study spinner
        String[] years = {"Select Year of Study","Year 1", "Year 2", "Year 3", "Year 4"};
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years);
        spinnerYear.setAdapter(yearAdapter);

        // Register button listener
        btnRegisterUnit.setOnClickListener(v -> registerUnit());
    }

    private void registerUnit() {
        String unitCode = etUnitCode.getText().toString().trim().toUpperCase();
        String unitName = etUnitName.getText().toString().trim();
        String department = spinnerUnitDept.getSelectedItem().toString();
        String semester = spinnerSemester.getSelectedItem().toString();
        String yearOfStudy = spinnerYear.getSelectedItem().toString();

        if (unitCode.isEmpty() || unitName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if unit code already exists
        db.collection("units")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Unit code already exists!", Toast.LENGTH_SHORT).show();
                    } else {
                        saveUnit(unitCode, unitName, department, semester, yearOfStudy);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveUnit(String unitCode, String unitName, String department, String semester, String yearOfStudy) {
        Map<String, Object> unit = new HashMap<>();
        unit.put("unit_code", unitCode);
        unit.put("unit_name", unitName);
        unit.put("department", department);
        unit.put("semester", semester);
        unit.put("year_of_study", yearOfStudy);

        db.collection("units")
                .add(unit)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Unit registered successfully", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void clearFields() {
        etUnitCode.setText("");
        etUnitName.setText("");
        spinnerUnitDept.setSelection(0);
        spinnerSemester.setSelection(0);
        spinnerYear.setSelection(0);
    }
}
