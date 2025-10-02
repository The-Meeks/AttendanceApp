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
    // Optional preselection from dashboard
    private String pendingUnitCode = null;
    private String pendingUnitName = null;
    private boolean makeupApproved = false;

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

        // Read optional intent extras for preselection
        if (getIntent() != null) {
            pendingUnitCode = getIntent().getStringExtra("unit_code");
            pendingUnitName = getIntent().getStringExtra("unit_name");
            makeupApproved = getIntent().getBooleanExtra("makeupApproved", false);
        }


        loadLecturerUnits();

        // When lecturer changes the selected unit, reset any in-progress session context
        spinnerUnits.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position >= 0 && position < unitCodes.size()) {
                    selectedUnitCode = unitCodes.get(position);
                    selectedUnitName = unitNames.get(position);
                    // Reset current session so new QRs belong to the newly selected unit
                    currentSessionId = null;
                    currentStartMillis = 0L;
                    // Clear displayed QR and session info for clarity
                    imageQRCheckIn.setImageDrawable(null);
                    imageQRCheckOut.setImageDrawable(null);
                    tvSessionInfo.setText("");
                    tvCheckInWindow.setText("");
                    tvCheckOutWindow.setText("");
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnGenerateCheckIn.setOnClickListener(v -> generateCheckIn());
        btnGenerateCheckOut.setOnClickListener(v -> generateCheckOut());
        findViewById(R.id.btnCloseSession).setOnClickListener(v -> closeSession());
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

                    // 1) Deduplicate by unit_code
                    java.util.LinkedHashMap<String, String> codeToName = new java.util.LinkedHashMap<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String unitCode = doc.getString("unit_code");
                        String unitName = doc.getString("unit_name");
                        if (unitCode != null && unitName != null && !codeToName.containsKey(unitCode)) {
                            codeToName.put(unitCode, unitName);
                        }
                    }

                    if (codeToName.isEmpty()) {
                        unitList.add("No units assigned");
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                this, android.R.layout.simple_spinner_item, unitList);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerUnits.setAdapter(adapter);
                        return;
                    }

                    // 2) Validate against catalog 'units' so removed codes are filtered out
                    java.util.List<String> codes = new java.util.ArrayList<>(codeToName.keySet());
                    final java.util.Set<String> valid = new java.util.HashSet<>();
                    final int total = codes.size();
                    final int[] batchesLeft = { (int) Math.ceil(total / 10.0) };
                    for (int i = 0; i < total; i += 10) {
                        java.util.List<String> batch = codes.subList(i, Math.min(i + 10, total));
                        db.collection("units").whereIn("unit_code", batch).get().addOnSuccessListener(unitsSnap -> {
                            for (com.google.firebase.firestore.DocumentSnapshot u : unitsSnap.getDocuments()) {
                                String c = u.getString("unit_code");
                                if (c != null) valid.add(c);
                            }
                        }).addOnCompleteListener(t -> {
                            batchesLeft[0]--;
                            if (batchesLeft[0] <= 0) {
                                // 3) Build spinner lists only with valid codes and sort by name
                                java.util.List<String> outCodes = new java.util.ArrayList<>();
                                java.util.List<String> outNames = new java.util.ArrayList<>();
                                for (java.util.Map.Entry<String, String> e : codeToName.entrySet()) {
                                    if (!valid.contains(e.getKey())) continue;
                                    outCodes.add(e.getKey());
                                    outNames.add(e.getValue());
                                }
                                // sort by name, and apply the same order to codes/names and display
                                java.util.List<Integer> idxs = new java.util.ArrayList<>();
                                for (int k = 0; k < outCodes.size(); k++) idxs.add(k);
                                idxs.sort((a, b) -> outNames.get(a).compareToIgnoreCase(outNames.get(b)));

                                java.util.List<String> display = new java.util.ArrayList<>();
                                for (Integer id : idxs) {
                                    unitCodes.add(outCodes.get(id));
                                    unitNames.add(outNames.get(id));
                                    display.add(outNames.get(id) + " (" + outCodes.get(id) + ")");
                                }

                                unitList.addAll(display);

                                if (unitList.isEmpty()) unitList.add("No units assigned");

                                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                                        this, android.R.layout.simple_spinner_item, unitList);
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                                spinnerUnits.setAdapter(adapter);

                                // If pending extras provided, try to preselect
                                if (pendingUnitCode != null && !pendingUnitCode.isEmpty()) {
                                    int idx = -1;
                                    for (int i2 = 0; i2 < unitCodes.size(); i2++) {
                                        if (pendingUnitCode.equals(unitCodes.get(i2))) { idx = i2; break; }
                                    }
                                    if (idx >= 0) {
                                        spinnerUnits.setSelection(idx);
                                        selectedUnitCode = unitCodes.get(idx);
                                        selectedUnitName = unitNames.get(idx);
                                    }
                                }
                            }
                        });
                    }
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

            // Option 1: auto-tag as makeup if an approved window exists for today and now is within the window.
            final DocumentReference sessionRef = db.collection("lecturerSessions").document(currentSessionId);
            db.collection("makeupRequests")
                    .whereEqualTo("lecturer_id", user.getUid())
                    .whereEqualTo("unit_code", selectedUnitCode)
                    .whereEqualTo("status", "approved")
                    .whereEqualTo("requested_date", date)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(reqSnap -> {
                        boolean withinApproved = false;
                        if (!reqSnap.isEmpty()) {
                            Long rs = reqSnap.getDocuments().get(0).getLong("requested_start_millis");
                            Long re = reqSnap.getDocuments().get(0).getLong("requested_end_millis");
                            if (rs != null && re != null && now >= rs && now <= re) {
                                withinApproved = true;
                            }
                        }
                        if (withinApproved || makeupApproved) {
                            session.put("makeup", true);
                        }

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
                    })
                    .addOnFailureListener(e -> {
                        // If the request check fails, create session without the makeup tag.
                        sessionRef.set(session).addOnSuccessListener(aVoid -> {
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
                        }).addOnFailureListener(ex -> Toast.makeText(this, "Failed to create session", Toast.LENGTH_SHORT).show());
                    });

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

                // If the stored session belongs to a different unit, start a new one for the current selection
                String sessUnit = snap.getString("unit_code");
                if (sessUnit != null && !sessUnit.equals(selectedUnitCode)) {
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

                // Make effectively-final copies for use in lambdas
                final long finalCiEnd = ciEnd;
                final long finalCoEnd = coEnd;
                final long finalStartMillis = currentStartMillis;

                Map<String, Object> updates = new HashMap<>();
                updates.put("end_millis", endMillis);
                updates.put("end_time", endTime);
                updates.put("checkin_end_millis", finalCiEnd);
                updates.put("checkout_end_millis", finalCoEnd);

                sessionRef.update(updates).addOnSuccessListener(v -> {
                    tvSessionInfo.setText("Session: " + selectedUnitName + " (" + selectedUnitCode + ")\n"
                            + "Date: " + (date != null ? date : dateFormat.format(new Date(finalStartMillis))) + "\n"
                            + "Time: " + (startTime != null ? startTime : timeFormat.format(new Date(finalStartMillis))) + " - " + endTime);

                    try {
                        if (checkInEndMillis != null) {
                            String qrCheckIn = selectedUnitCode + "|" + currentSessionId + "|IN";
                            setQrOn(imageQRCheckIn, qrCheckIn);
                            tvCheckInWindow.setText("Valid: " + timeFormat.format(new Date(finalStartMillis)) + " - " + timeFormat.format(new Date(finalCiEnd)));
                        }
                        if (checkOutEndMillis != null) {
                            String qrCheckOut = selectedUnitCode + "|" + currentSessionId + "|OUT";
                            setQrOn(imageQRCheckOut, qrCheckOut);
                            tvCheckOutWindow.setText("Valid: " + timeFormat.format(new Date(finalStartMillis)) + " - " + timeFormat.format(new Date(finalCoEnd)));
                        }
                    } catch (Exception ignored) {}
                });
            });
        }
    }

    // Finalize the current session: clamp windows to now, mark remaining students as Absent, and recompute counts
    private void closeSession() {
        if (currentSessionId == null) {
            Toast.makeText(this, "No open session to close", Toast.LENGTH_SHORT).show();
            return;
        }
        final long now = System.currentTimeMillis();
        final DocumentReference sessionRef = db.collection("lecturerSessions").document(currentSessionId);

        // 1) Clamp windows and end time to now
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("checkin_end_millis", now);
        updates.put("checkout_end_millis", now);
        updates.put("end_millis", now);
        sessionRef.update(updates).addOnSuccessListener(x -> {
            // 2) Mark all enrolled but not present as Absent
            db.collection("studentUnits").whereEqualTo("unit_code", selectedUnitCode).get().addOnSuccessListener(enrolledSnap -> {
                java.util.Set<String> allUids = new java.util.HashSet<>();
                for (com.google.firebase.firestore.DocumentSnapshot d : enrolledSnap.getDocuments()) {
                    String uid = d.getString("student_id");
                    if (uid != null) allUids.add(uid);
                }
                sessionRef.collection("attendance").whereEqualTo("status", "Present").get().addOnSuccessListener(presentSnap -> {
                    java.util.Set<String> presentUids = new java.util.HashSet<>();
                    for (com.google.firebase.firestore.DocumentSnapshot d : presentSnap.getDocuments()) {
                        presentUids.add(d.getId());
                    }
                    for (String uid : allUids) {
                        if (presentUids.contains(uid)) continue;
                        java.util.Map<String, Object> m = new java.util.HashMap<>();
                        m.put("student_id", uid);
                        m.put("unit_code", selectedUnitCode);
                        m.put("status", "Absent");
                        sessionRef.collection("attendance").document(uid)
                                .set(m, com.google.firebase.firestore.SetOptions.merge());
                    }
                    // 3) Recompute counts
                    sessionRef.collection("attendance").whereEqualTo("status", "Present").get().addOnSuccessListener(pq -> {
                        int presentCount = pq.size();
                        int enrolledCount = allUids.size();
                        int percentage = (enrolledCount > 0) ? Math.round(100f * presentCount / enrolledCount) : 0;
                        java.util.Map<String, Object> fin = new java.util.HashMap<>();
                        fin.put("presentCount", presentCount);
                        fin.put("enrolledCount", enrolledCount);
                        fin.put("attendancePercentage", percentage);
                        sessionRef.update(fin);
                        Toast.makeText(this, "Session closed", Toast.LENGTH_SHORT).show();
                    });
                });
            });
        });
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
