package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LecturerSessionDetailActivity extends AppCompatActivity {

    private TextView tvHeader, tvSummary;
    private RecyclerView rv;
    private Button btnClose, btnExportCsv, btnExportPdf;
    private final List<Row> rows = new ArrayList<>();
    private Adapter adapter;

    private FirebaseFirestore db;
    private String unitCode, unitName, sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_session_detail);

        unitCode = getIntent().getStringExtra("unit_code");
        unitName = getIntent().getStringExtra("unit_name");
        sessionId = getIntent().getStringExtra("session_id");

        tvHeader = findViewById(R.id.tvHeader);
        tvSummary = findViewById(R.id.tvSummary);
        rv = findViewById(R.id.rvAttendance);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter(rows);
        rv.setAdapter(adapter);
        btnClose = findViewById(R.id.btnCloseSession);
        btnExportCsv = findViewById(R.id.btnExportCsv);
        btnExportPdf = findViewById(R.id.btnExportPdf);

        db = FirebaseFirestore.getInstance();

        btnClose.setOnClickListener(v -> Toast.makeText(this, "Use Close Session on QR screen", Toast.LENGTH_SHORT).show());
        btnExportCsv.setOnClickListener(v -> Toast.makeText(this, "CSV export coming soon", Toast.LENGTH_SHORT).show());
        btnExportPdf.setOnClickListener(v -> Toast.makeText(this, "PDF export coming soon", Toast.LENGTH_SHORT).show());

        loadHeader();
        loadAttendance();
    }

    private void loadHeader() {
        db.collection("lecturerSessions").document(sessionId).get().addOnSuccessListener(s -> {
            String d = s.getString("date");
            String st = s.getString("start_time");
            String et = s.getString("end_time");
            String lecturerName = s.getString("lecturer_name");
            if (lecturerName == null) lecturerName = "";
            String title = (unitName != null ? unitName : unitCode) + " (" + unitCode + ")";
            String time = (d != null ? d : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date())) + ", " + (st != null ? st : "--") + " - " + (et != null ? et : "--");
            tvHeader.setText("Session Info\n" + title + "\n" + time + "\n" + lecturerName);
        });
    }

    private void loadAttendance() {
        // 1) Get roster for unit
        db.collection("studentUnits").whereEqualTo("unit_code", unitCode).get().addOnSuccessListener(roster -> {
            Set<String> studentIds = new HashSet<>();
            for (DocumentSnapshot d : roster.getDocuments()) {
                String uid = d.getString("student_id");
                if (uid != null) studentIds.add(uid);
            }
            int total = studentIds.size();

            // 2) Read attendance docs for this session
            db.collection("lecturerSessions").document(sessionId).collection("attendance").get().addOnSuccessListener(attSnap -> {
                Map<String, String> statusById = new HashMap<>();
                for (DocumentSnapshot a : attSnap.getDocuments()) {
                    statusById.put(a.getId(), a.getString("status"));
                }

                // 3) Enrich names/regs from 'students' collection in batches of 10 using documentId whereIn
                List<String> idList = new ArrayList<>(studentIds);
                Map<String, String> nameById = new HashMap<>();
                Map<String, String> regById = new HashMap<>();
                if (idList.isEmpty()) {
                    renderAttendance(studentIds, statusById, nameById, regById, total);
                    return;
                }

                final int totalIds = idList.size();
                final int[] batchesLeft = { (int) Math.ceil(totalIds / 10.0) };
                for (int i = 0; i < totalIds; i += 10) {
                    List<String> batch = idList.subList(i, Math.min(i + 10, totalIds));
                    db.collection("students").whereIn(FieldPath.documentId(), batch).get().addOnSuccessListener(stSnap -> {
                        for (DocumentSnapshot s : stSnap.getDocuments()) {
                            String id = s.getId();
                            String nm = s.getString("full_name");
                            String rg = s.getString("reg_number");
                            if (id != null) {
                                if (nm != null) nameById.put(id, nm);
                                if (rg != null) regById.put(id, rg);
                            }
                        }
                    }).addOnCompleteListener(t -> {
                        batchesLeft[0]--;
                        if (batchesLeft[0] <= 0) {
                            renderAttendance(studentIds, statusById, nameById, regById, total);
                        }
                    });
                }
            });
        });
    }

    private void renderAttendance(Set<String> studentIds,
                                  Map<String, String> statusById,
                                  Map<String, String> nameById,
                                  Map<String, String> regById,
                                  int total) {
        rows.clear();
        int presentCount = 0;
        for (String uid : studentIds) {
            String fullName = nameById.getOrDefault(uid, uid);
            String reg = regById.getOrDefault(uid, uid);
            String status = statusById.get(uid);
            String label = ("Present".equalsIgnoreCase(status)) ? "Present" : "Absent";
            if ("Present".equalsIgnoreCase(label)) presentCount++;
            rows.add(new Row(fullName, reg, label));
        }
        adapter.notifyDataSetChanged();

        int absent = total - presentCount;
        int pct = (total > 0) ? Math.round(100f * presentCount / total) : 0;
        tvSummary.setText("Total: " + total + "    Present: " + presentCount + "    Absent: " + absent + "    Attendance: " + pct + "%");
    }

    static class Row {
        String name; String reg; String status;
        Row(String n, String r, String s) { name=n; reg=r; status=s; }
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        List<Row> data;
        Adapter(List<Row> d) { data=d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_lecturer_attendance_row, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Row it = data.get(position);
            h.tvName.setText(it.name);
            h.tvReg.setText(it.reg);
            h.tvStatus.setText("Present".equalsIgnoreCase(it.status) ? "✅ Present" : "❌ Absent");
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvReg, tvStatus;
            VH(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvReg = itemView.findViewById(R.id.tvReg);
                tvStatus = itemView.findViewById(R.id.tvStatus);
            }
        }
    }
}
