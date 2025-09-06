package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class StudentViewAttendanceActivity extends AppCompatActivity {

    private Spinner spinnerUnits;
    private TextView tvPercentage, tvTotal, tvAttended, tvMissed;
    private ProgressBar progressAttendance;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private List<String> unitDisplayList = new ArrayList<>();
    private HashMap<String, String> unitMap = new HashMap<>(); // unitCode -> unitName
    private String studentId;

    private static final int TOTAL_SESSIONS = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view_attendance);

        spinnerUnits = findViewById(R.id.spinnerUnits);
        tvPercentage = findViewById(R.id.tvPercentage);
        tvTotal = findViewById(R.id.tvTotal);
        tvAttended = findViewById(R.id.tvAttended);
        tvMissed = findViewById(R.id.tvMissed);
        progressAttendance = findViewById(R.id.progressAttendance);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        studentId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        loadUnits();

        spinnerUnits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                String selectedDisplay = unitDisplayList.get(position);
                String unitCode = selectedDisplay.split(" - ")[0]; // extract code before dash
                loadAttendance(unitCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing
            }
        });
    }

    private void loadUnits() {
        db.collection("studentUnits")
                .whereEqualTo("student_id", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    unitDisplayList.clear();
                    unitMap.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String unitCode = doc.getString("unit_code");
                        String unitName = doc.getString("unit_name");
                        if (unitCode != null && unitName != null) {
                            unitMap.put(unitCode, unitName);
                            unitDisplayList.add(unitCode + " - " + unitName);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            StudentViewAttendanceActivity.this,
                            android.R.layout.simple_spinner_item,
                            unitDisplayList
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUnits.setAdapter(adapter);

                    if (!unitDisplayList.isEmpty()) {
                        String firstUnitCode = unitDisplayList.get(0).split(" - ")[0];
                        loadAttendance(firstUnitCode);
                    }
                });
    }

    private void loadAttendance(String unitCode) {
        if (studentId.isEmpty()) return;

        db.collection("attendance")
                .whereEqualTo("student_id", studentId)
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(attSnapshots -> {
                    int attended = attSnapshots.size();
                    int missed = TOTAL_SESSIONS - attended;

                    double percentage = (attended * 100.0) / TOTAL_SESSIONS;
                    if (attended >= TOTAL_SESSIONS) {
                        percentage = 100.0; // ✅ cap at 100%
                    }

                    // Update UI
                    tvPercentage.setText(String.format(Locale.getDefault(), "%.2f%%", percentage));
                    progressAttendance.setProgress((int) percentage);

                    tvTotal.setText("Total Sessions: " + TOTAL_SESSIONS);
                    tvAttended.setText("Lectures Attended: " + attended);
                    tvMissed.setText("Lectures Missed: " + missed);

                    // 🔹 Change progress color
                    if (percentage < 50) {
                        progressAttendance.getProgressDrawable().setTint(
                                getResources().getColor(android.R.color.holo_red_dark)
                        );
                    } else if (percentage < 75) {
                        progressAttendance.getProgressDrawable().setTint(
                                getResources().getColor(android.R.color.holo_orange_light)
                        );
                    } else {
                        progressAttendance.getProgressDrawable().setTint(
                                getResources().getColor(android.R.color.holo_green_dark)
                        );
                    }
                });
    }
}
