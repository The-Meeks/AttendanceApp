package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class PromoteCourseActivity extends AppCompatActivity {

    private Spinner spinnerDept, spinnerCourse, spinnerYear, spinnerSemester;
    private Button btnPromoteCourse;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promote_students);

        spinnerDept = findViewById(R.id.spinnerDept);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        btnPromoteCourse = findViewById(R.id.btnPromoteCourse);

        db = FirebaseFirestore.getInstance();

        // Department spinner
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Department", "Computer Science", "Information Technology"});
        deptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDept.setAdapter(deptAdapter);

        // Course spinner (changes based on department)
        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedDept = spinnerDept.getSelectedItem().toString();
                if (selectedDept.equals("Computer Science")) {
                    setCourseAdapter(new String[]{"Select Course", "BSc Computer Science"});
                } else if (selectedDept.equals("Information Technology")) {
                    setCourseAdapter(new String[]{"Select Course", "BSc Information Technology", "BSc Business IT"});
                } else {
                    setCourseAdapter(new String[]{"Select Course"});
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });

        // Year spinner
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Year", "1", "2", "3", "4"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        // Semester spinner
        ArrayAdapter<String> semAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Select Semester", "1", "2"});
        semAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semAdapter);

        // Button action
        btnPromoteCourse.setOnClickListener(v -> promoteWholeCourse());
    }

    private void setCourseAdapter(String[] courses) {
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                courses);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);
    }

    private void promoteWholeCourse() {
        String dept = spinnerDept.getSelectedItem().toString();
        String course = spinnerCourse.getSelectedItem().toString();
        String year = spinnerYear.getSelectedItem().toString();
        String sem = spinnerSemester.getSelectedItem().toString();

        if (dept.equals("Select Department") || course.equals("Select Course")
                || year.equals("Select Year") || sem.equals("Select Semester")) {
            Toast.makeText(this, "Please select all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int currentYear = Integer.parseInt(year);
        int currentSemester = Integer.parseInt(sem);

        // Promotion logic: if sem=1 -> sem=2 (same year), else sem=1 next year
        int nextYear = currentYear;
        int nextSem;

        if (currentSemester == 1) {
            nextSem = 2;
        } else {
            nextSem = 1;
            nextYear = currentYear + 1; // Move to next year if semester is 2
        }

        int finalNextYear = nextYear;
        int finalNextSem = nextSem;

        db.collection("students")
                .whereEqualTo("department", dept)
                .whereEqualTo("course", course)
                .whereEqualTo("year_of_study", currentYear)
                .whereEqualTo("semester", currentSemester)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(this, "No students found to promote", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        DocumentReference ref = doc.getReference();
                        ref.update("year_of_study", finalNextYear,
                                "semester", finalNextSem);
                    }

                    Toast.makeText(this, "Promotion successful", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
