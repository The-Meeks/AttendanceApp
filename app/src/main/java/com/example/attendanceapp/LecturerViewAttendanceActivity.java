package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class LecturerViewAttendanceActivity extends AppCompatActivity {

    private Spinner unitSpinner;
    private ListView sessionListView;
    private TextView summaryText;
    private Button btnDownload;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private ArrayList<String> unitDisplayList = new ArrayList<>();
    private ArrayList<String> unitCodeList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;
    private ArrayAdapter<String> attendanceAdapter;

    private ArrayList<String> attendanceList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_view_attendance);

        unitSpinner = findViewById(R.id.unitSpinner);
        sessionListView = findViewById(R.id.sessionListView);
        summaryText = findViewById(R.id.summaryText);
        btnDownload = findViewById(R.id.btnDownload);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Spinner adapter
        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, unitDisplayList);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(spinnerAdapter);

        // ListView adapter
        attendanceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendanceList);
        sessionListView.setAdapter(attendanceAdapter);

        loadLecturerUnits();

        unitSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> adapterView, android.view.View view, int position, long id) {
                if (position == 0) {
                    attendanceList.clear();
                    attendanceAdapter.notifyDataSetChanged();
                    summaryText.setText("Select a unit to view attendance");
                } else {
                    String selectedUnitCode = unitCodeList.get(position);
                    loadAttendance(selectedUnitCode);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> adapterView) {}
        });

        btnDownload.setOnClickListener(v -> {
            // Later: implement CSV/PDF export
            Toast.makeText(this, "Download feature coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadLecturerUnits() {
        unitDisplayList.clear();
        unitCodeList.clear();

        unitDisplayList.add("Select a unit to view attendance"); // default
        unitCodeList.add(""); // placeholder

        String lecturerId = auth.getCurrentUser().getUid();

        db.collection("lecturerSessions")
                .whereEqualTo("lecturer_id", lecturerId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String unitCode = doc.getString("unit_code");
                            String unitName = doc.getString("unit_name");
                            if (unitCode != null && unitName != null) {
                                unitDisplayList.add(unitName + " (" + unitCode + ")");
                                unitCodeList.add(unitCode);
                            }
                        }
                        spinnerAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Failed to load units", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadAttendance(String unitCode) {
        attendanceList.clear();

        db.collection("attendance")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        int attended = 0;
                        int total = 0;

                        if (task.getResult().isEmpty()) {
                            attendanceList.add("No attendance records found");
                        } else {
                            for (DocumentSnapshot doc : task.getResult()) {
                                String name = doc.getString("full_name");
                                String reg = doc.getString("reg_number");
                                Long timestamp = doc.getLong("timestamp");

                                String time = "";
                                if (timestamp != null) {
                                    time = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                            .format(new Date(timestamp));
                                }

                                attendanceList.add(name + " (" + reg + ")\nTime: " + time);

                                attended++;
                                total++; // here total = attended (unless you also store absentees separately)
                            }
                        }

                        // Update summary
                        int enrolled = 40; // TODO: fetch actual total enrolled from "students" collection
                        int absent = enrolled - attended;
                        double percent = enrolled > 0 ? (attended * 100.0 / enrolled) : 0;

                        summaryText.setText("Total Enrolled: " + enrolled +
                                " | Attended: " + attended +
                                " | Absent: " + absent +
                                " | Attendance %: " + String.format(Locale.getDefault(), "%.1f", percent) + "%");

                        attendanceAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error loading attendance", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
