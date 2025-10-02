package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeLabel, tvStudentName, btnScanQR, tvAlertText, btnLogoutStudent;
    private LinearLayout cardMyUnits, cardAttendanceReport, alertCard;
    private RecyclerView rvUpcoming;
    private UpcomingAdapter upcomingAdapter;
    private final List<UpcomingItem> upcoming = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // UI references
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tvWelcomeLabel = findViewById(R.id.tvWelcomeStudent);
        tvStudentName = findViewById(R.id.tvStudentName);
        btnScanQR = findViewById(R.id.btnScanQR);
        cardMyUnits = findViewById(R.id.cardMyUnits);
        cardAttendanceReport = findViewById(R.id.cardAttendanceReport);
        alertCard = findViewById(R.id.alertCard);
        tvAlertText = findViewById(R.id.tvAlertText);
        btnLogoutStudent = findViewById(R.id.btnLogoutStudent);
        rvUpcoming = findViewById(R.id.rvUpcomingClasses);
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        upcomingAdapter = new UpcomingAdapter(upcoming);
        rvUpcoming.setAdapter(upcomingAdapter);

        // Display student's name from Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("students").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("full_name");
                            if (fullName != null) {
                                tvStudentName.setText(fullName);
                            }
                        }
                    });
            // Load upcoming classes and alerts
            loadUpcoming(uid);
            computeAlert(uid);
        }

        // Actions
        btnScanQR.setOnClickListener(v -> startActivity(new Intent(this, StudentScanQRActivity.class)));
        cardMyUnits.setOnClickListener(v -> startActivity(new Intent(this, StudentUnitsActivity.class)));
        cardAttendanceReport.setOnClickListener(v -> startActivity(new Intent(this, StudentViewAttendanceActivity.class)));
        btnLogoutStudent.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(this, StudentLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadUpcoming(String uid) {
        // Fetch student's units then map to catalog to get schedule
        db.collection("studentUnits").whereEqualTo("student_id", uid).get().addOnSuccessListener(snap -> {
            List<String> unitCodes = new ArrayList<>();
            Map<String, String> unitNames = new HashMap<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String code = d.getString("unit_code");
                String name = d.getString("unit_name");
                if (code != null) { unitCodes.add(code); unitNames.put(code, name); }
            }
            if (unitCodes.isEmpty()) { upcoming.clear(); upcomingAdapter.notifyDataSetChanged(); return; }

            db.collection("units").whereIn("unit_code", unitCodes).get().addOnSuccessListener(unitsSnap -> {
                upcoming.clear();
                // Build next few upcoming based on schedule_day and allocated times
                String todayName = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
                for (QueryDocumentSnapshot u : unitsSnap) {
                    String code = u.getString("unit_code");
                    String name = u.getString("unit_name");
                    String day = u.getString("schedule_day");
                    String start = u.getString("allocated_start_time");
                    String end = u.getString("allocated_end_time");
                    String room = u.getString("room");
                    if (code == null) continue;
                    String displayName = name != null ? name : unitNames.getOrDefault(code, code);

                    // Compute next day label (e.g., today or next occurrence within a week)
                    String dayDisp = day != null ? day.substring(0, Math.min(3, day.length())) : "";
                    // Very simple: include all enrolled units as upcoming; prioritize today first
                    UpcomingItem item = new UpcomingItem(displayName, code,
                            (start != null ? start : "--") + " - " + (end != null ? end : "--"),
                            dayDisp,
                            room != null ? room : "");
                    upcoming.add(item);
                }
                // TODO: sort by next occurrence/time if needed
                upcomingAdapter.notifyDataSetChanged();
            });
        });
    }

    private void computeAlert(String uid) {
        // Find any unit where this student missed last 3 sessions
        db.collection("studentUnits").whereEqualTo("student_id", uid).get().addOnSuccessListener(snap -> {
            List<String> codes = new ArrayList<>();
            Map<String, String> names = new HashMap<>();
            for (DocumentSnapshot d : snap.getDocuments()) {
                String c = d.getString("unit_code");
                String n = d.getString("unit_name");
                if (c != null) { codes.add(c); if (n != null) names.put(c, n); }
            }
            if (codes.isEmpty()) { alertCard.setVisibility(View.GONE); return; }

            // Check each code's last 3 sessions
            checkNextAlert(uid, codes, names, 0);
        });
    }

    private void checkNextAlert(String uid, List<String> codes, Map<String,String> names, int idx) {
        if (idx >= codes.size()) { return; }
        String code = codes.get(idx);
        db.collection("lecturerSessions")
                .whereEqualTo("unit_code", code)
                .orderBy("start_millis", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .addOnSuccessListener(sessions -> {
                    final int[] abs = {0};
                    List<String> sessionIds = new ArrayList<>();
                    for (DocumentSnapshot s : sessions.getDocuments()) { sessionIds.add(s.getId()); }
                    if (sessionIds.isEmpty()) { checkNextAlert(uid, codes, names, idx+1); return; }

                    // Chain through attendance subcollections
                    checkAttendanceForSessions(uid, code, names.getOrDefault(code, code), sessionIds, 0, 0, () -> {
                        // If not triggered, move to next
                        checkNextAlert(uid, codes, names, idx+1);
                    });
                });
    }

    private interface Done { void go(); }

    private void checkAttendanceForSessions(String uid, String unitCode, String unitName, List<String> sessionIds, int i, int absentCount, Done done) {
        if (i >= sessionIds.size()) {
            // No alert condition met
            done.go();
            return;
        }
        String sid = sessionIds.get(i);
        db.collection("lecturerSessions").document(sid)
                .collection("attendance")
                .document(uid)
                .get()
                .addOnSuccessListener(aDoc -> {
                    String status = aDoc.getString("status");
                    int nextAbsent = absentCount + ("Absent".equalsIgnoreCase(status) ? 1 : 0);
                    if (nextAbsent >= 3) {
                        alertCard.setVisibility(View.VISIBLE);
                        tvAlertText.setText("You have missed 3 consecutive sessions in " + unitName + ". Please see your lecturer.");
                        // Stop checking further once one alert is shown
                    } else {
                        checkAttendanceForSessions(uid, unitCode, unitName, sessionIds, i+1, nextAbsent, done);
                    }
                })
                .addOnFailureListener(e -> checkAttendanceForSessions(uid, unitCode, unitName, sessionIds, i+1, absentCount, done));
    }

    // Data + Adapter for upcoming list
    static class UpcomingItem {
        String title; String code; String time; String day; String room;
        UpcomingItem(String title, String code, String time, String day, String room) {
            this.title = title; this.code = code; this.time = time; this.day = day; this.room = room;
        }
    }

    class UpcomingAdapter extends RecyclerView.Adapter<UpcomingAdapter.VH> {
        List<UpcomingItem> data;
        UpcomingAdapter(List<UpcomingItem> d) { data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.View v = getLayoutInflater().inflate(R.layout.item_upcoming_class, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            UpcomingItem it = data.get(position);
            h.tvClassTitle.setText(it.title);
            h.tvClassTime.setText(it.time);
            h.tvClassDay.setText(it.day);
            h.tvClassRoom.setText(it.room != null && !it.room.isEmpty() ? it.room : "");
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvClassTitle, tvClassTime, tvClassDay, tvClassRoom;
            VH(@NonNull View itemView) { super(itemView);
                tvClassTitle = itemView.findViewById(R.id.tvClassTitle);
                tvClassTime = itemView.findViewById(R.id.tvClassTime);
                tvClassDay = itemView.findViewById(R.id.tvClassDay);
                tvClassRoom = itemView.findViewById(R.id.tvClassRoom);
            }
        }
    }
}
