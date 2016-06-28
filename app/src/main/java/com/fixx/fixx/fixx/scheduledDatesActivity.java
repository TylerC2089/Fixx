package com.fixx.fixx.fixx;

import android.app.usage.UsageEvents;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.EventLog;
import android.view.View;
import android.widget.CalendarView;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.github.sundeepk.compactcalendarview.CompactCalendarView;
import com.github.sundeepk.compactcalendarview.domain.CalendarDayEvent;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class scheduledDatesActivity extends ActionBarActivity {

    CompactCalendarView calendarView;
    TextView monthView;
    TextView descriptionText;
    TextView timeText;
    TextView techName;
    Spinner statusSpinner;
    ImageView techImageView;
    Map<Date, Map<String, String>> scheduledDates = new HashMap<>();

    String[] statusArray;
    List<String> statusList = new ArrayList<>();

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;

    Dataset userInfo = MainMenuActivity.userInfo;
    int numberOfRequests = Integer.valueOf(userInfo.get("RequestNumber"));
    String identityID = MainMenuActivity.credentialsProvider.getIdentityId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scheduled_dates);

        calendarView = (CompactCalendarView)findViewById(R.id.compactcalendar_view);
        monthView = (TextView)findViewById(R.id.textView);
        descriptionText = (TextView)findViewById(R.id.problemDescription);
        timeText = (TextView)findViewById(R.id.arrivalTime);
        techName = (TextView)findViewById(R.id.technicianName);
        statusSpinner = (Spinner)findViewById(R.id.statusSelect);
        techImageView = (ImageView)findViewById(R.id.technicianImage);

        calendarView.shouldScrollMonth(false);

        int month = calendarView.getFirstDayOfCurrentMonth().getMonth();
        monthView.setText(getStringMonth(month));

        statusArray = getResources().getStringArray(R.array.status_states);
        for (String s : statusArray) {
            statusList.add(s);
        }

        // TODO: Add logic for downloading and displaying all scheduled dates using the below line
        addDate("6-4-2016", "15:00", "Leaking pipe needs fixed.", "Anton", "Incomplete", "http://zblogged.com/wp-content/uploads/2015/11/5.png");
        for (int i = 0; i < numberOfRequests; i++) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("RequestID", new AttributeValue(identityID + String.valueOf(i)));
            GetItemRequest req = new GetItemRequest("FixxRequests", key);
            dynamo.getItemAsync(req, new AsyncHandler<GetItemRequest, GetItemResult>() {
                @Override
                public void onError(Exception e) {
                    System.out.println("Error: Could not get job request");
                }

                @Override
                public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
                    final Map<String, AttributeValue> requestItem = getItemResult.getItem();
                    if (requestItem.get("TechnicianID").getS().equals(" ")) {
                        String times = "";
                        String dateTimes[] = requestItem.get("RepairDate").getS().split("~|~");
                        for (int i = 0; i < dateTimes.length; i++) {
                            String dateTime = dateTimes[i];
                            String date = dateTime.substring(0, dateTime.indexOf(" "));
                            times = dateTime.substring(dateTime.indexOf(" "));
                            addDate(date, times, requestItem.get("Details").getS(),
                                    "", requestItem.get("Status").getS(), "");
                        }
                    }
                    Map<String, AttributeValue> key = new HashMap<String, AttributeValue>(1);
                    key.put("UserID", requestItem.get("TechnicianID"));
                    GetItemRequest req = new GetItemRequest("FixxUsers", key);
                    dynamo.getItemAsync(req, new AsyncHandler<GetItemRequest, GetItemResult>() {
                        @Override
                        public void onError(Exception e) {
                            System.out.println("Error: Could not get technician");
                        }

                        @Override
                        public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
                            Map<String, AttributeValue> technicianItem = getItemResult.getItem();
                            String times = "";
                            String dateTimes[] = requestItem.get("RepairDate").getS().split("~|~");
                            for (int i = 0; i < dateTimes.length; i++) {
                                String dateTime = dateTimes[i];
                                String date = dateTime.substring(0, dateTime.indexOf(" "));
                                times = dateTime.substring(dateTime.indexOf(" "));
                                addDate(date, times, requestItem.get("Details").getS(),
                                        technicianItem.get("FirstName").getS() + " " + technicianItem.get("LastName").getS(),
                                        requestItem.get("Status").getS(), "");
                            }
                        }
                    });
                }
            });
        }
        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        calendarView.setListener(new CompactCalendarView.CompactCalendarViewListener() {
            @Override
            public void onDayClick(Date dateClicked) {
                Map<String, String> jobData = scheduledDates.get(dateClicked);
                if (jobData != null) {
                    descriptionText.setText(jobData.get("Description"));
                    timeText.setText(jobData.get("Time"));
                    techName.setText(jobData.get("TechName"));
                    int index = statusList.indexOf(jobData.get("Status"));
                    statusSpinner.setSelection(index);
                    ImageLoader imageLoader = ImageLoader.getInstance();
                    imageLoader.displayImage(jobData.get("TechImageURL"), techImageView);
                }
            }

            @Override
            public void onMonthScroll(Date firstDayOfNewMonth) {

            }
        });
    }

    private void addDate (String date, String time, String description, String techName, String status, String techImageURL) {
        // Add the date to the calendar
        Date formattedDate = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("M-d-yyyy");
        formattedDate = formatter.parse(date, new ParsePosition(0));
        long millis = formattedDate.getTime();
        CalendarDayEvent ev = new CalendarDayEvent(millis, Color.BLACK);
        calendarView.addEvent(ev);
        // Create a new job entry
        Map<String, String> jobData = new HashMap<>();
        jobData.put("Date", date);
        jobData.put("Time", time);
        jobData.put("Description", description);
        jobData.put("TechName", techName);
        jobData.put("Status", status);
        jobData.put("TechImageURL", techImageURL);
        scheduledDates.put(formattedDate, jobData);
    }

    private String getStringMonth (int month) {
        if (month == 0) {
            return "January";
        } else if (month == 1) {
            return "February";
        } else if (month == 2) {
            return "March";
        } else if (month == 3) {
            return "April";
        } else if (month == 4) {
            return "May";
        } else if (month == 5) {
            return "June";
        } else if (month == 6) {
            return "July";
        } else if (month == 7) {
            return "August";
        } else if (month == 8) {
            return "September";
        } else if (month == 9) {
            return "October";
        } else if (month == 10) {
            return "November";
        } else if (month == 11) {
            return "December";
        } else {
            return "";
        }
    }



    public void showPreviousMonth (View v) {
        calendarView.showPreviousMonth();
        int month = calendarView.getFirstDayOfCurrentMonth().getMonth();
        monthView.setText(getStringMonth(month));
        calendarView.animate();
    }

    public void showNextMonth (View v) {
        calendarView.showNextMonth();
        int month = calendarView.getFirstDayOfCurrentMonth().getMonth();
        monthView.setText(getStringMonth(month));
        calendarView.animate();
    }

}
