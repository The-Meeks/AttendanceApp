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
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.EventListener;
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
    private ListenerRegistration unitsListener;
    private ListenerRegistration catalogListener;
    private final java.util.Set<String> activeUnitCodes = new java.util.HashSet<>();
    private com.google.firebase.firestore.QuerySnapshot lastUnitsSnap;

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

        // Start listening in onStart to respect lifecycle

        // Back button
        btnBack.setOnClickListener(v -> finish());
    }

    private void startUnitsListener(String lecturerUid) {
        stopUnitsListener();

        // Listen to catalog of actual units to know which codes still exist
        catalogListener = db.collection("units").addSnapshotListener((catalog, ce) -> {
            activeUnitCodes.clear();
            if (catalog != null) {
                for (QueryDocumentSnapshot d : catalog) {
                    String c = d.getString("unit_code");
                    if (c != null) activeUnitCodes.add(c);
                }
            }
            // Rebuild immediately using last snapshot
            rebuildFromSnapshots();
        });

        unitsListener = db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", lecturerUid)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error loading units", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    lastUnitsSnap = snap;
                    rebuildFromSnapshots();
                });
    }

    private void rebuildFromSnapshots() {
        unitsList.clear();
        if (lastUnitsSnap != null) {
            java.util.Map<String, String> unique = new java.util.LinkedHashMap<>();
            for (QueryDocumentSnapshot document : lastUnitsSnap) {
                String unitCode = document.getString("unit_code");
                String unitName = document.getString("unit_name");
                String semester = document.getString("semester");
                Long year = document.getLong("year_of_study");

                if (unitCode == null && unitName == null) continue;
                if (unitCode != null && !activeUnitCodes.isEmpty() && !activeUnitCodes.contains(unitCode)) {
                    continue; // filtered out by catalog removal
                }

                String safeCode = unitCode != null ? unitCode : "";
                String safeName = unitName != null ? unitName : "";
                String safeSem = semester != null ? semester : "";
                String safeYear = year != null ? String.valueOf(year) : "";

                String unitDetails = safeCode + " - " + safeName +
                        "\nYear: " + safeYear + ", " + safeSem;
                if (unitCode != null && !unique.containsKey(unitCode)) {
                    unique.put(unitCode, unitDetails);
                }
            }
            unitsList.addAll(unique.values());
        }
        if (unitsList.isEmpty()) {
            unitsList.add("No units found for this lecturer.");
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
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startUnitsListener(currentUser.getUid());
        } else {
            Toast.makeText(this, "No lecturer logged in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopUnitsListener();
    }
}
