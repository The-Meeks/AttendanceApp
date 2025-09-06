package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class StudentViewAttendanceActivity extends AppCompatActivity {

    private Spinner unitSpinner;
    private ProgressBar attendanceProgress;
    private TextView attendanceSummary;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<String> unitList = new ArrayList<>();
    private List<String> unitCodes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view_attendance);

        unitSpinner = findViewById(R.id.unitSpinner);
        attendanceProgress = findViewById(R.id.attendanceProgress);
        attendanceSummary = findViewById(R.id.attendanceSummary);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadStudentUnits();
    }

    private void loadStudentUnits() {
        String studentId = mAuth.getCurrentUser().getUid();
        CollectionReference unitsRef = db.collection("studentUnits");

        unitsRef.whereEqualTo("student_id", studentId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        unitList.clear();
                        unitCodes.clear();

                        // Add default option
                        unitList.add("Select a unit to view attendance");
                        unitCodes.add(""); // placeholder for default

                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String unitName = doc.getString("unit_name");
                            String unitCode = doc.getString("unit_code");
                            if (unitName != null && unitCode != null) {
                                unitList.add(unitCode + " - " + unitName);
                                unitCodes.add(unitCode);
                            }
                        }

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                StudentViewAttendanceActivity.this,
                                android.R.layout.simple_spinner_item,
                                unitList
                        );
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        unitSpinner.setAdapter(adapter);

                        // Listener for unit selection
                        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                if (position > 0 && position < unitCodes.size()) {
                                    loadAttendanceForUnit(unitCodes.get(position));
                                } else {
                                    attendanceSummary.setText("Select a unit to view attendance");
                                    attendanceProgress.setProgress(0);
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {}
                        });
                    }
                });
    }

    private void loadAttendanceForUnit(String unitCode) {
        String studentId = mAuth.getCurrentUser().getUid();

        // Fetch sessions for the unit
        db.collection("sessions")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        int plannedSessions = task.getResult().size();
                        if (plannedSessions == 0) {
                            attendanceSummary.setText("No sessions found for this unit.");
                            attendanceProgress.setProgress(0);
                            return;
                        }

                        // Now check attendance
                        db.collection("attendance")
                                .whereEqualTo("student_id", studentId)
                                .whereEqualTo("unit_code", unitCode)
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> attendanceTask) {
                                        if (attendanceTask.isSuccessful() && attendanceTask.getResult() != null) {
                                            int attended = attendanceTask.getResult().size();
                                            int missed = plannedSessions - attended;
                                            int remaining = plannedSessions - (attended + missed);

                                            int percentage = (int) (((double) attended / plannedSessions) * 100);

                                            String summary = "Unit: " + unitCode +
                                                    "\nPlanned Sessions: " + plannedSessions +
                                                    "\nAttended: " + attended +
                                                    "\nMissed: " + missed +
                                                    "\nRemaining: " + (remaining < 0 ? 0 : remaining) +
                                                    "\nAttendance %: " + percentage + "%";

                                            attendanceSummary.setText(summary);
                                            attendanceProgress.setProgress(percentage);
                                        }
                                    }
                                });
                    }
                });
    }
}
