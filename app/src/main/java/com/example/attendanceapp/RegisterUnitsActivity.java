package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterUnitsActivity extends AppCompatActivity {

    private EditText etUnitCode, etUnitName;
    private Spinner spinnerUnitDept;
    private Button btnRegisterUnit;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register_units);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views with corrected ID
        etUnitCode = findViewById(R.id.etUnitCode);
        etUnitName = findViewById(R.id.etUnitName);
        spinnerUnitDept = findViewById(R.id.spinnerUnitDept); // ✅ Corrected ID
        btnRegisterUnit = findViewById(R.id.btnRegisterUnit);

        // Set up spinner
        String[] departments = {"Computer Science", "Information Technology"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, departments);
        spinnerUnitDept.setAdapter(adapter);

        // Click listener
        btnRegisterUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUnit();
            }
        });
    }

    private void registerUnit() {
        String unitCode = etUnitCode.getText().toString().trim();
        String unitName = etUnitName.getText().toString().trim();
        String department = spinnerUnitDept.getSelectedItem().toString();

        if (unitCode.isEmpty() || unitName.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> unit = new HashMap<>();
        unit.put("unit_code", unitCode);
        unit.put("unit_name", unitName);
        unit.put("department", department);

        db.collection("units")
                .add(unit)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Unit registered successfully", Toast.LENGTH_SHORT).show();
                    clearFields();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void clearFields() {
        etUnitCode.setText("");
        etUnitName.setText("");
        spinnerUnitDept.setSelection(0);
    }
}
