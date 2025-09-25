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
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LecturerGenerateQRActivity extends AppCompatActivity {

    private Spinner spinnerUnits;
    private EditText etCheckInMins, etCheckOutMins;
    private Button btnGenerateCheckIn, btnGenerateCheckOut, btnBack;
    private ImageView imageQRCheckIn, imageQRCheckOut;
    private TextView tvSessionInfo, tvCheckInWindow, tvCheckOutWindow;

    private FirebaseFirestore db;

    private ArrayList<String> unitList = new ArrayList<>();
    private ArrayList<String> unitCodes = new ArrayList<>();
    private ArrayList<String> unitNames = new ArrayList<>();
    private String selectedUnitCode = "";
    private String selectedUnitName = "";
    // Track a single session per screen so IN and OUT share same sessionId
    private String currentSessionId = null;
    private long currentStartMillis = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        spinnerUnits = findViewById(R.id.spinnerUnits);
        etCheckInMins = findViewById(R.id.etCheckInMins);
        etCheckOutMins = findViewById(R.id.etCheckOutMins);
        btnGenerateCheckIn = findViewById(R.id.btnGenerateCheckIn);
        btnGenerateCheckOut = findViewById(R.id.btnGenerateCheckOut);
        btnBack = findViewById(R.id.btnBack);
        imageQRCheckIn = findViewById(R.id.imageQRCheckIn);
        imageQRCheckOut = findViewById(R.id.imageQRCheckOut);
        tvSessionInfo = findViewById(R.id.tvSessionInfo);
        tvCheckInWindow = findViewById(R.id.tvCheckInWindow);
        tvCheckOutWindow = findViewById(R.id.tvCheckOutWindow);

        db = FirebaseFirestore.getInstance();

        loadLecturerUnits();

        btnGenerateCheckIn.setOnClickListener(v -> generateCheckIn());
        btnGenerateCheckOut.setOnClickListener(v -> generateCheckOut());
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

    private void generateCheckIn() {
        if (!ensureUnitSelection()) return;

        String minsStr = etCheckInMins.getText().toString().trim();
        if (minsStr.isEmpty()) {
            Toast.makeText(this, "Enter check-in expiry (minutes)", Toast.LENGTH_SHORT).show();
            return;
        }
        int mins = Integer.parseInt(minsStr);
        if (mins <= 0) {
            Toast.makeText(this, "Minutes must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        long checkInEnd = now + mins * 60L * 1000L;

        createOrUpdateSession(now, checkInEnd, null);
    }

    private void generateCheckOut() {
        if (!ensureUnitSelection()) return;

        String minsStr = etCheckOutMins.getText().toString().trim();
        if (minsStr.isEmpty()) {
            Toast.makeText(this, "Enter check-out expiry (minutes)", Toast.LENGTH_SHORT).show();
            return;
        }
        int mins = Integer.parseInt(minsStr);
        if (mins <= 0) {
            Toast.makeText(this, "Minutes must be > 0", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        long checkOutEnd = now + mins * 60L * 1000L;

        createOrUpdateSession(now, null, checkOutEnd);
    }

    private boolean ensureUnitSelection() {
        if (unitList.isEmpty() || unitCodes.isEmpty()) {
            Toast.makeText(this, "No unit selected", Toast.LENGTH_SHORT).show();
            return false;
        }
        int pos = spinnerUnits.getSelectedItemPosition();
        if (pos >= unitCodes.size()) {
            Toast.makeText(this, "Invalid unit selection", Toast.LENGTH_SHORT).show();
            return false;
        }
        selectedUnitCode = unitCodes.get(pos);
        selectedUnitName = unitNames.get(pos);
        return true;
    }

    private void saveSession(long startMillis, long endMillis) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference docRef = db.collection("lecturerSessions").document();

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

        // Initialize with zero windows; they'll be set by button actions
        long checkInStart = startMillis;
        long checkOutStart = startMillis;
        session.put("checkin_start_millis", checkInStart);
        session.put("checkin_end_millis", startMillis);
        session.put("checkout_start_millis", checkOutStart);
        session.put("checkout_end_millis", startMillis);

        docRef.set(session).addOnSuccessListener(aVoid -> {
            // Initialize attendance counters based on enrollments
            db.collection("studentUnits")
                    .whereEqualTo("unit_code", selectedUnitCode)
                    .get()
                    .addOnSuccessListener(q -> {
                        int enrolledCount = q.size();
                        Map<String, Object> init = new HashMap<>();
                        init.put("presentCount", 0);
                        init.put("enrolledCount", enrolledCount);
                        init.put("attendancePercentage", 0);
                        docRef.update(init);
                    });

            try {
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

    // Create session if needed and/or update expiry windows
    private void createOrUpdateSession(long now, Long checkInEndMillis, Long checkOutEndMillis) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

        if (currentSessionId == null) {
            // Create a brand new session document
            DocumentReference docRef = db.collection("lecturerSessions").document();
            currentSessionId = docRef.getId();
            currentStartMillis = now;

            long ciEnd = (checkInEndMillis != null ? checkInEndMillis : now);
            long coEnd = (checkOutEndMillis != null ? checkOutEndMillis : now);
            long endMillis = Math.max(ciEnd, coEnd);

            String date = dateFormat.format(new Date(now));
            String startTime = timeFormat.format(new Date(now));
            String endTime = timeFormat.format(new Date(endMillis));

            Map<String, Object> session = new HashMap<>();
            session.put("sessionId", currentSessionId);
            session.put("lecturer_id", user.getUid());
            session.put("unit_code", selectedUnitCode);
            session.put("unit_name", selectedUnitName);
            session.put("date", date);
            session.put("start_time", startTime);
            session.put("end_time", endTime);
            session.put("start_millis", now);
            session.put("end_millis", endMillis);
            session.put("checkin_start_millis", now);
            session.put("checkin_end_millis", ciEnd);
            session.put("checkout_start_millis", now);
            session.put("checkout_end_millis", coEnd);

            DocumentReference sessionRef = db.collection("lecturerSessions").document(currentSessionId);
            sessionRef.set(session).addOnSuccessListener(aVoid -> {
                // Initialize attendance counters based on enrollments
                db.collection("studentUnits")
                        .whereEqualTo("unit_code", selectedUnitCode)
                        .get()
                        .addOnSuccessListener(q -> {
                            int enrolledCount = q.size();
                            Map<String, Object> init = new HashMap<>();
                            init.put("presentCount", 0);
                            init.put("enrolledCount", enrolledCount);
                            init.put("attendancePercentage", 0);
                            sessionRef.update(init);
                        });

                // Update UI and QRs
                tvSessionInfo.setText("Session: " + selectedUnitName + " (" + selectedUnitCode + ")\n"
                        + "Date: " + date + "\n"
                        + "Time: " + startTime + " - " + endTime);

                try {
                    if (checkInEndMillis != null) {
                        String qrCheckIn = selectedUnitCode + "|" + currentSessionId + "|IN";
                        setQrOn(imageQRCheckIn, qrCheckIn);
                        tvCheckInWindow.setText("Valid: " + timeFormat.format(new Date(now)) + " - " + timeFormat.format(new Date(ciEnd)));
                    }
                    if (checkOutEndMillis != null) {
                        String qrCheckOut = selectedUnitCode + "|" + currentSessionId + "|OUT";
                        setQrOn(imageQRCheckOut, qrCheckOut);
                        tvCheckOutWindow.setText("Valid: " + timeFormat.format(new Date(now)) + " - " + timeFormat.format(new Date(coEnd)));
                    }
                } catch (Exception ignored) {}
            }).addOnFailureListener(e -> Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show());

        } else {
            // Update existing session's windows and end time
            DocumentReference sessionRef = db.collection("lecturerSessions").document(currentSessionId);
            sessionRef.get().addOnSuccessListener(snap -> {
                if (!snap.exists()) {
                    // Fallback: reset and create
                    currentSessionId = null;
                    createOrUpdateSession(now, checkInEndMillis, checkOutEndMillis);
                    return;
                }

                Long existingCiEnd = snap.getLong("checkin_end_millis");
                Long existingCoEnd = snap.getLong("checkout_end_millis");

                long ciEnd = existingCiEnd != null ? existingCiEnd : currentStartMillis;
                long coEnd = existingCoEnd != null ? existingCoEnd : currentStartMillis;

                if (checkInEndMillis != null) ciEnd = Math.max(ciEnd, checkInEndMillis);
                if (checkOutEndMillis != null) coEnd = Math.max(coEnd, checkOutEndMillis);

                long endMillis = Math.max(ciEnd, coEnd);
                String date = snap.getString("date");
                String startTime = snap.getString("start_time");
                String endTime = timeFormat.format(new Date(endMillis));

                Map<String, Object> updates = new HashMap<>();
                updates.put("end_millis", endMillis);
                updates.put("end_time", endTime);
                updates.put("checkin_end_millis", ciEnd);
                updates.put("checkout_end_millis", coEnd);

                sessionRef.update(updates).addOnSuccessListener(v -> {
                    tvSessionInfo.setText("Session: " + selectedUnitName + " (" + selectedUnitCode + ")\n"
                            + "Date: " + (date != null ? date : dateFormat.format(new Date(currentStartMillis))) + "\n"
                            + "Time: " + (startTime != null ? startTime : timeFormat.format(new Date(currentStartMillis))) + " - " + endTime);

                    try {
                        if (checkInEndMillis != null) {
                            String qrCheckIn = selectedUnitCode + "|" + currentSessionId + "|IN";
                            setQrOn(imageQRCheckIn, qrCheckIn);
                            tvCheckInWindow.setText("Valid: " + timeFormat.format(new Date(currentStartMillis)) + " - " + timeFormat.format(new Date(ciEnd)));
                        }
                        if (checkOutEndMillis != null) {
                            String qrCheckOut = selectedUnitCode + "|" + currentSessionId + "|OUT";
                            setQrOn(imageQRCheckOut, qrCheckOut);
                            tvCheckOutWindow.setText("Valid: " + timeFormat.format(new Date(currentStartMillis)) + " - " + timeFormat.format(new Date(coEnd)));
                        }
                    } catch (Exception ignored) {}
                });
            });
        }
    }

    private void setQrOn(ImageView imageView, String content) throws WriterException {
        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
            }
        }
        imageView.setImageBitmap(bitmap);
    }
}
