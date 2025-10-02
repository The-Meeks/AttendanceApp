package com.example.attendanceapp;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

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
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentUnitSessionsActivity extends AppCompatActivity {

    private static final int TERM_SESSIONS_CAP = 12;

    private RecyclerView rvSessions;
    private TextView tvUnitHeader;
    private final List<SessionRow> rows = new ArrayList<>();
    private SessionsAdapter adapter;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    private String unitCode;
    private String unitName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_unit_sessions);

        unitCode = getIntent().getStringExtra("unit_code");
        unitName = getIntent().getStringExtra("unit_name");

        tvUnitHeader = findViewById(R.id.tvUnitHeader);
        rvSessions = findViewById(R.id.rvSessions);
        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SessionsAdapter(rows);
        rvSessions.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        String header = (unitName != null ? unitName : unitCode) + " • Session List";
        tvUnitHeader.setText(header);

        if (unitCode == null || currentUser == null) {
            return;
        }

        loadSessions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Auto refresh session list when returning to this screen
        if (unitCode != null && currentUser != null) {
            rows.clear();
            adapter.notifyDataSetChanged();
            loadSessions();
        }
    }

    private void loadSessions() {
        db.collection("lecturerSessions")
                .whereEqualTo("unit_code", unitCode)
                .get()
                .addOnSuccessListener(sessions -> {
                    rows.clear();
                    List<QueryDocumentSnapshot> docs = new ArrayList<>();
                    for (QueryDocumentSnapshot s : sessions) docs.add(s);
                    // Sort client-side by start_millis ASC and cap at 12
                    docs.sort((a, b) -> {
                        Long sa = a.getLong("start_millis");
                        Long sb = b.getLong("start_millis");
                        long va = sa != null ? sa : 0L; long vb = sb != null ? sb : 0L;
                        return Long.compare(va, vb);
                    });
                    if (docs.size() > TERM_SESSIONS_CAP) docs = docs.subList(0, TERM_SESSIONS_CAP);

                    List<String> ids = new ArrayList<>();
                    List<Long> starts = new ArrayList<>();
                    for (QueryDocumentSnapshot s : docs) {
                        ids.add(s.getId());
                        Long st = s.getLong("start_millis");
                        starts.add(st != null ? st : 0L);
                    }
                    // First, add realized sessions with placeholder status (to be filled asynchronously)
                    SimpleDateFormat df = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    for (int i = 0; i < ids.size(); i++) {
                        String title = "Session " + (i + 1);
                        String subtitle = starts.get(i) > 0 ? df.format(new Date(starts.get(i))) : "";
                        rows.add(new SessionRow(title, subtitle, "", R.drawable.bg_badge_upcoming));
                    }
                    // Fill remaining as upcoming placeholders up to 12
                    for (int i = ids.size(); i < TERM_SESSIONS_CAP; i++) {
                        String title = "Session " + (i + 1);
                        rows.add(new SessionRow(title, "", "Upcoming", R.drawable.bg_badge_upcoming));
                    }
                    adapter.notifyDataSetChanged();

                    // Now fetch attendance status for realized sessions
                    for (int i = 0; i < ids.size(); i++) {
                        final int index = i;
                        db.collection("lecturerSessions").document(ids.get(i))
                                .collection("attendance").document(currentUser.getUid())
                                .get()
                                .addOnSuccessListener(a -> {
                                    String status = a.getString("status");
                                    applyStatusToRow(index, status);
                                })
                                .addOnFailureListener(e -> applyStatusToRow(index, null));
                    }
                })
                .addOnFailureListener(e -> {
                    // Show 12 upcoming rows even if query fails
                    rows.clear();
                    for (int i = 0; i < TERM_SESSIONS_CAP; i++) {
                        rows.add(new SessionRow("Session " + (i + 1), "", "Upcoming", R.drawable.bg_badge_upcoming));
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void applyStatusToRow(int index, String status) {
        if (index < 0 || index >= rows.size()) return;
        // Only two states for realized sessions: Present or Absent.
        // For upcoming (not created) rows we keep the pre-filled "Upcoming".
        String current = rows.get(index).status;
        if ("Upcoming".equalsIgnoreCase(current)) {
            return;
        }
        String label;
        int bg;
        if ("Present".equalsIgnoreCase(status)) {
            label = "Present"; bg = R.drawable.bg_badge_present;
        } else {
            label = "Absent"; bg = R.drawable.bg_badge_absent;
        }
        rows.get(index).status = label;
        rows.get(index).statusBg = bg;
        adapter.notifyItemChanged(index);
    }

    static class SessionRow {
        String title; String subtitle; String status; int statusBg;
        SessionRow(String t, String sub, String s, int b) { title=t; subtitle=sub; status=s; statusBg=b; }
    }

    class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.VH> {
        List<SessionRow> data;
        SessionsAdapter(List<SessionRow> d) { data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_student_session, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            SessionRow it = data.get(position);
            h.tvSessionTitle.setText(it.title);
            h.tvSessionSubtitle.setText(it.subtitle);
            h.tvStatusBadge.setText(it.status != null && !it.status.isEmpty() ? it.status : "Upcoming");
            h.tvStatusBadge.setBackgroundResource(it.statusBg);
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvSessionTitle, tvSessionSubtitle, tvStatusBadge;
            VH(@NonNull View itemView) {
                super(itemView);
                tvSessionTitle = itemView.findViewById(R.id.tvSessionTitle);
                tvSessionSubtitle = itemView.findViewById(R.id.tvSessionSubtitle);
                tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            }
        }
    }
}
