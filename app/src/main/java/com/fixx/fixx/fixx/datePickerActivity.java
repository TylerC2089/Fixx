package com.fixx.fixx.fixx;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class datePickerActivity extends ActionBarActivity {

    CalendarView calendar;
    Spinner spinner;
    ArrayAdapter adapter;

    String mode = "picture";
    String category = "Property";
    String description = "";
    List<String> selectedDates = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_picker);

        setTitle("Select Arrival Dates");

        Intent thisIntent = this.getIntent();
        mode = thisIntent.getStringExtra("mode");
        System.out.println(mode);
        category = thisIntent.getStringExtra("category");
        System.out.println(category);
        description = thisIntent.getStringExtra("description");
        System.out.println(description);

        // Connect to the UI elements
        calendar = (CalendarView)findViewById(R.id.calendarView);
        spinner = (Spinner)findViewById(R.id.spinner);

        // Setup the spinner array adapter
        adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set a listener on the calendar to add each selected date to a list
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                // Format date
                String date = String.valueOf(month) + "-" + String.valueOf(dayOfMonth) + "-" + String.valueOf(year);
                if (!selectedDates.contains(date)) {
                    // Add date to selected dates
                    selectedDates.add(date);
                    // Add date to the spinner
                    adapter.add(date);
                }
            }
        });
    }

    public void next (View v) {
        Intent timePickerIntent = new Intent(this, timePickerActivity.class);
        timePickerIntent.putExtra("mode", mode);
        timePickerIntent.putExtra("category", category);
        timePickerIntent.putExtra("description", description);
        String[] selectedDatesArray = new String[selectedDates.size()];
        for (int i = 0; i < selectedDates.size(); i++) {
            selectedDatesArray[i] = selectedDates.get(i);
        }
        timePickerIntent.putExtra("dates", selectedDatesArray);
        startActivity(timePickerIntent);
        finish();
    }

    public void removeDate (View v) {
        adapter.remove(spinner.getSelectedItem());
        selectedDates.remove(spinner.getSelectedItemPosition());
    }
}
