package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LecturerDashboardActivity extends AppCompatActivity {

    private TextView tvWelcomeLecturer, btnCreateQR, btnViewAttendance, btnViewUnits, btnLogoutLecturer;
    private RecyclerView rvTodayClasses, rvAlerts;
    private TodayClassesAdapter todayAdapter;
    private AlertsAdapter alertsAdapter;
    private final List<LecturerClassItem> todayList = new ArrayList<>();
    private final List<AlertItem> alertList = new ArrayList<>();

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private DrawerLayout drawerLayout;
    private com.google.android.material.navigation.NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.nav_view_lecturer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        tvWelcomeLecturer = findViewById(R.id.tvWelcomeLecturer);
        btnCreateQR = findViewById(R.id.btnCreateQR);
        btnViewAttendance = findViewById(R.id.btnViewAttendance);
        btnViewUnits = findViewById(R.id.btnViewUnits);
        btnLogoutLecturer = findViewById(R.id.btnLogoutLecturer);
        rvTodayClasses = findViewById(R.id.rvTodayClasses);
        rvAlerts = findViewById(R.id.rvAlerts);

        rvTodayClasses.setLayoutManager(new LinearLayoutManager(this));
        rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new TodayClassesAdapter(todayList);
        alertsAdapter = new AlertsAdapter(alertList);
        rvTodayClasses.setAdapter(todayAdapter);
        rvAlerts.setAdapter(alertsAdapter);

        // Display lecturer's name from Firestore
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection("lecturers").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String fullName = documentSnapshot.getString("full_name");
                            if (fullName != null) {
                                tvWelcomeLecturer.setText("Welcome, " + fullName);
                            } else {
                                tvWelcomeLecturer.setText("Welcome, Lecturer");
                            }
                        }
                    })
                    .addOnFailureListener(e -> tvWelcomeLecturer.setText("Welcome"));
            // Also set drawer header name
            if (navView != null) {
                View header = navView.getHeaderView(0);
                TextView hdrName = header != null ? header.findViewById(R.id.tvLecturerHeaderName) : null;
                db.collection("lecturers").document(uid).get().addOnSuccessListener(d -> {
                    if (d.exists() && hdrName != null) {
                        String full = d.getString("full_name");
                        if (full != null) hdrName.setText(full);
                    }
                });
            }
        }

        // Click listeners
        btnCreateQR.setOnClickListener(v ->
                startActivity(new Intent(this, LecturerGenerateQRActivity.class)));

        btnViewAttendance.setOnClickListener(v ->
                startActivity(new Intent(this, LecturerViewAttendanceActivity.class)));

        btnViewUnits.setOnClickListener(v ->
                startActivity(new Intent(this, ViewUnitsActivity.class)));

        btnLogoutLecturer.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(this, LecturerLoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        // Drawer item handling remains below

        if (navView != null) {
            navView.setNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.nav_today_classes) {
                    // No-op or scroll to top
                } else if (id == R.id.nav_my_units) {
                    startActivity(new Intent(this, ViewUnitsActivity.class));
                } else if (id == R.id.nav_attendance_overview) {
                    startActivity(new Intent(this, LecturerViewAttendanceActivity.class));
                } else if (id == R.id.nav_reports) {
                    startActivity(new Intent(this, LecturerViewAttendanceActivity.class));
                } else if (id == R.id.nav_logout) {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(this, LecturerLoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }
                drawerLayout.closeDrawer(Gravity.START);
                return true;
            });
        }

        loadTodayClasses();
        loadAttendanceAlerts();
    }

    private void loadTodayClasses() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        String today = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date()); // Monday, ...
        SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String todayYmd = ymd.format(new Date());

        db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", uid)
                .get()
                .addOnSuccessListener(lectUnits -> {
                    todayList.clear();
                    Set<String> myUnitCodes = new HashSet<>();
                    Map<String, String> myUnitNames = new HashMap<>();
                    for (QueryDocumentSnapshot d : lectUnits) {
                        String code = d.getString("unit_code");
                        String name = d.getString("unit_name");
                        if (code != null) {
                            myUnitCodes.add(code);
                            if (name != null) myUnitNames.put(code, name);
                        }
                    }
                    if (myUnitCodes.isEmpty()) { todayAdapter.notifyDataSetChanged(); return; }

                    db.collection("units").whereIn("unit_code", new ArrayList<>(myUnitCodes)).get()
                            .addOnSuccessListener(unitsSnap -> {
                                for (QueryDocumentSnapshot u : unitsSnap) {
                                    String code = u.getString("unit_code");
                                    String name = u.getString("unit_name");
                                    String day = u.getString("schedule_day");
                                    String start = u.getString("allocated_start_time");
                                    String end = u.getString("allocated_end_time");
                                    if (code != null && today.equalsIgnoreCase(day)) {
                                        String displayName = name != null ? name : myUnitNames.getOrDefault(code, code);
                                        todayList.add(new LecturerClassItem(code, displayName, start != null ? start : "--", end != null ? end : "--"));
                                    }
                                }
                                // If no classes today, just render
                                if (todayList.isEmpty()) { todayAdapter.notifyDataSetChanged(); return; }

                                // Fetch approved makeups for this lecturer to:
                                // 1) Mark reschedules within this week (not today)
                                // 2) Include make-ups scheduled TODAY even if the unit is not normally on today's weekday
                                db.collection("makeupRequests")
                                        .whereEqualTo("lecturer_id", uid)
                                        .whereEqualTo("status", "approved")
                                        .get()
                                        .addOnSuccessListener((QuerySnapshot rq) -> {
                                            // Compute this week's window (today .. today+6)
                                            Calendar cal = Calendar.getInstance();
                                            Date todayDate;
                                            try { todayDate = ymd.parse(todayYmd); } catch (Exception ex) { todayDate = new Date(); }
                                            cal.setTime(todayDate);
                                            long startOfWindow = cal.getTimeInMillis();
                                            cal.add(Calendar.DAY_OF_YEAR, 6);
                                            long endOfWindow = cal.getTimeInMillis();

                                            Map<String, String> unitRescheduleText = new HashMap<>();
                                            // Track units already listed today for easy lookup
                                            java.util.Set<String> listedToday = new java.util.HashSet<>();
                                            for (LecturerClassItem it : todayList) listedToday.add(it.unitCode);
                                            SimpleDateFormat dayFmt = new SimpleDateFormat("EEE", Locale.getDefault());
                                            for (QueryDocumentSnapshot d : rq) {
                                                String code = d.getString("unit_code");
                                                String reqDate = d.getString("requested_date");
                                                String startStr = d.getString("requested_start_time_str");
                                                String endStr = d.getString("requested_end_time_str");
                                                if (code == null || reqDate == null) continue;
                                                try {
                                                    Date rd = ymd.parse(reqDate);
                                                    if (rd == null) continue;
                                                    long rdm = rd.getTime();
                                                    // If the approved make-up is today, include it in today's classes (even if not normally scheduled today)
                                                    if (reqDate.equals(todayYmd)) {
                                                        if (!listedToday.contains(code)) {
                                                            String displayName = myUnitNames.getOrDefault(code, code);
                                                            todayList.add(new LecturerClassItem(code, displayName,
                                                                    startStr != null ? startStr : "--",
                                                                    endStr != null ? endStr : "--"));
                                                            listedToday.add(code);
                                                        }
                                                        continue;
                                                    }
                                                    // Else, mark reschedule banner for later this week
                                                    if (rdm >= startOfWindow && rdm <= endOfWindow) {
                                                        String label = "Rescheduled to " + dayFmt.format(rd) + (startStr != null && endStr != null ? (" " + startStr + " - " + endStr) : "");
                                                        unitRescheduleText.put(code, label);
                                                    }
                                                } catch (Exception ignore) {}
                                            }

                                            // Apply to today's list
                                            for (LecturerClassItem item : todayList) {
                                                if (unitRescheduleText.containsKey(item.unitCode)) {
                                                    item.rescheduled = true;
                                                    item.rescheduleText = unitRescheduleText.get(item.unitCode);
                                                }
                                            }
                                            todayAdapter.notifyDataSetChanged();
                                        })
                                        .addOnFailureListener(e -> todayAdapter.notifyDataSetChanged());
                            });
                });
    }

    private void loadAttendanceAlerts() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        // Strategy: For each unit the lecturer teaches, get last 2 sessions, find students absent in both
        db.collection("lecturerUnits").whereEqualTo("lecturer_id", uid).get().addOnSuccessListener(lectUnits -> {
            alertList.clear();
            List<String> unitCodes = new ArrayList<>();
            Map<String, String> unitNames = new HashMap<>();
            for (QueryDocumentSnapshot d : lectUnits) {
                String code = d.getString("unit_code");
                String name = d.getString("unit_name");
                if (code != null) { unitCodes.add(code); unitNames.put(code, name); }
            }
            if (unitCodes.isEmpty()) { alertsAdapter.notifyDataSetChanged(); return; }

            for (String code : unitCodes) {
                db.collection("lecturerSessions")
                        .whereEqualTo("lecturer_id", uid)
                        .whereEqualTo("unit_code", code)
                        .orderBy("start_millis", Query.Direction.DESCENDING)
                        .limit(2)
                        .get()
                        .addOnSuccessListener(sessions -> {
                            // Build absent counts
                            Map<String, Integer> absentCount = new HashMap<>();
                            List<String> sessionIds = new ArrayList<>();
                            for (QueryDocumentSnapshot s : sessions) sessionIds.add(s.getId());
                            if (sessionIds.isEmpty()) { alertsAdapter.notifyDataSetChanged(); return; }

                            for (String sid : sessionIds) {
                                db.collection("lecturerSessions").document(sid)
                                        .collection("attendance")
                                        .whereEqualTo("status", "Absent")
                                        .get()
                                        .addOnSuccessListener(absDocs -> {
                                            for (QueryDocumentSnapshot a : absDocs) {
                                                String studentName = a.getString("full_name");
                                                String key = a.getId();
                                                absentCount.put(key + "|" + code + "|" + (studentName != null ? studentName : key),
                                                        absentCount.getOrDefault(key + "|" + code + "|" + (studentName != null ? studentName : key), 0) + 1);
                                            }
                                            // After processing this session's absentees, check if counts reached 2
                                            for (Map.Entry<String, Integer> e : absentCount.entrySet()) {
                                                if (e.getValue() >= 2) {
                                                    String[] parts = e.getKey().split("\\|");
                                                    if (parts.length >= 3) {
                                                        String studentId = parts[0];
                                                        String unitCode = parts[1];
                                                        String name = parts[2];
                                                        String unitName = unitNames.getOrDefault(unitCode, unitCode);
                                                        alertList.add(new AlertItem(name, unitName + " - 2 consecutive absences"));
                                                    }
                                                }
                                            }
                                            alertsAdapter.notifyDataSetChanged();
                                        });
                            }
                        });
            }
        });
    }

    private void startSessionForUnit(String unitCode, String unitName) {
        Intent i = new Intent(this, LecturerGenerateQRActivity.class);
        i.putExtra("unit_code", unitCode);
        i.putExtra("unit_name", unitName);
        startActivity(i);
    }

    private void requestMakeup(String unitCode, String unitName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        // Pick date and time window
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dp = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            Calendar start = Calendar.getInstance();
            start.set(Calendar.YEAR, year);
            start.set(Calendar.MONTH, month);
            start.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog tpStart = new TimePickerDialog(this, (v1, sh, sm) -> {
                Calendar end = (Calendar) start.clone();
                start.set(Calendar.HOUR_OF_DAY, sh);
                start.set(Calendar.MINUTE, sm);
                // Default duration 2 hours for convenience; lecturer can end earlier via picker below
                end.set(Calendar.HOUR_OF_DAY, sh);
                end.set(Calendar.MINUTE, sm);
                end.add(Calendar.HOUR_OF_DAY, 2);

                TimePickerDialog tpEnd = new TimePickerDialog(this, (v2, eh, em) -> {
                    end.set(Calendar.HOUR_OF_DAY, eh);
                    end.set(Calendar.MINUTE, em);

                    long startMillis = start.getTimeInMillis();
                    long endMillis = end.getTimeInMillis();
                    if (endMillis <= startMillis) {
                        Toast.makeText(this, "End time must be after start time", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
                    java.text.SimpleDateFormat tf = new java.text.SimpleDateFormat("hh:mma", java.util.Locale.getDefault());

                    Map<String, Object> req = new HashMap<>();
                    req.put("lecturer_id", user.getUid());
                    req.put("unit_code", unitCode);
                    req.put("unit_name", unitName);
                    req.put("status", "pending");
                    req.put("requested_at", new Date().getTime());
                    req.put("requested_date", df.format(start.getTime()));
                    req.put("requested_start_millis", startMillis);
                    req.put("requested_end_millis", endMillis);
                    req.put("requested_start_time_str", tf.format(start.getTime()));
                    req.put("requested_end_time_str", tf.format(end.getTime()));

                    db.collection("makeupRequests").add(req)
                            .addOnSuccessListener(r -> Toast.makeText(this, "Make-up request submitted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }, end.get(Calendar.HOUR_OF_DAY), end.get(Calendar.MINUTE), false);
                tpEnd.setTitle("Select End Time");
                tpEnd.show();
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false);
            tpStart.setTitle("Select Start Time");
            tpStart.show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dp.setTitle("Select Make-up Date");
        dp.show();
    }

    // Simple alert item
    static class AlertItem {
        String studentName;
        String detail;
        AlertItem(String studentName, String detail) { this.studentName = studentName; this.detail = detail; }
    }

    // Adapter for Today's Classes
    class TodayClassesAdapter extends RecyclerView.Adapter<TodayClassesAdapter.VH> {
        List<LecturerClassItem> data;
        TodayClassesAdapter(List<LecturerClassItem> d) { data = d; }

        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lecturer_class, parent, false);
            return new VH(v);
        }

        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            LecturerClassItem item = data.get(position);
            h.tvUnitTitle.setText(item.unitName + " (" + item.unitCode + ")");
            h.tvUnitTime.setText((item.startTime != null ? item.startTime : "--") + " - " + (item.endTime != null ? item.endTime : "--"));
            TextView tvRes = h.itemView.findViewById(R.id.tvReschedule);
            if (tvRes != null) {
                if (item.rescheduled && item.rescheduleText != null) {
                    tvRes.setVisibility(View.VISIBLE);
                    tvRes.setText(item.rescheduleText);
                } else {
                    tvRes.setVisibility(View.GONE);
                }
            }
            h.btnStartSession.setOnClickListener(v -> startSessionForUnit(item.unitCode, item.unitName));
            h.btnRequestMakeup.setOnClickListener(v -> requestMakeup(item.unitCode, item.unitName));
        }

        @Override public int getItemCount() { return data.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvUnitTitle, tvUnitTime; Button btnStartSession, btnRequestMakeup;
            VH(@NonNull View itemView) {
                super(itemView);
                tvUnitTitle = itemView.findViewById(R.id.tvUnitTitle);
                tvUnitTime = itemView.findViewById(R.id.tvUnitTime);
                btnStartSession = itemView.findViewById(R.id.btnStartSession);
                btnRequestMakeup = itemView.findViewById(R.id.btnRequestMakeup);
            }
        }
    }

    // Adapter for Alerts
    class AlertsAdapter extends RecyclerView.Adapter<AlertsAdapter.VH> {
        List<AlertItem> data;
        AlertsAdapter(List<AlertItem> d) { data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_attendance_alert, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            AlertItem a = data.get(position);
            h.tvStudentName.setText(a.studentName);
            h.tvAlertDetail.setText(a.detail);
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvStudentName, tvAlertDetail;
            VH(@NonNull View itemView) { super(itemView); tvStudentName = itemView.findViewById(R.id.tvStudentName); tvAlertDetail = itemView.findViewById(R.id.tvAlertDetail); }
        }
    }
}
