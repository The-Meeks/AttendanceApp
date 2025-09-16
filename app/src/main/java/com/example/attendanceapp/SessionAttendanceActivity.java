package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Locale;

public class SessionAttendanceActivity extends AppCompatActivity {

    private TextView sessionSummary;
    private ListView studentListView;

    private FirebaseFirestore db;
    private String unitCode, sessionId;
    private ArrayList<String> studentList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private int totalEnrolled = 0;
    private int totalAttended = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_attendance);

        sessionSummary = findViewById(R.id.sessionSummary);
        studentListView = findViewById(R.id.studentListView);

        db = FirebaseFirestore.getInstance();
        unitCode = getIntent().getStringExtra("unit_code");
        sessionId = getIntent().getStringExtra("session_id");

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, studentList);
        studentListView.setAdapter(adapter);

        loadAttendance();
    }

    private void loadAttendance() {
        db.collection("lecturerSessions")
                .document(sessionId)
                .collection("attendance")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    totalAttended = querySnapshot.size();
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "No students attended this session", Toast.LENGTH_SHORT).show();
                    }
                    studentList.clear();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String fullName = doc.getString("full_name");
                        String regNumber = doc.getString("reg_number");
                        studentList.add(fullName + " (" + regNumber + ")");
                    }
                    adapter.notifyDataSetChanged();
                    loadSummary();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading attendance: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void loadSummary() {
        db.collection("studentUnits")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    totalEnrolled = querySnapshot.size();
                    int absent = totalEnrolled - totalAttended;
                    double percentage = totalEnrolled > 0 ? (totalAttended * 100.0) / totalEnrolled : 0;

                    sessionSummary.setText("Enrolled: " + totalEnrolled +
                            " | Attended: " + totalAttended +
                            " | Absent: " + absent +
                            " | %: " + String.format(Locale.getDefault(), "%.2f", percentage));
                });
    }
}
