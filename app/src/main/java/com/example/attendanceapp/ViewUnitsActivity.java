package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class ViewUnitsActivity extends AppCompatActivity {

    private ListView listViewUnits;
    private TextView textTitle;
    private Button btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayList<String> unitsList;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_units);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI references
        listViewUnits = findViewById(R.id.listViewUnits);
        textTitle = findViewById(R.id.textTitle);
        btnBack = findViewById(R.id.btnBack);

        // Setup list
        unitsList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, unitsList);
        listViewUnits.setAdapter(adapter);

        // Get current lecturer UID
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String lecturerUid = currentUser.getUid();

            // Debugging: Show UID
            Toast.makeText(this, "Logged in UID: " + lecturerUid, Toast.LENGTH_LONG).show();

            loadLecturerUnits(lecturerUid);
        } else {
            Toast.makeText(this, "No lecturer logged in", Toast.LENGTH_SHORT).show();
        }

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadLecturerUnits(String lecturerUid) {
        db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", lecturerUid)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        unitsList.clear();

                        for (QueryDocumentSnapshot document : task.getResult()) {
                            String unitName = document.getString("unit_name");
                            String unitCode = document.getString("unit_code");
                            String semester = document.getString("semester");
                            Long year = document.getLong("year_of_study");

                            String unitDetails = unitCode + " - " + unitName +
                                    "\nYear: " + year + ", " + semester;
                            unitsList.add(unitDetails);
                        }

                        if (unitsList.isEmpty()) {
                            unitsList.add("No units found for this lecturer.");
                        }

                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error loading units", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
