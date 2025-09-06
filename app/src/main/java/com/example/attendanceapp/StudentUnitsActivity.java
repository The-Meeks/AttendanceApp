package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class StudentUnitsActivity extends AppCompatActivity {

    private ListView listViewUnits;
    private Button btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> unitsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_units);

        listViewUnits = findViewById(R.id.listViewUnits);
        btnBack = findViewById(R.id.btnBack);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        unitsList = new ArrayList<>();

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, unitsList);
        listViewUnits.setAdapter(adapter);

        loadStudentUnits();

        btnBack.setOnClickListener(v -> finish());
    }

    private void loadStudentUnits() {
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("studentUnits")
                .whereEqualTo("student_id", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    unitsList.clear();
                    if (!queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String unitName = doc.getString("unit_name");
                            String unitCode = doc.getString("unit_code");
                            Long year = doc.getLong("year_of_study");
                            Long sem = doc.getLong("semester");

                            unitsList.add(unitCode + " - " + unitName +
                                    " (Year " + year + ", Sem " + sem + ")");
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No units found for you.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }
}
