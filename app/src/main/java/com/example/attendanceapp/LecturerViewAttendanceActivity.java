package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class LecturerViewAttendanceActivity extends AppCompatActivity {

    private Spinner unitSpinner;
    private ListView sessionListView;
    private TextView summaryText;
    private Button btnDownload;

    private FirebaseFirestore db;
    private String lecturerId;
    private ArrayList<String> unitList = new ArrayList<>();
    private ArrayList<String> unitCodes = new ArrayList<>();

    private ArrayList<String> sessionDisplayList = new ArrayList<>();
    private ArrayList<String> sessionIds = new ArrayList<>();
    private ArrayAdapter<String> sessionAdapter;

    private int totalEnrolled = 0;
    private int totalAttended = 0;
    private int totalSessions = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_view_attendance);

        unitSpinner = findViewById(R.id.unitSpinner);
        sessionListView = findViewById(R.id.sessionListView);
        summaryText = findViewById(R.id.summaryText);
        btnDownload = findViewById(R.id.btnDownload);

        db = FirebaseFirestore.getInstance();
        lecturerId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Adapter for sessions
        sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, sessionDisplayList);
        sessionListView.setAdapter(sessionAdapter);

        loadLecturerUnits();

        // Handle unit selection
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String unitCode = unitCodes.get(position);
                loadSessions(unitCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Handle session click → show details
        sessionListView.setOnItemClickListener((parent, view, position, id) -> {
            String sessionId = sessionIds.get(position);
            String unitCode = unitCodes.get(unitSpinner.getSelectedItemPosition());

            Intent intent = new Intent(LecturerViewAttendanceActivity.this, SessionAttendanceActivity.class);
            intent.putExtra("unit_code", unitCode);
            intent.putExtra("session_id", sessionId);
            startActivity(intent);
        });

        // Download attendance report (placeholder)
        btnDownload.setOnClickListener(v ->
                Toast.makeText(this, "Download feature coming soon...", Toast.LENGTH_SHORT).show()
        );
    }

    private void loadLecturerUnits() {
        db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", lecturerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    unitList.clear();
                    unitCodes.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String unitCode = doc.getString("unit_code");
                        String unitName = doc.getString("unit_name");
                        unitList.add(unitCode + " - " + unitName);
                        unitCodes.add(unitCode);
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                            android.R.layout.simple_spinner_item, unitList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    unitSpinner.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading units: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSessions(String unitCode) {
        sessionDisplayList.clear();
        sessionIds.clear();
        totalSessions = 0;
        totalAttended = 0;

        db.collection("lecturerSessions")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        sessionAdapter.notifyDataSetChanged();
                        summaryText.setText("No sessions found for this unit.");
                        return;
                    }

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        totalSessions++;
                        String sessionId = doc.getId();
                        sessionIds.add(sessionId);

                        long createdAt = doc.getLong("createdAt") != null ? doc.getLong("createdAt") : 0;
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                        String dateStr = createdAt > 0 ? sdf.format(createdAt) : "Unknown Date";

                        CollectionReference attendanceRef = doc.getReference().collection("attendance");
                        attendanceRef.get().addOnSuccessListener(attSnap -> {
                            int attendedCount = attSnap.size();
                            totalAttended += attendedCount;

                            String displayText = "Session: " + dateStr + "\nAttended: " + attendedCount + " students";
                            sessionDisplayList.add(displayText);
                            sessionAdapter.notifyDataSetChanged();

                            updateSummary(unitCode);
                        });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading sessions: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void updateSummary(String unitCode) {
        db.collection("studentUnits")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    totalEnrolled = querySnapshot.size();
                    int absent = totalEnrolled * totalSessions - totalAttended;
                    double percentage = totalEnrolled > 0 ? (totalAttended * 100.0) / (totalEnrolled * totalSessions) : 0;

                    summaryText.setText("Total Enrolled: " + totalEnrolled +
                            " | Attended: " + totalAttended +
                            " | Absent: " + absent +
                            " | Attendance %: " + String.format(Locale.getDefault(), "%.2f", percentage));
                });
    }
}
