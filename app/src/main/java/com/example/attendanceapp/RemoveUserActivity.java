package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class RemoveUserActivity extends AppCompatActivity {

    private Spinner spinnerUserType;
    private EditText etUserEmail;
    private Button btnRemoveUser;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remove_user);

        spinnerUserType = findViewById(R.id.spinnerUserType); // ✅ fixed ID
        etUserEmail = findViewById(R.id.etUserEmail);         // ✅ fixed ID
        btnRemoveUser = findViewById(R.id.btnRemoveUser);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        String[] roles = {"student", "lecturer"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, roles);
        spinnerUserType.setAdapter(adapter);

        btnRemoveUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeUser();
            }
        });
    }

    private void removeUser() {
        String email = etUserEmail.getText().toString().trim();
        String role = spinnerUserType.getSelectedItem().toString();

        if (email.isEmpty()) {
            Toast.makeText(this, "Enter user email", Toast.LENGTH_SHORT).show();
            return;
        }

        String collection = role.equals("student") ? "students" : "lecturers";

        db.collection(collection)
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        queryDocumentSnapshots.getDocuments().get(0).getReference().delete()
                                .addOnSuccessListener(unused -> Toast.makeText(RemoveUserActivity.this, "User removed from Firestore", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(RemoveUserActivity.this, "Failed to remove user", Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(RemoveUserActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(RemoveUserActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
