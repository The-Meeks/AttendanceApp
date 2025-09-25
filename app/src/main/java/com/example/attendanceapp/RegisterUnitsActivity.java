package com.example.attendanceapp;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RegisterUnitsActivity extends AppCompatActivity {

    private EditText etUnitCode, etUnitName, etStartTime, etEndTime;
    private Spinner spinnerUnitDept, spinnerSemester, spinnerYear, spinnerDayOfWeek;
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
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        spinnerUnitDept = findViewById(R.id.spinnerUnitDept);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerDayOfWeek = findViewById(R.id.spinnerDayOfWeek);
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

        // Set up day of week spinner
        String[] days = {"Select Day of Week", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        // Register button listener
        btnRegisterUnit.setOnClickListener(v -> registerUnit());

        // Time pickers
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));
    }

    private void registerUnit() {
        String unitCode = etUnitCode.getText().toString().trim().toUpperCase();
        String unitName = etUnitName.getText().toString().trim();
        String department = spinnerUnitDept.getSelectedItem().toString();
        String semester = spinnerSemester.getSelectedItem().toString();
        String yearOfStudy = spinnerYear.getSelectedItem().toString();
        String dayOfWeek = spinnerDayOfWeek.getSelectedItem().toString();
        String startTime = etStartTime.getText().toString().trim();
        String endTime = etEndTime.getText().toString().trim();

        if (unitCode.isEmpty() || unitName.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || dayOfWeek.startsWith("Select")) {
            Toast.makeText(this, "Please fill in all fields including day and allocated time", Toast.LENGTH_SHORT).show();
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
                        saveUnit(unitCode, unitName, department, semester, yearOfStudy, dayOfWeek, startTime, endTime);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void saveUnit(String unitCode, String unitName, String department, String semester, String yearOfStudy,
                          String dayOfWeek, String startTime, String endTime) {
        Map<String, Object> unit = new HashMap<>();
        unit.put("unit_code", unitCode);
        unit.put("unit_name", unitName);
        unit.put("department", department);
        unit.put("semester", semester);
        unit.put("year_of_study", yearOfStudy);
        unit.put("schedule_day", dayOfWeek);
        unit.put("allocated_start_time", startTime);
        unit.put("allocated_end_time", endTime);

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
        etStartTime.setText("");
        etEndTime.setText("");
        spinnerUnitDept.setSelection(0);
        spinnerSemester.setSelection(0);
        spinnerYear.setSelection(0);
        spinnerDayOfWeek.setSelection(0);
    }

    private void showTimePicker(EditText target) {
        // Default to 11:00 AM
        int defaultHour = 11;
        int defaultMinute = 0;

        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            try {
                // Format to 12-hour with AM/PM
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
                cal.set(java.util.Calendar.MINUTE, minute);
                SimpleDateFormat fmt = new SimpleDateFormat("hh:mma", Locale.getDefault());
                target.setText(fmt.format(cal.getTime()));
            } catch (Exception ignored) { }
        }, defaultHour, defaultMinute, false);
        dialog.show();
    }
}
