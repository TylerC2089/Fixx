package com.fixx.fixx.fixx;

import android.content.Intent;
import android.opengl.Visibility;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Layout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class jobRequestSubmissionActivity extends ActionBarActivity {

    CalendarView calendar;
    Spinner spinner;
    TimePicker timePicker;
    EditText descriptionInput;

    String mode = "picture";
    String category = "Property";
    List<String> selectedDates = new ArrayList<>();
    Map<String, String> dateTimeMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_job_request_submission);

        Intent thisIntent = this.getIntent();
        mode = thisIntent.getStringExtra("mode");

        // Connect to the UI elements
        calendar = (CalendarView)findViewById(R.id.calendarView);
        spinner = (Spinner)findViewById(R.id.spinner);
        timePicker = (TimePicker)findViewById(R.id.timePicker);
        descriptionInput = (EditText)findViewById(R.id.description);

        // Setup the spinner array adapter
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Set a listener on the calendar to add each selected date to a list
        calendar.setOnDateChangeListener(new CalendarView.OnDateChangeListener() {
            @Override
            public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
                // Format date
                String date = String.valueOf(month) + "/" + String.valueOf(dayOfMonth) + "/" + String.valueOf(year);
                // Add date to selected dates
                selectedDates.add(date);
                // Add date to the spinner
                adapter.add(date);
                // Add default date-time entry
                dateTimeMap.put(date, "0:0");
            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            @Override
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                String date = spinner.getSelectedItem().toString();
                String time = String.valueOf(hourOfDay) + ":" + String.valueOf(minute);
                dateTimeMap.put(date, time);
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

    public void setCategory (View v) {
        Button btn = (Button)v;
        category = btn.getText().toString();
        View layoutGroup = (View)btn.getParent();
        layoutGroup.setVisibility(View.GONE);
    }

    private void uploadJobRequest() {

    }


}
