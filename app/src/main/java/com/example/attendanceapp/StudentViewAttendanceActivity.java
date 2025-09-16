package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;

public class StudentViewAttendanceActivity extends AppCompatActivity {

    private Spinner unitSpinner;
    private ProgressBar attendanceProgress;
    private TextView attendanceSummary;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private ArrayList<String> unitList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view_attendance);

        unitSpinner = findViewById(R.id.unitSpinner);
        attendanceProgress = findViewById(R.id.attendanceProgress);
        attendanceSummary = findViewById(R.id.attendanceSummary);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadStudentUnits();
    }

    private void loadStudentUnits() {
        db.collection("attendance")
                .whereEqualTo("student_id", currentUser.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    HashSet<String> uniqueUnits = new HashSet<>();
                    for (QueryDocumentSnapshot doc : query) {
                        String unit = doc.getString("unit_code");
                        if (unit != null) uniqueUnits.add(unit);
                    }

                    unitList.clear();
                    unitList.addAll(uniqueUnits);

                    if (unitList.isEmpty()) {
                        unitList.add("No attendance records");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, unitList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    unitSpinner.setAdapter(adapter);

                    unitSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                            if (!unitList.get(position).equals("No attendance records")) {
                                calculateAttendance(unitList.get(position));
                            } else {
                                attendanceSummary.setText("Select a unit to view attendance");
                            }
                        }

                        @Override
                        public void onNothingSelected(android.widget.AdapterView<?> parent) { }
                    });
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load units", Toast.LENGTH_SHORT).show()
                );
    }

    private void calculateAttendance(String unitCode) {
        db.collection("attendance")
                .whereEqualTo("student_id", currentUser.getUid())
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(query -> {
                    int attended = query.size(); // how many times student attended
                    int total = attended; // replace with real total sessions count if you track it

                    int percentage = (total > 0) ? (attended * 100 / total) : 0;

                    attendanceProgress.setProgress(percentage);
                    attendanceSummary.setText("Unit: " + unitCode +
                            "\nAttended: " + attended +
                            " | Total: " + total +
                            " | Attendance %: " + percentage + "%");
                });
    }
}
