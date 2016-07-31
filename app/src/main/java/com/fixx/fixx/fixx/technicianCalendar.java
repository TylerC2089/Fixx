package com.fixx.fixx.fixx;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class technicianCalendar extends ActionBarActivity {

    CalendarView calendarView;
    TableLayout scheduleTable;
    TextView dateText;
    Button nextDay;
    Button prevDay;

    Rect selectionArea;
    Map<String, String> activeJobRequest = new HashMap<>();
    int requestStartTime = 0;
    int requestEndTime = 0;
    // Date -> hourMap, hourMap: hour -> jobRequest
    Map<String, Map<String, Map<String, String>>> calendarModel = new HashMap<>();
    List<String> scheduledDates = new ArrayList<>();

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;
    Dataset userInfo = MainMenuActivity.userInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_calendar);

        calendarView = (CalendarView)findViewById(R.id.calendarView);
        scheduleTable = (TableLayout)findViewById(R.id.scheduleTable);
        dateText = (TextView)findViewById(R.id.dateText);
        nextDay = (Button)findViewById(R.id.nextButton);
        prevDay = (Button)findViewById(R.id.prevButton);

        String[] requestArray = userInfo.get("RequestRegistry").toString().split(",");
        String activeRequestID = requestArray[requestArray.length - 1];

        for (int i = 0; i < requestArray.length; i++) {
            String requestID = requestArray[i];
            Map<String, String> jobRequest = new HashMap<>();
            jobRequest.put("AddressLine1", userInfo.get(requestID + ":AddressLine1").toString());
            jobRequest.put("AddressLine2", userInfo.get(requestID + ":AddressLine2").toString());
            jobRequest.put("TenantName", userInfo.get(requestID + ":TenantName").toString());
            jobRequest.put("HasPet", userInfo.get(requestID + ":HasPet").toString());
            jobRequest.put("NumberOfOccupants", userInfo.get(requestID + ":NumberOfOccupants").toString());
            jobRequest.put("JobDetails", userInfo.get(requestID + ":JobDetails").toString());
            jobRequest.put("RepairDate", userInfo.get(requestID + ":RepairDate").toString());
            jobRequest.put("TimeRange", userInfo.get(requestID + ":TimeRange").toString());
            jobRequest.put("MediaID", userInfo.get(requestID + ":MediaID").toString());
            if (!jobRequest.get("TimeRange").equals("")) {
                Map<String, Map<String, String>> hourRequestMap = new HashMap<>();
                if (calendarModel.containsKey(jobRequest.get("RepairDate"))) {
                    hourRequestMap = calendarModel.get(jobRequest.get("RepairDate"));
                }
                int hour = Integer.parseInt(jobRequest.get("TimeRange").split("-")[0]);
                int endHour = Integer.parseInt(jobRequest.get("TimeRange").split("-")[1]);
                while (hour <= endHour) {
                    hourRequestMap.put(String.valueOf(hour), jobRequest);
                    hour++;
                }
                calendarModel.put(jobRequest.get("RepairDate"), hourRequestMap);
            }
        }

        String mode = getIntent().getStringExtra("Mode");
        if (mode.equals("scheduleRequest")) {
            calendarView.setVisibility(View.INVISIBLE);
            activeJobRequest.put("AddressLine1", userInfo.get(activeRequestID + ":AddressLine1"));
            activeJobRequest.put("AddressLine2", userInfo.get(activeRequestID + ":AddressLine2"));
            activeJobRequest.put("TenantName", userInfo.get(activeRequestID + ":TenantName"));
            activeJobRequest.put("HasPet", userInfo.get(activeRequestID + ":HasPet"));
            activeJobRequest.put("NumberOfOccupants", userInfo.get(activeRequestID + ":NumberOfOccupants"));
            activeJobRequest.put("MediaID", userInfo.get(activeRequestID + ":MediaID"));
            activeJobRequest.put("Details", userInfo.get(activeRequestID + ":Details"));
            activeJobRequest.put("RepairDate", userInfo.get(activeRequestID + ":RepairDate"));
            activeJobRequest.put("RequestID", userInfo.get(activeRequestID + ":RequestID"));
            String[] repairDatesArray = activeJobRequest.get("RepairDate").split(",");
            for (int i = 0; i < repairDatesArray.length; i++) {
                scheduledDates.add(repairDatesArray[i]);
            }
            nextDay.setVisibility(View.VISIBLE);
            prevDay.setVisibility(View.VISIBLE);
            nextDay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int nextDateIndex = scheduledDates.indexOf(dateText.getText()) + 1;
                    if (nextDateIndex < scheduledDates.size()) {
                        String date = scheduledDates.get(nextDateIndex);
                        refreshDateEvents(date);
                    }
                }
            });
            prevDay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int prevDateIndex = scheduledDates.indexOf(dateText.getText()) - 1;
                    if (prevDateIndex >= 0) {
                        String date = scheduledDates.get(scheduledDates.indexOf(dateText.getText()));
                        refreshDateEvents(date);
                    }
                }
            });
            refreshDateEvents(scheduledDates.get(0));
        } else {
            dateText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    calendarView.setVisibility(View.VISIBLE);
                }
            });
        }

        // Setup selection area
        Point screenBounds = new Point();
        getWindowManager().getDefaultDisplay().getSize(screenBounds);
        selectionArea = new Rect(0, 0, screenBounds.x, 0);

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (activeJobRequest != null) {
            int touches = event.getPointerCount();
            if (touches == 2) {
                float firstTouchY = event.getY(0);
                float secondTouchY = event.getY(1);
                if (firstTouchY <= secondTouchY) {
                    selectionArea.top = (int) firstTouchY;
                    selectionArea.bottom = (int) secondTouchY;
                } else {
                    selectionArea.top = (int) secondTouchY;
                    selectionArea.bottom = (int) firstTouchY;
                }
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        for (int i = 0; i < scheduleTable.getChildCount(); i++) {
                            View row = scheduleTable.getChildAt(i);
                            Rect rowBounds = new Rect();
                            row.getHitRect(rowBounds);
                            if (Rect.intersects(selectionArea, rowBounds)) {
                                // Get the row elements
                                ViewGroup rowGroup = (ViewGroup) row;
                                EditText numberText = (EditText) rowGroup.getChildAt(0);
                                Button rowButton = (Button) rowGroup.getChildAt(1);
                                // Set the button attributes
                                String address = activeJobRequest.get("Address");
                                rowButton.setText(address);
                                rowButton.setBackgroundColor(Color.RED);
                                // Change time range if necessary
                                int number = Integer.parseInt(numberText.getText().toString());
                                if (number < requestStartTime) {
                                    requestStartTime = number;
                                }
                                if (number > requestEndTime) {
                                    requestEndTime = number;
                                }
                            }
                        }
                    case MotionEvent.ACTION_UP:
                        // Setup selection area
                        Point screenBounds = new Point();
                        getWindowManager().getDefaultDisplay().getSize(screenBounds);
                        selectionArea = new Rect(0, 0, screenBounds.x, 0);
                        // Clear the active job request
                        activeJobRequest = null;
                }
            }
        }
        return false;
    }

    private void refreshDateEvents (String date) {
        dateText.setText(date);
        Map<String, Map<String, String>> hourRequestMap = calendarModel.get(date);
        if (hourRequestMap != null) {
            Set<String> hours = hourRequestMap.keySet();
            for (String hour : hours) {
                ViewGroup row = (ViewGroup) scheduleTable.getChildAt(Integer.parseInt(hour) - 1);
                Button btn = (Button) row.getChildAt(1);
                // Set the button attributes
                String address = hourRequestMap.get(hour).get("AddressLine1");
                btn.setText(address);
                btn.setBackgroundColor(Color.RED);
            }
        }
    }
}
