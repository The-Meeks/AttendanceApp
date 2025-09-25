package com.example.attendanceapp;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class UnitReportAdapter extends RecyclerView.Adapter<UnitReportAdapter.ViewHolder> {

    private List<UnitReport> reportList;

    public UnitReportAdapter(List<UnitReport> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_unit_report, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnitReport report = reportList.get(position);
        
        holder.tvUnitName.setText(report.getDisplayName());
        holder.tvUnitCode.setText(report.getUnitCode());
        holder.tvSessionInfo.setText("Sessions: " + report.getTotalSessions() + " | Students: " + report.getTotalStudents());
        holder.tvAttendancePercentage.setText(report.getAttendancePercentage() + "%");
        
        // Set color based on attendance percentage
        try {
            holder.tvAttendancePercentage.setTextColor(Color.parseColor(report.getAttendanceColor()));
        } catch (IllegalArgumentException e) {
            // Fallback to default color if parsing fails
            holder.tvAttendancePercentage.setTextColor(Color.parseColor("#2ECC71"));
        }
    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvUnitName, tvUnitCode, tvSessionInfo, tvAttendancePercentage;
        
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUnitName = itemView.findViewById(R.id.tvUnitName);
            tvUnitCode = itemView.findViewById(R.id.tvUnitCode);
            tvSessionInfo = itemView.findViewById(R.id.tvSessionInfo);
            tvAttendancePercentage = itemView.findViewById(R.id.tvAttendancePercentage);
        }
    }
}
