package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class StudentViewAttendanceActivity extends AppCompatActivity {

    private RecyclerView rvUnitsAttendance;
    private UnitsAdapter adapter;
    private final List<UnitAttendanceItem> items = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private static final int TERM_SESSIONS_CAP = 12;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_view_attendance);

        rvUnitsAttendance = findViewById(R.id.rvUnitsAttendance);
        rvUnitsAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UnitsAdapter(items);
        rvUnitsAttendance.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadEnrolledUnits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto-refresh when returning from scan or other screens
        items.clear();
        adapter.notifyDataSetChanged();
        loadEnrolledUnits();
    }

    private void loadEnrolledUnits() {
        // Get all units the student is enrolled in
        db.collection("studentUnits").whereEqualTo("student_id", currentUser.getUid()).get().addOnSuccessListener(snap -> {
            Map<String, String> codeToName = new HashMap<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String code = d.getString("unit_code");
                String name = d.getString("unit_name");
                if (code != null) codeToName.put(code, name != null ? name : code);
            }
            if (codeToName.isEmpty()) {
                items.clear();
                adapter.notifyDataSetChanged();
                Toast.makeText(this, "No enrolled units", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate against catalog 'units' so we don't show removed/non-existent units
            List<String> allCodes = new ArrayList<>(codeToName.keySet());
            final Set<String> validCodes = new HashSet<>();
            if (allCodes.isEmpty()) {
                items.clear();
                adapter.notifyDataSetChanged();
                return;
            }
            // Firestore whereIn supports up to 10 values; batch if needed
            int total = allCodes.size();
            final int[] remainingBatches = { (int) Math.ceil(total / 10.0) };
            for (int i = 0; i < total; i += 10) {
                List<String> batch = allCodes.subList(i, Math.min(i + 10, total));
                db.collection("units").whereIn("unit_code", batch).get().addOnSuccessListener(unitSnap -> {
                    for (DocumentSnapshot ud : unitSnap.getDocuments()) {
                        String c = ud.getString("unit_code");
                        if (c != null) validCodes.add(c);
                    }
                }).addOnCompleteListener(t -> {
                    remainingBatches[0]--;
                    if (remainingBatches[0] <= 0) {
                        // Now render only valid codes
                        items.clear();
                        adapter.notifyDataSetChanged();
                        for (Map.Entry<String, String> e : codeToName.entrySet()) {
                            if (!validCodes.contains(e.getKey())) continue;
                            loadProgressForUnit(e.getKey(), e.getValue());
                        }
                    }
                });
            }
        });
    }

    private void loadProgressForUnit(String unitCode, String unitName) {
        db.collection("lecturerSessions")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(sessions -> {
                    // Sort client-side by start_millis ASC and limit 12
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot s : sessions) docs.add(s);
                    docs.sort((a, b) -> {
                        Long sa = a.getLong("start_millis");
                        Long sb = b.getLong("start_millis");
                        long va = sa != null ? sa : 0L; long vb = sb != null ? sb : 0L;
                        return Long.compare(va, vb);
                    });
                    if (docs.size() > TERM_SESSIONS_CAP) {
                        docs = docs.subList(0, TERM_SESSIONS_CAP);
                    }
                    int held = docs.size();
                    if (held == 0) {
                        addOrUpdateItem(unitCode, unitName, 0, 0);
                        return;
                    }
                    final int[] present = {0};
                    final int[] processed = {0};
                    for (QueryDocumentSnapshot s : docs) {
                        String sid = s.getId();
                        db.collection("lecturerSessions").document(sid)
                                .collection("attendance").document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(a -> {
                                    String status = a.getString("status");
                                    if ("Present".equalsIgnoreCase(status)) present[0]++;
                                })
                                .addOnCompleteListener(t -> {
                                    processed[0]++;
                                    if (processed[0] >= held) {
                                        addOrUpdateItem(unitCode, unitName, held, present[0]);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    // If query fails (e.g., missing index), still render the card as 0/12 so the list is visible
                    addOrUpdateItem(unitCode, unitName, 0, 0);
                });
    }

    private void addOrUpdateItem(String code, String name, int held, int present) {
        UnitAttendanceItem item = new UnitAttendanceItem(name, code, held, present);
        items.add(item);
        adapter.notifyItemInserted(items.size() - 1);
    }

    static class UnitAttendanceItem {
        String unitName; String unitCode; int held; int present;
        UnitAttendanceItem(String unitName, String unitCode, int held, int present) {
            this.unitName = unitName; this.unitCode = unitCode; this.held = held; this.present = present;
        }
    }

    class UnitsAdapter extends RecyclerView.Adapter<UnitsAdapter.VH> {
        List<UnitAttendanceItem> data;
        UnitsAdapter(List<UnitAttendanceItem> d) { data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_student_unit_attendance, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            UnitAttendanceItem it = data.get(position);
            h.tvUnitTitle.setText(it.unitName);
            h.tvUnitSub.setText(it.unitCode);
            // Percent attended should be based on Present/12 (term cap)
            int pctAttended = (int) Math.round((it.present * 100.0) / TERM_SESSIONS_CAP);
            // Show: '<held>/12 • <pctAttended>% attended'
            h.tvProgressCount.setText(it.held + "/" + TERM_SESSIONS_CAP + " • " + pctAttended + "% attended");
            h.progressAttendance.setMax(TERM_SESSIONS_CAP);
            h.progressAttendance.setProgress(it.present);
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(StudentViewAttendanceActivity.this, StudentUnitSessionsActivity.class);
                i.putExtra("unit_code", it.unitCode);
                i.putExtra("unit_name", it.unitName);
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvUnitTitle, tvUnitSub, tvProgressCount; ProgressBar progressAttendance;
            VH(@NonNull View itemView) { super(itemView);
                tvUnitTitle = itemView.findViewById(R.id.tvUnitTitle);
                tvUnitSub = itemView.findViewById(R.id.tvUnitSub);
                tvProgressCount = itemView.findViewById(R.id.tvProgressCount);
                progressAttendance = itemView.findViewById(R.id.progressAttendance);
            }
        }
    }
}
