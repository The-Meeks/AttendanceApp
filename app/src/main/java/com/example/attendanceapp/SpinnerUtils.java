package com.example.attendanceapp;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

public class SpinnerUtils {

    // Setup Department Spinner
    public static void setupDepartmentSpinner(Context context, Spinner spinner) {
        List<String> departments = new ArrayList<>();
        departments.add("Computer Science");
        departments.add("Information Technology");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, departments);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    // Setup Course Spinner based on Department
    public static void setupCourseSpinner(Context context, Spinner spinner, String department) {
        List<String> courses = new ArrayList<>();

        if (department.equals("Computer Science")) {
            courses.add("BSc Computer Science");
        } else if (department.equals("Information Technology")) {
            courses.add("BSc Information Technology");
            courses.add("BSc Business Information Technology");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, courses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
}
