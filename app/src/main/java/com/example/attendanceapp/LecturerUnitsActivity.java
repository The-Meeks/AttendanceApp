package com.example.attendanceapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LecturerUnitsActivity extends AppCompatActivity {

    private static final int TERM_SESSIONS_CAP = 12;

    private RecyclerView rvUnits;
    private UnitsAdapter adapter;
    private final List<UnitRow> rows = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lecturer_units);

        rvUnits = findViewById(R.id.rvLecturerUnits);
        rvUnits.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UnitsAdapter(rows);
        rvUnits.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadUnits();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rows.clear();
        adapter.notifyDataSetChanged();
        loadUnits();
    }

    private void loadUnits() {
        db.collection("lecturerUnits")
                .whereEqualTo("lecturer_id", currentUser.getUid())
                .get()
                .addOnSuccessListener(q -> {
                    LinkedHashMap<String, String> codeToName = new LinkedHashMap<>();
                    for (QueryDocumentSnapshot d : q) {
                        String code = d.getString("unit_code");
                        String name = d.getString("unit_name");
                        if (code != null && name != null && !codeToName.containsKey(code)) {
                            codeToName.put(code, name);
                        }
                    }
                    if (codeToName.isEmpty()) {
                        Toast.makeText(this, "No units assigned", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // For each unit, count sessions created so far (cap 12)
                    for (Map.Entry<String, String> e : codeToName.entrySet()) {
                        String code = e.getKey(); String name = e.getValue();
                        db.collection("lecturerSessions")
                                .whereEqualTo("unit_code", code)
                                .get()
                                .addOnSuccessListener(ss -> {
                                    int created = Math.min(ss.size(), TERM_SESSIONS_CAP);
                                    rows.add(new UnitRow(name, code, created));
                                    adapter.notifyItemInserted(rows.size() - 1);
                                });
                    }
                });
    }

    static class UnitRow {
        String name; String code; int created;
        UnitRow(String n, String c, int cr) { name=n; code=c; created=cr; }
    }

    class UnitsAdapter extends RecyclerView.Adapter<UnitsAdapter.VH> {
        List<UnitRow> data;
        UnitsAdapter(List<UnitRow> d) { data = d; }
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_lecturer_unit, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            UnitRow it = data.get(position);
            h.tvTitle.setText(it.name);
            h.tvSub.setText(it.code);
            h.tvCount.setText(it.created + "/" + TERM_SESSIONS_CAP);
            h.itemView.setOnClickListener(v -> {
                Intent i = new Intent(LecturerUnitsActivity.this, LecturerUnitSessionsActivity.class);
                i.putExtra("unit_code", it.code);
                i.putExtra("unit_name", it.name);
                startActivity(i);
            });
        }
        @Override public int getItemCount() { return data.size(); }
        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSub, tvCount;
            VH(@NonNull View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvUnitTitle);
                tvSub = itemView.findViewById(R.id.tvUnitSub);
                tvCount = itemView.findViewById(R.id.tvProgressCount);
            }
        }
    }
}
