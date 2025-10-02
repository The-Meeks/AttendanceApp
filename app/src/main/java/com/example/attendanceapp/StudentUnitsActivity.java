package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class StudentUnitsActivity extends AppCompatActivity {

    private ListView listViewUnits;
    private Button btnBack;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ArrayAdapter<String> adapter;
    private List<String> unitsList;
    private ListenerRegistration unitsListener;
    private ListenerRegistration catalogListener;
    private final java.util.Set<String> activeUnitCodes = new java.util.HashSet<>();
    private com.google.firebase.firestore.QuerySnapshot lastUnitsSnap;

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
        btnBack.setOnClickListener(v -> finish());
    }

    private void startUnitsListener(String uid) {
        stopUnitsListener();

        // Listen to catalog 'units' to know which unit_codes still exist
        catalogListener = db.collection("units").addSnapshotListener((catalog, ce) -> {
            activeUnitCodes.clear();
            if (catalog != null) {
                for (DocumentSnapshot d : catalog.getDocuments()) {
                    String code = d.getString("unit_code");
                    if (code != null) activeUnitCodes.add(code);
                }
            }
            // Debug: indicate catalog changed
            // Toast.makeText(this, "Units catalog updated", Toast.LENGTH_SHORT).show();
            rebuildFromSnapshots();
        });

        unitsListener = db.collection("studentUnits")
                .whereEqualTo("student_id", uid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    lastUnitsSnap = snap;
                    // Debug: indicate studentUnits changed
                    // Toast.makeText(this, "Your units updated", Toast.LENGTH_SHORT).show();
                    rebuildFromSnapshots();
                });
    }

    private void rebuildFromSnapshots() {
        unitsList.clear();
        if (lastUnitsSnap != null && !lastUnitsSnap.isEmpty()) {
            java.util.Map<String, String> unique = new java.util.LinkedHashMap<>();
            for (DocumentSnapshot doc : lastUnitsSnap.getDocuments()) {
                String unitName = doc.getString("unit_name");
                String unitCode = doc.getString("unit_code");
                Long year = doc.getLong("year_of_study");
                Long sem = doc.getLong("semester");

                if (unitCode != null && !activeUnitCodes.isEmpty() && !activeUnitCodes.contains(unitCode)) {
                    continue; // hide units removed from catalog
                }

                String safeCode = unitCode != null ? unitCode : "";
                String safeName = unitName != null ? unitName : "";
                String safeYear = year != null ? String.valueOf(year) : "";
                String safeSem = sem != null ? String.valueOf(sem) : "";

                if (unitCode != null && !unique.containsKey(unitCode)) {
                    unique.put(unitCode, safeCode + " - " + safeName +
                            " (Year " + safeYear + ", Sem " + safeSem + ")");
                }
            }
            unitsList.addAll(unique.values());
        }
        if (unitsList.isEmpty()) {
            // Avoid duplicate toast spam; just show empty state
            unitsList.add("No units found for you.");
        }
        adapter.notifyDataSetChanged();
    }

    private void stopUnitsListener() {
        if (unitsListener != null) {
            unitsListener.remove();
            unitsListener = null;
        }
        if (catalogListener != null) {
            catalogListener.remove();
            catalogListener = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser u = mAuth.getCurrentUser();
        if (u != null) {
            startUnitsListener(u.getUid());
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopUnitsListener();
    }
}
