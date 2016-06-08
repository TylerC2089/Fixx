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

import java.util.HashMap;
import java.util.Map;

public class timePickerActivity extends ActionBarActivity {

    Spinner spinner;
    TimePicker timePicker;
    ArrayAdapter adapter;

    String mode = "picture";
    String description = "";
    String category = "Property";
    Map<String, String> dateTimeMap = new HashMap<>();
    String[] dates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker);

        setTitle("Select Arrival Times");

        // Get extras from prior activities
        Intent thisIntent = getIntent();
        mode = thisIntent.getStringExtra("mode");
        System.out.println(mode);
        description = thisIntent.getStringExtra("description");
        System.out.println(description);
        category = thisIntent.getStringExtra("category");
        System.out.println(category);
        dates = thisIntent.getExtras().getStringArray("dates");
        System.out.println(dates.length);

        // Connect to the UI elements
        spinner = (Spinner)findViewById(R.id.spinner);
        timePicker = (TimePicker)findViewById(R.id.timePicker);

        // Setup the spinner array adapter
        adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, dates);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set the time for each date to a default value
        for (int i = 0; i < dates.length; i++) {
            dateTimeMap.put(dates[i], "0:0");
        }

        // Set a listener to change the selected time in accordance with a newly selected date
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("New date selected");
                String time = "";
                String date = spinner.getSelectedItem().toString();
                time = dateTimeMap.get(date);
                String[] timeArray = time.split(":");
                timePicker.setCurrentHour(Integer.parseInt(timeArray[0]));
                timePicker.setCurrentMinute(Integer.parseInt(timeArray[1]));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    public void uploadJobRequest(View v) {
        // For testing purposes
        System.out.println("Category: " + category);
        System.out.println("Description: " + description);
        for (Map.Entry<String, String> e : dateTimeMap.entrySet()) {
            System.out.println("Date: " + e.getKey() + ", Time: " + e.getValue());
        }
        finish();
    }

    public void setTime (View v) {
        System.out.println("time changed");
        String date = spinner.getSelectedItem().toString();
        String time = String.valueOf(timePicker.getCurrentHour()) + ":" + String.valueOf(timePicker.getCurrentMinute());
        dateTimeMap.put(date, time);
    }
}
