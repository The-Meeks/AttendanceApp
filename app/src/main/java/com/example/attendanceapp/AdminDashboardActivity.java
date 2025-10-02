package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;

    // Quick Stats
    private TextView tvTotalStudents, tvTotalLecturers, tvTotalUnits;
    private TextView tvEmptyReports;

    // Firestore
    private FirebaseFirestore db;

    private UnitReportAdapter unitReportAdapter;
    private final List<UnitReport> unitReports = new ArrayList<>();
    
    // Real-time listeners
    private ListenerRegistration sessionsListener;
    private ListenerRegistration enrollmentsListener;
    private ListenerRegistration unitsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Firestore
        db = FirebaseFirestore.getInstance();

        // Quick Stats TextViews
        tvTotalStudents = findViewById(R.id.tvTotalStudents);
        tvTotalLecturers = findViewById(R.id.tvTotalLecturers);
        tvTotalUnits = findViewById(R.id.tvTotalUnits);
        tvEmptyReports = findViewById(R.id.tvEmptyReports);

        loadQuickStats();

        // Unit reports list
        RecyclerView recyclerView = findViewById(R.id.recyclerUnitReports);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        unitReportAdapter = new UnitReportAdapter(unitReports);
        recyclerView.setAdapter(unitReportAdapter);
        loadUnitReports();

        // Lecturer Management actions
        View rowAssignUnits = findViewById(R.id.rowAssignUnits);
        if (rowAssignUnits != null) {
            rowAssignUnits.setOnClickListener(v -> {
                startActivity(new Intent(this, AllocateUnitsActivity.class));
            });
        }

        View rowApproveMakeups = findViewById(R.id.rowApproveMakeups);
        if (rowApproveMakeups != null) {
            rowApproveMakeups.setOnClickListener(v -> {
                startActivity(new Intent(this, AdminMakeupRequestsActivity.class));
            });
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        setupRealtimeListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach listeners to avoid leaks
        if (sessionsListener != null) { sessionsListener.remove(); sessionsListener = null; }
        if (enrollmentsListener != null) { enrollmentsListener.remove(); enrollmentsListener = null; }
        if (unitsListener != null) { unitsListener.remove(); unitsListener = null; }
    }

    private void setupRealtimeListeners() {
        // Listen for new/updated sessions
        if (sessionsListener == null) {
            sessionsListener = db.collection("lecturerSessions")
                    .addSnapshotListener((snap, e) -> {
                        if (e == null) {
                            loadUnitReports();
                        }
                    });
        }
        // Listen for enrollment changes
        if (enrollmentsListener == null) {
            enrollmentsListener = db.collection("studentUnits")
                    .addSnapshotListener((snap, e) -> {
                        if (e == null) {
                            loadUnitReports();
                        }
                    });
        }
        // Listen for units changes (affects both stats and reports)
        if (unitsListener == null) {
            unitsListener = db.collection("units")
                    .addSnapshotListener((snap, e) -> {
                        if (e == null) {
                            loadQuickStats();
                            loadUnitReports();
                        }
                    });
        }
    }

    private void loadQuickStats() {
        // Students count
        db.collection("students").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    tvTotalStudents.setText(String.valueOf(snapshot.size()));
                }
            }
        });

        // Lecturers count
        db.collection("lecturers").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    tvTotalLecturers.setText(String.valueOf(snapshot.size()));
                }
            }
        });

        // Units count
        db.collection("units").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot snapshot = task.getResult();
                if (snapshot != null) {
                    tvTotalUnits.setText(String.valueOf(snapshot.size()));
                }
            }
        });
    }

    private void loadUnitReports() {
        // First get all units
        db.collection("units").get().addOnSuccessListener(unitsQuery -> {
            Map<String, UnitReport> unitReportMap = new HashMap<>();
            
            // Initialize reports for all units
            for (QueryDocumentSnapshot unitDoc : unitsQuery) {
                String unitCode = unitDoc.getString("unit_code");
                String unitName = unitDoc.getString("unit_name");
                if (unitCode != null) {
                    UnitReport report = new UnitReport(unitCode, unitName, 0, 0, 0);
                    unitReportMap.put(unitCode, report);
                }
            }
            
            // Get all sessions and calculate attendance
            db.collection("lecturerSessions").get().addOnSuccessListener(sessionsQuery -> {
                Map<String, Integer> sessionCounts = new HashMap<>();
                Map<String, Integer> totalAttendanceSum = new HashMap<>();
                
                for (QueryDocumentSnapshot sessionDoc : sessionsQuery) {
                    String unitCode = sessionDoc.getString("unit_code");
                    if (unitCode != null && unitReportMap.containsKey(unitCode)) {
                        // Count sessions per unit
                        sessionCounts.put(unitCode, sessionCounts.getOrDefault(unitCode, 0) + 1);
                        
                        // Sum attendance percentages
                        Long attendancePct = sessionDoc.getLong("attendancePercentage");
                        int percentage = attendancePct != null ? attendancePct.intValue() : 0;
                        totalAttendanceSum.put(unitCode, totalAttendanceSum.getOrDefault(unitCode, 0) + percentage);
                    }
                }
                
                // Get student enrollment counts for each unit
                db.collection("studentUnits").get().addOnSuccessListener(enrollmentQuery -> {
                    Map<String, Integer> enrollmentCounts = new HashMap<>();
                    
                    for (QueryDocumentSnapshot enrollDoc : enrollmentQuery) {
                        String unitCode = enrollDoc.getString("unit_code");
                        if (unitCode != null) {
                            enrollmentCounts.put(unitCode, enrollmentCounts.getOrDefault(unitCode, 0) + 1);
                        }
                    }
                    
                    // Update unit reports with calculated data
                    unitReports.clear();
                    for (UnitReport report : unitReportMap.values()) {
                        String unitCode = report.getUnitCode();
                        int sessionCount = sessionCounts.getOrDefault(unitCode, 0);
                        int totalAttendance = totalAttendanceSum.getOrDefault(unitCode, 0);
                        int enrolledStudents = enrollmentCounts.getOrDefault(unitCode, 0);
                        
                        // Calculate average attendance percentage
                        int avgAttendance = sessionCount > 0 ? totalAttendance / sessionCount : 0;
                        
                        report.setTotalSessions(sessionCount);
                        report.setAttendancePercentage(avgAttendance);
                        report.setTotalStudents(enrolledStudents);
                        
                        // Only add units that have sessions
                        if (sessionCount > 0) {
                            unitReports.add(report);
                        }
                    }
                    
                    // Update UI
                    updateReportsUI();
                });
            });
        });
    }
    
    private void updateReportsUI() {
        if (unitReports.isEmpty()) {
            tvEmptyReports.setVisibility(View.VISIBLE);
        } else {
            tvEmptyReports.setVisibility(View.GONE);
        }
        unitReportAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_register_students) {
            startActivity(new Intent(this, RegisterStudentsActivity.class));
        } else if (id == R.id.nav_register_lecturers) {
            startActivity(new Intent(this, RegisterLecturersActivity.class));
        }
        else if (id == R.id.nav_manage_units) {
            startActivity(new Intent(this, RegisterUnitsActivity.class));
        }else if (id == R.id.nav_allocate_units) {
            startActivity(new Intent(this, AllocateUnitsActivity.class));
        } else if (id == R.id.nav_enroll_students) {
            startActivity(new Intent(this, EnrollCourseActivity.class));
        } else if (id == R.id.nav_promotion) {
            startActivity(new Intent(this, PromoteCourseActivity.class));
        } else if (id == R.id.nav_remove_users) {
            startActivity(new Intent(this, RemoveUserActivity.class));
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
