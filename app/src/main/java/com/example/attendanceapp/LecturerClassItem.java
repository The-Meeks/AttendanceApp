package com.example.attendanceapp;

public class LecturerClassItem {
    public String unitCode;
    public String unitName;
    public String startTime;
    public String endTime;
    public boolean rescheduled = false;
    public String rescheduleText = null;

    public LecturerClassItem(String unitCode, String unitName, String startTime, String endTime) {
        this.unitCode = unitCode;
        this.unitName = unitName;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
