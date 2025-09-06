package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentScanQRActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private FirebaseFirestore db;
    private String studentId;
    private String studentName;
    private String regNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_scan_qr);

        barcodeView = findViewById(R.id.barcode_scanner);
        db = FirebaseFirestore.getInstance();
        studentId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Load student info
        db.collection("students").document(studentId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        studentName = document.getString("full_name");
                        regNumber = document.getString("reg_number");
                    }
                    barcodeView.decodeContinuous(callback);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading student info", Toast.LENGTH_SHORT).show();
                    barcodeView.decodeContinuous(callback);
                });
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                barcodeView.pause();
                handleScannedCode(result.getText());
            }
        }
    };

    private void handleScannedCode(String qrText) {
        if (!qrText.contains("|")) {
            Toast.makeText(this, "Invalid QR format", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        String[] parts = qrText.split("\\|");
        if (parts.length != 2) {
            Toast.makeText(this, "Invalid QR content", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        String unitCode = parts[0];
        String sessionId = parts[1];

        markAttendance(sessionId, unitCode);
    }

    private void markAttendance(String sessionId, String unitCode) {
        if (studentName == null || regNumber == null) {
            Toast.makeText(this, "Could not load student details", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        db.collection("lecturerSessions").document(sessionId)
                .collection("attendance").document(studentId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Toast.makeText(this, "Already marked present", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Map<String, Object> data = new HashMap<>();
                        data.put("student_id", studentId);
                        data.put("full_name", studentName);
                        data.put("reg_number", regNumber);
                        data.put("unit_code", unitCode);
                        data.put("timestamp", System.currentTimeMillis());

                        db.collection("lecturerSessions").document(sessionId)
                                .collection("attendance").document(studentId)
                                .set(data)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this,
                                            String.format(Locale.getDefault(),
                                                    "Attendance marked: %s (%s)",
                                                    studentName, regNumber),
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this,
                                            "Failed to mark attendance: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                    barcodeView.resume();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}
