package com.example.attendanceapp;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class StudentScanQRActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeScanner;
    private TextView tvScanStatus;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_scan_qr);

        barcodeScanner = findViewById(R.id.barcode_scanner);
        tvScanStatus = findViewById(R.id.tvScanStatus);
        db = FirebaseFirestore.getInstance();

        barcodeScanner.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result.getText() != null) {
                    handleScannedQR(result.getText());
                    barcodeScanner.pause();
                }
            }
        });
    }

    private void handleScannedQR(String qrText) {
        try {
            JSONObject obj = new JSONObject(qrText);
            String sessionId = obj.getString("sessionId");

            fetchSession(sessionId);

        } catch (Exception e) {
            tvScanStatus.setText("Invalid QR format");
        }
    }

    private void fetchSession(String sessionId) {
        db.collection("lecturerSessions").document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        tvScanStatus.setText("Session not found");
                        return;
                    }

                    validateSession(documentSnapshot);
                })
                .addOnFailureListener(e ->
                        tvScanStatus.setText("Error fetching session")
                );
    }

    private void validateSession(DocumentSnapshot doc) {
        FirebaseUser student = FirebaseAuth.getInstance().getCurrentUser();
        if (student == null) {
            tvScanStatus.setText("Not logged in");
            return;
        }

        String unitCode = doc.getString("unit_code");
        String unitName = doc.getString("unit_name");
        long startTime = doc.getLong("start_time");
        long endTime = doc.getLong("end_time");
        long now = System.currentTimeMillis();

        // Check time validity
        if (now < startTime || now > endTime) {
            tvScanStatus.setText("Session expired or not started");
            return;
        }

        // Check enrollment
        db.collection("studentUnits")
                .whereEqualTo("student_id", student.getUid())
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(querySnapshots -> {
                    if (querySnapshots.isEmpty()) {
                        tvScanStatus.setText("Not enrolled in " + unitCode);
                        return;
                    }

                    recordAttendance(student.getUid(), doc.getId(), unitCode, unitName);

                })
                .addOnFailureListener(e ->
                        tvScanStatus.setText("Error checking enrollment")
                );
    }

    private void recordAttendance(String studentId, String sessionId, String unitCode, String unitName) {
        Map<String, Object> attendance = new HashMap<>();
        attendance.put("student_id", studentId);
        attendance.put("session_id", sessionId);
        attendance.put("unit_code", unitCode);
        attendance.put("unit_name", unitName);
        attendance.put("timestamp", System.currentTimeMillis());

        db.collection("attendance")
                .add(attendance)
                .addOnSuccessListener(documentReference -> {
                    tvScanStatus.setText("Attendance marked for " + unitName);
                })
                .addOnFailureListener(e ->
                        tvScanStatus.setText("Failed to mark attendance")
                );
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeScanner.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeScanner.pause();
    }
}
