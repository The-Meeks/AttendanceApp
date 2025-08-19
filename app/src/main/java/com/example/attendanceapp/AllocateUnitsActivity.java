package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class AllocateUnitsActivity extends AppCompatActivity {

    private Spinner spinnerLecturer, spinnerUnit, spinnerYear, spinnerSemester;
    private Button btnAllocate;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_allocate_units);

        spinnerLecturer = findViewById(R.id.spinnerLecturer);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnAllocate = findViewById(R.id.btnAllocate);

        db = FirebaseFirestore.getInstance();

        loadLecturers();
        loadUnits();

        // Year spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Year", "1", "2", "3", "4"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Semester spinner
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Semester", "Semester 1", "Semester 2"});
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semAdapter);

        btnAllocate.setOnClickListener(v -> allocateUnit());
    }

    private void loadLecturers() {
        db.collection("lecturers").get().addOnSuccessListener(queryDocumentSnapshots -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Placeholder
            adapter.add("Select Lecturer");

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("full_name");
                String id = doc.getId(); // lecturer UID
                adapter.add(name + " (" + id + ")");
            }
            spinnerLecturer.setAdapter(adapter);
        });
    }

    private void loadUnits() {
        db.collection("units").get().addOnSuccessListener(queryDocumentSnapshots -> {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            // Placeholder
            adapter.add("Select Unit");

            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                String unitName = doc.getString("unit_name");
                String code = doc.getString("unit_code");
                adapter.add(code + " - " + unitName);
            }
            spinnerUnit.setAdapter(adapter);
        });
    }

    private void allocateUnit() {
        if (spinnerLecturer.getSelectedItem() == null ||
                spinnerUnit.getSelectedItem() == null ||
                spinnerYear.getSelectedItemPosition() == 0 ||
                spinnerSemester.getSelectedItemPosition() == 0) {

            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String lecturerItem = spinnerLecturer.getSelectedItem().toString();
        if (lecturerItem.equals("Select Lecturer")) {
            Toast.makeText(this, "Please select a lecturer", Toast.LENGTH_SHORT).show();
            return;
        }

        String unitItem = spinnerUnit.getSelectedItem().toString();
        if (unitItem.equals("Select Unit")) {
            Toast.makeText(this, "Please select a unit", Toast.LENGTH_SHORT).show();
            return;
        }

        String lecturerId = lecturerItem.substring(lecturerItem.indexOf("(") + 1, lecturerItem.indexOf(")"));
        String unitCode = unitItem.split(" - ")[0];
        String unitName = unitItem.split(" - ")[1];

        int yearOfStudy = Integer.parseInt(spinnerYear.getSelectedItem().toString());
        String semester = spinnerSemester.getSelectedItem().toString();

        HashMap<String, Object> allocation = new HashMap<>();
        allocation.put("lecturer_id", lecturerId);
        allocation.put("unit_code", unitCode);
        allocation.put("unit_name", unitName);
        allocation.put("year_of_study", yearOfStudy);
        allocation.put("semester", semester);

        db.collection("lecturerUnits").add(allocation)
                .addOnSuccessListener(unused -> Toast.makeText(this, "Unit allocated!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
