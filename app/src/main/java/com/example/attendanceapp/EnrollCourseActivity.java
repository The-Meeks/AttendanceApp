package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.*;

import java.util.HashMap;

public class EnrollCourseActivity extends AppCompatActivity {

    private Spinner spinnerDept, spinnerCourse, spinnerYear, spinnerSemester, spinnerUnit;
    private Button btnEnroll;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_students);

        spinnerDept = findViewById(R.id.spinnerDept);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerYear = findViewById(R.id.spinnerYear);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        btnEnroll = findViewById(R.id.btnEnrollCourse);

        db = FirebaseFirestore.getInstance();

        SpinnerUtils.setupDepartmentSpinner(this, spinnerDept);
        spinnerDept.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                SpinnerUtils.setupCourseSpinner(EnrollCourseActivity.this, spinnerCourse,
                        spinnerDept.getSelectedItem().toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Select Year", "1", "2", "3", "4"});
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Select Semester", "1", "2"});
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        loadUnits();

        btnEnroll.setOnClickListener(v -> enrollCourse());
    }

    private void loadUnits() {
        db.collection("units").get()
                .addOnSuccessListener(query -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    for (DocumentSnapshot doc : query) {
                        String code = doc.getString("unit_code");
                        String name = doc.getString("unit_name");
                        adapter.add(code + " - " + name);
                    }
                    spinnerUnit.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load units", Toast.LENGTH_SHORT).show());
    }

    private void enrollCourse() {
        String department = spinnerDept.getSelectedItem().toString();
        String course = spinnerCourse.getSelectedItem().toString();
        String year = spinnerYear.getSelectedItem().toString();
        String semester = spinnerSemester.getSelectedItem().toString();

        if (year.equals("Select Year") || semester.equals("Select Semester")) {
            Toast.makeText(this, "Please select year and semester", Toast.LENGTH_SHORT).show();
            return;
        }

        String unitText = spinnerUnit.getSelectedItem().toString();
        String[] unitSplit = unitText.split(" - ");
        String unitCode = unitSplit[0];
        String unitName = unitSplit[1];

        int yearOfStudy = Integer.parseInt(year);
        int semesterNum = Integer.parseInt(semester);

        db.collection("students")
                .whereEqualTo("department", department)
                .whereEqualTo("course", course)
                .whereEqualTo("year_of_study", yearOfStudy)
                .whereEqualTo("semester", semesterNum)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        Toast.makeText(this,
                                "No students found for this course/year/semester",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    for (DocumentSnapshot doc : query) {
                        String uid = doc.getString("uid");
                        if (uid != null) {
                            db.collection("studentUnits")
                                    .whereEqualTo("student_id", uid)
                                    .whereEqualTo("unit_code", unitCode)
                                    .get()
                                    .addOnSuccessListener(existing -> {
                                        if (existing.isEmpty()) {
                                            HashMap<String, Object> enrollment = new HashMap<>();
                                            enrollment.put("student_id", uid);
                                            enrollment.put("unit_code", unitCode);
                                            enrollment.put("unit_name", unitName);
                                            enrollment.put("department", department);
                                            enrollment.put("course", course);
                                            enrollment.put("year_of_study", yearOfStudy);
                                            enrollment.put("semester", semesterNum);

                                            db.collection("studentUnits").add(enrollment);
                                        }
                                    });
                        }
                    }

                    Toast.makeText(this,
                            "Enrolled all " + course + " (Year " + year + ", Sem " + semester + ") to " + unitName,
                            Toast.LENGTH_LONG).show();
                });
    }
}
