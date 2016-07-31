package com.fixx.fixx.fixx;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CalendarView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
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

    // Variables for Amazon Cognito and user authentication
    private Dataset userInfo = MainMenuActivity.userInfo;
    private String identityID = MainMenuActivity.credentialsProvider.getIdentityId();
    private String propertyID;
    private int requestNumber = 0;

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;

    // Variables for s3
    private AmazonS3Client s3 = MainMenuActivity.s3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_picker);

        requestNumber = Integer.valueOf(userInfo.get("RequestNumber"));
        propertyID = userInfo.get("PropertyID");

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

    private void uploadRequest (String requestID, String category, String details, String mediaID,
                                String propertyID, String repairDate, String status,
                                String technicianID, String tenantID) {
        Map<String, AttributeValue> requestAttributes = new HashMap<>(9);
        requestAttributes.put("RequestID", new AttributeValue(requestID));
        requestAttributes.put("Category", new AttributeValue(category));
        requestAttributes.put("Details", new AttributeValue(details));
        requestAttributes.put("MediaID", new AttributeValue(mediaID));
        requestAttributes.put("PropertyID", new AttributeValue(propertyID));
        requestAttributes.put("RepairDate", new AttributeValue(repairDate));
        requestAttributes.put("Status", new AttributeValue(status));
        requestAttributes.put("TechnicianID", new AttributeValue(technicianID));
        requestAttributes.put("TenantID", new AttributeValue(tenantID));
        PutItemRequest req = new PutItemRequest("FixxRequests", requestAttributes);
        dynamo.putItemAsync(req, new AsyncHandler<PutItemRequest, PutItemResult>() {
            @Override
            public void onError(Exception e) {
                System.out.println("Error: Could not upload request to database");
                System.out.println(e.getMessage());
            }

            @Override
            public void onSuccess(PutItemRequest request, PutItemResult putItemResult) {
                finish();
            }
        });
    }

    private class s3PutObjectAsync extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String[] params) {
            String bucket = params[0].toString();
            String key = params[1].toString();
            File file = new File(params[2]);
            s3.putObject(bucket, key, file);
            return "";
        }

        @Override
        protected void onPostExecute(String message) {
            //process message
        }
    }

    private String publishMedia (String mediaType) {
        String mediaID = "";
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Fixx/";
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        if (mediaType.equals("picture")) {
            int index = 1;
            String filePath = path + "Image" + String.valueOf(index) + ".jpg";
            File image = new File(filePath);
            while (image.exists()) {
                s3PutObjectAsync putObjectAsync = new s3PutObjectAsync();
                putObjectAsync.execute("fixx-media", identityID + String.valueOf(requestNumber) + "Image"
                        + String.valueOf(index) + ".jpg", filePath);
                mediaID = mediaID + identityID + String.valueOf(requestNumber) + "Image"
                        + String.valueOf(index) + ".jpg,";
                index++;
                filePath = path + "Image" + String.valueOf(index) + ".jpg";
                image = new File(filePath);
            }
            mediaID = mediaID.substring(0, mediaID.length() - 1);
        } else if (mediaType.equals("video")) {
            String filePath = path + "Video.3gp";
            s3PutObjectAsync putObjectAsync = new s3PutObjectAsync();
            putObjectAsync.execute("fixx-media", identityID + String.valueOf(requestNumber) + "Video.3gp",
                    filePath);
            mediaID = identityID + String.valueOf(requestNumber) + "Video.3gp";
        }
        return mediaID;
    }

    private void uploadMedia (String mediaID, String locationURL, String propertyID, String type,
                              String userID) {
        Map<String, AttributeValue> requestAttributes = new HashMap<>(5);
        requestAttributes.put("MediaID", new AttributeValue(mediaID));
        requestAttributes.put("LocationURL", new AttributeValue(locationURL));
        requestAttributes.put("PropertyID", new AttributeValue(propertyID));
        requestAttributes.put("Type", new AttributeValue(type));
        requestAttributes.put("UserID", new AttributeValue(userID));
        PutItemRequest req = new PutItemRequest("FixxMedia", requestAttributes);
        dynamo.putItemAsync(req);
    }

    public void uploadJobRequest(View v) {
        String mediaID = publishMedia(mode);
        System.out.println(mediaID);
        if (mode.equals("video")) {
            uploadMedia(mediaID, "https://s3.amazonaws.com/fixx-media/" + mediaID, propertyID, mode,
                    identityID);
        } else if (mode.equals("picture")) {
            String[] pictureIDs = mediaID.split(",");
            for (String s : pictureIDs) {
                System.out.println(s);
                uploadMedia(s, "https://s3.amazonaws.com/fixx-media/" + s, propertyID, mode,
                        identityID);
            }
        }
        String datesList = "";
        for (int i = 0; i < dates.length; i++) {
            datesList = datesList + dates[i] + " ";
            datesList = datesList + dateTimeMap.get(dates[i]) + "~|~";
        }
        datesList = datesList.substring(0, datesList.length() - 3);
        uploadRequest(identityID + String.valueOf(requestNumber), category, description, mediaID,
                propertyID, datesList, "Incomplete", " ", identityID);
        requestNumber++;
        userInfo.put("RequestNumber", String.valueOf(requestNumber));
    }

    public void setTime (View v) {
        System.out.println("time changed");
        String date = spinner.getSelectedItem().toString();
        String hour = String.valueOf(timePicker.getCurrentHour());
        String minute = String.valueOf(timePicker.getCurrentMinute());
        if (minute.length() < 2) {
            minute = "0" + minute;
        }
        String time = hour + ":" + minute;
        dateTimeMap.put(date, time);
    }
}
