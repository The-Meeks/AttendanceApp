package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StudentScanQRActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String studentId;
    private String studentName;
    private String regNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_scan_qr);

        barcodeView = findViewById(R.id.barcode_scanner);
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        studentId = mAuth.getCurrentUser().getUid();

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
        String[] parts = qrText.split("\\|");
        if (parts.length < 2 || parts.length > 3) {
            Toast.makeText(this, "Invalid QR content", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        String unitCode = parts[0];
        String sessionId = parts[1];
        String type = parts.length == 3 ? parts[2] : "IN"; // default backward-compat

        if (!"IN".equals(type) && !"OUT".equals(type)) {
            Toast.makeText(this, "Invalid QR type", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        markAttendance(sessionId, unitCode, type);
    }

    private void markAttendance(String sessionId, String unitCode, String type) {
        if (studentName == null || regNumber == null) {
            Toast.makeText(this, "Could not load student details", Toast.LENGTH_SHORT).show();
            barcodeView.resume();
            return;
        }

        DocumentReference sessionRef = db.collection("lecturerSessions").document(sessionId);

        sessionRef.get().addOnSuccessListener(snap -> {
            if (!snap.exists()) {
                Toast.makeText(this, "Session not found", Toast.LENGTH_SHORT).show();
                barcodeView.resume();
                return;
            }

            long now = System.currentTimeMillis();
            Long cinStart = snap.getLong("checkin_start_millis");
            Long cinEnd = snap.getLong("checkin_end_millis");
            Long coutStart = snap.getLong("checkout_start_millis");
            Long coutEnd = snap.getLong("checkout_end_millis");
            Long startMillis = snap.getLong("start_millis");
            Long endMillis = snap.getLong("end_millis");

            if ("IN".equals(type)) {
                if (cinStart != null && cinEnd != null && (now < cinStart || now > cinEnd)) {
                    Toast.makeText(this, "Check-in window closed", Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                    return;
                }
            } else {
                if (coutStart != null && coutEnd != null && (now < coutStart || now > coutEnd)) {
                    Toast.makeText(this, "Check-out window closed", Toast.LENGTH_SHORT).show();
                    barcodeView.resume();
                    return;
                }
            }

            SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

            DocumentReference attRef = sessionRef.collection("attendance").document(studentId);
            attRef.get().addOnSuccessListener(doc -> {
                Map<String, Object> updates = new HashMap<>();
                updates.put("student_id", studentId);
                updates.put("full_name", studentName);
                updates.put("reg_number", regNumber);
                updates.put("unit_code", unitCode);

                if ("IN".equals(type)) {
                    updates.put("checkin_millis", now);
                    updates.put("checkin_time", timeFormat.format(new Date(now)));
                    if (!doc.exists()) {
                        // create base with date for display
                        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        updates.put("date", dateFormat.format(new Date(now)));
                    }
                } else { // OUT
                    updates.put("checkout_millis", now);
                    updates.put("checkout_time", timeFormat.format(new Date(now)));
                }

                attRef.set(updates, com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            // After any update, recompute status if possible and update session percentage
                            attRef.get().addOnSuccessListener(cur -> {
                                Long inMs = cur.getLong("checkin_millis");
                                Long outMs = cur.getLong("checkout_millis");
                                String status;
                                if (inMs == null && outMs == null) {
                                    status = "Invalid";
                                } else if (inMs != null && outMs == null) {
                                    status = "Absent"; // checked in only
                                } else if (inMs == null) {
                                    status = "Invalid"; // checkout without checkin
                                } else {
                                    long sessionDuration = (endMillis != null && startMillis != null) ? (endMillis - startMillis) : 0L;
                                    long attended = outMs - inMs;
                                    float ratio = (sessionDuration > 0) ? (attended / (float) sessionDuration) : 0f;
                                    status = ratio >= 0.75f ? "Present" : "Partial";
                                }
                                Map<String, Object> st = new HashMap<>();
                                st.put("status", status);
                                attRef.update(st);

                                // Recompute present count only (status == Present)
                                sessionRef.collection("attendance")
                                        .whereEqualTo("status", "Present")
                                        .get()
                                        .addOnSuccessListener(presentQuery -> {
                                            int presentCount = presentQuery.size();
                                            Integer enrolledCount = 0;
                                            Long enrolled = snap.getLong("enrolledCount");
                                            if (enrolled != null) enrolledCount = enrolled.intValue();
                                            int percentage = (enrolledCount > 0)
                                                    ? Math.round(100f * presentCount / enrolledCount)
                                                    : 0;
                                            Map<String, Object> upd = new HashMap<>();
                                            upd.put("presentCount", presentCount);
                                            upd.put("attendancePercentage", percentage);
                                            sessionRef.update(upd);
                                        });
                            });

                            Toast.makeText(this,
                                    ("IN".equals(type) ? "Check-in recorded" : "Check-out recorded"),
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            barcodeView.resume();
                        });
            });
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
