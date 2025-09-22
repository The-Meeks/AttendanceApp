package com.example.attendanceapp;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LecturerGenerateQRActivity extends AppCompatActivity {

    private Spinner spinnerUnits;
    private EditText etDurationMins;
    private Button btnGenerate, btnBack;
    private ImageView imageQR;
    private TextView tvSessionInfo;

    private FirebaseFirestore db;

    private ArrayList<String> unitList = new ArrayList<>();
    private ArrayList<String> unitCodes = new ArrayList<>();
    private ArrayList<String> unitNames = new ArrayList<>();
    private String selectedUnitCode = "";
    private String selectedUnitName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        spinnerUnits = findViewById(R.id.spinnerUnits);
        etDurationMins = findViewById(R.id.etDurationMins);
        btnGenerate = findViewById(R.id.btnGenerate);
        btnBack = findViewById(R.id.btnBack);
        imageQR = findViewById(R.id.imageQR);
        tvSessionInfo = findViewById(R.id.tvSessionInfo);

        db = FirebaseFirestore.getInstance();

        loadLecturerUnits();

        btnGenerate.setOnClickListener(v -> generateSession());
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadLecturerUnits() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", user.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    unitList.clear();
                    unitCodes.clear();
                    unitNames.clear();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String unitCode = doc.getString("unit_code");
                        String unitName = doc.getString("unit_name");

                        if (unitCode != null && unitName != null) {
                            unitList.add(unitName + " (" + unitCode + ")");
                            unitCodes.add(unitCode);
                            unitNames.add(unitName);
                        }
                    }

                    if (unitList.isEmpty()) {
                        unitList.add("No units assigned");
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this, android.R.layout.simple_spinner_item, unitList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerUnits.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load units", Toast.LENGTH_SHORT).show()
                );
    }

    private void generateSession() {
        if (unitList.isEmpty() || unitCodes.isEmpty()) {
            Toast.makeText(this, "No unit selected", Toast.LENGTH_SHORT).show();
            return;
        }

        int pos = spinnerUnits.getSelectedItemPosition();
        if (pos >= unitCodes.size()) {
            Toast.makeText(this, "Invalid unit selection", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedUnitCode = unitCodes.get(pos);
        selectedUnitName = unitNames.get(pos);

        String durationStr = etDurationMins.getText().toString().trim();

        if (durationStr.isEmpty()) {
            Toast.makeText(this, "Enter duration", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Integer.parseInt(durationStr);

        long startMillis = System.currentTimeMillis();
        long endMillis = startMillis + (duration * 60L * 1000);

        saveSession(startMillis, endMillis);
    }

    private void saveSession(long startMillis, long endMillis) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = db.collection("lecturerSessions").document();

        // Format date and time for display
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        String date = dateFormat.format(new Date(startMillis));
        String startTime = timeFormat.format(new Date(startMillis));
        String endTime = timeFormat.format(new Date(endMillis));

        Map<String, Object> session = new HashMap<>();
        session.put("sessionId", docRef.getId());
        session.put("lecturer_id", user.getUid());
        session.put("unit_code", selectedUnitCode);
        session.put("unit_name", selectedUnitName);
        session.put("date", date);
        session.put("start_time", startTime);
        session.put("end_time", endTime);
        session.put("start_millis", startMillis);
        session.put("end_millis", endMillis);

        docRef.set(session).addOnSuccessListener(aVoid -> {
            try {
                // QR = unitCode|sessionId
                String qrContent = selectedUnitCode + "|" + docRef.getId();

                generateQRCode(qrContent);

                tvSessionInfo.setText("Session: " + selectedUnitName + " (" + selectedUnitCode + ")\n"
                        + "Date: " + date + "\n"
                        + "Time: " + startTime + " - " + endTime);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show()
        );
    }

    private void generateQRCode(String text) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int size = 512;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size);
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }

            imageQR.setImageBitmap(bitmap);

        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error generating QR", Toast.LENGTH_SHORT).show();
        }
    }
}
