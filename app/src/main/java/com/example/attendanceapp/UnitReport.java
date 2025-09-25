package com.example.attendanceapp;

public class UnitReport {
    private String unitCode;
    private String unitName;
    private int attendancePercentage;
    private int totalSessions;
    private int totalStudents;

    public UnitReport() {
        // Default constructor required for Firestore
    }

    public UnitReport(String unitCode, String unitName, int attendancePercentage, int totalSessions, int totalStudents) {
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.attendancePercentage = attendancePercentage;
        this.totalSessions = totalSessions;
        this.totalStudents = totalStudents;
    }

    // Getters and setters
    public String getUnitCode() {
        return unitCode;
    }

    public void setUnitCode(String unitCode) {
        this.unitCode = unitCode;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public int getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(int attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public int getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(int totalSessions) {
        this.totalSessions = totalSessions;
    }

    public int getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(int totalStudents) {
        this.totalStudents = totalStudents;
    }

    public String getDisplayName() {
        return unitName != null && !unitName.isEmpty() ? unitName : unitCode;
    }

    public String getAttendanceColor() {
        if (attendancePercentage >= 90) {
            return "#2ECC71"; // Green
        } else if (attendancePercentage >= 80) {
            return "#27AE60"; // Dark green
        } else if (attendancePercentage >= 70) {
            return "#F39C12"; // Orange
        } else {
            return "#E74C3C"; // Red
        }
    }
}
