package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ViewAttendanceActivity extends AppCompatActivity {

    private ListView listViewAttendance;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_attendance);

        listViewAttendance = findViewById(R.id.listViewAttendance);
        db = FirebaseFirestore.getInstance();

        loadAttendanceData();
    }

    private void loadAttendanceData() {
        db.collection("attendance")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> attendanceList = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String student = document.getString("student");
                        String unit = document.getString("unit");
                        String status = document.getString("status");
                        String date = document.getString("date");
                        attendanceList.add("Student: " + student + "\nUnit: " + unit + "\nStatus: " + status + "\nDate: " + date);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, attendanceList);
                    listViewAttendance.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show());
    }
}
