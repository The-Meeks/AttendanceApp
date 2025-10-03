package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LecturerUnitSessionsActivity extends AppCompatActivity {

    private static final int TERM_SESSIONS_CAP = 12;

    private TextView tvHeader;
    private RecyclerView rv;
    private final List<Row> rows = new ArrayList<>();
    private Adapter adapter;

    private FirebaseFirestore db;
    private String unitCode;
    private String unitName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_unit_sessions);

        unitCode = getIntent().getStringExtra("unit_code");
        unitName = getIntent().getStringExtra("unit_name");

        tvHeader = findViewById(R.id.tvUnitHeader);
        rv = findViewById(R.id.rvSessions);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter(rows);
        rv.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();

        tvHeader.setText((unitName != null ? unitName : unitCode) + " • Sessions");

        load();
    }

    private void load() {
        db.collection("lecturerSessions")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(snap -> {
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot d : snap) docs.add(d);
                    // sort client-side by start_millis asc
                    docs.sort((a, b) -> {
                        Long sa = a.getLong("start_millis");
                        Long sb = b.getLong("start_millis");
                        long va = sa != null ? sa : 0L; long vb = sb != null ? sb : 0L;
                        return Long.compare(va, vb);
                    });
                    rows.clear();
                    SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    int taught = Math.min(docs.size(), TERM_SESSIONS_CAP);
                    for (int i = 0; i < taught; i++) {
                        QueryDocumentSnapshot d = docs.get(i);
                        Long st = d.getLong("start_millis");
                        String sub = st != null && st > 0 ? df.format(new Date(st)) : "";
                        rows.add(new Row("Session " + (i + 1), sub, "Taught", d.getId()));
                    }
                    for (int i = taught; i < TERM_SESSIONS_CAP; i++) {
                        rows.add(new Row("Session " + (i + 1), "", "Upcoming", null));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    static class Row {
        String title; String subtitle; String badge; String sessionId;
        Row(String t, String s, String b, String id) { title=t; subtitle=s; badge=b; sessionId=id; }
    }

    class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        List<Row> data;
        Adapter(List<Row> d) { data=d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_lecturer_session, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            Row it = data.get(position);
            h.tvTitle.setText(it.title);
            h.tvSub.setText(it.subtitle);
            h.tvBadge.setText(it.badge);
            h.itemView.setEnabled(it.sessionId != null);
            h.itemView.setAlpha(it.sessionId != null ? 1f : 0.6f);
            h.itemView.setOnClickListener(v -> {
                if (it.sessionId == null) return;
                Intent i = new Intent(LecturerUnitSessionsActivity.this, LecturerSessionDetailActivity.class);
                i.putExtra("unit_code", unitCode);
                i.putExtra("unit_name", unitName);
                i.putExtra("session_id", it.sessionId);
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub, tvBadge;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvSessionTitle);
                tvSub = itemView.findViewById(R.id.tvSessionSubtitle);
                tvBadge = itemView.findViewById(R.id.tvStatusBadge);
            }
        }
    }
}
