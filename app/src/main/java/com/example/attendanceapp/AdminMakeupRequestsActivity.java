package com.example.attendanceapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminMakeupRequestsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private RequestsAdapter adapter;
    private final List<DocumentSnapshot> requests = new ArrayList<>();

    private FirebaseFirestore db;
    private ListenerRegistration listener;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mma", Locale.getDefault());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_makeup_requests);

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerRequests);
        tvEmpty = findViewById(R.id.tvEmptyRequests);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RequestsAdapter();
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) { listener.remove(); listener = null; }
    }

    private void attachListener() {
        if (listener != null) return;
        listener = db.collection("makeupRequests")
                .whereEqualTo("status", "pending")
                .orderBy("requested_at", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    requests.clear();
                    if (snap != null) {
                        requests.addAll(snap.getDocuments());
                    }
                    tvEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();
                });
    }

    private class RequestsAdapter extends RecyclerView.Adapter<RequestsAdapter.VH> {
        @NonNull @Override public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_makeup_request, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int position) {
            DocumentSnapshot doc = requests.get(position);
            String unitName = doc.getString("unit_name");
            String unitCode = doc.getString("unit_code");
            String date = doc.getString("requested_date");
            Long startMs = doc.getLong("requested_start_millis");
            Long endMs = doc.getLong("requested_end_millis");
            String lecturerId = doc.getString("lecturer_id");

            h.tvTitle.setText(unitName + " (" + unitCode + ")");
            String timeWindow;
            if (startMs != null && endMs != null) {
                timeWindow = timeFormat.format(new java.util.Date(startMs)) + " - " + timeFormat.format(new java.util.Date(endMs));
            } else {
                timeWindow = "";
            }
            h.tvSubtitle.setText("Date: " + (date != null ? date : "-") + (timeWindow.isEmpty() ? "" : ("\nTime: " + timeWindow)));

            h.btnApprove.setOnClickListener(v -> approve(doc));
            h.btnReject.setOnClickListener(v -> reject(doc));
        }
        @Override public int getItemCount() { return requests.size(); }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle; Button btnApprove, btnReject;
            VH(@NonNull View itemView) { super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }

    private void approve(DocumentSnapshot doc) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        // Optimistic remove for instant UI feedback
        removeFromList(doc.getId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "approved");
        updates.put("approved_by", admin != null ? admin.getUid() : "");
        updates.put("approved_at", Timestamp.now());
        db.collection("makeupRequests").document(doc.getId())
                .update(updates)
                .addOnSuccessListener(v -> Toast.makeText(this, "Approved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void reject(DocumentSnapshot doc) {
        FirebaseUser admin = FirebaseAuth.getInstance().getCurrentUser();
        // Optimistic remove for instant UI feedback
        removeFromList(doc.getId());
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("rejected_by", admin != null ? admin.getUid() : "");
        updates.put("rejected_at", Timestamp.now());
        db.collection("makeupRequests").document(doc.getId())
                .update(updates)
                .addOnSuccessListener(v -> Toast.makeText(this, "Rejected", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void removeFromList(String id) {
        int idx = -1;
        for (int i = 0; i < requests.size(); i++) {
            if (id.equals(requests.get(i).getId())) { idx = i; break; }
        }
        if (idx >= 0) {
            requests.remove(idx);
            adapter.notifyItemRemoved(idx);
            tvEmpty.setVisibility(requests.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
}
