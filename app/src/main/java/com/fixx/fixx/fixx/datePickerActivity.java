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
        setContentView(R.layout.activity_date_picker);

        setTitle("Select Arrival Dates");

        Intent thisIntent = this.getIntent();
        mode = thisIntent.getStringExtra("mode");
        System.out.println(mode);
        category = thisIntent.getStringExtra("category");
        System.out.println(category);
        description = thisIntent.getStringExtra("description");
        System.out.println(description);

        propertyID = userInfo.get("PropertyID");

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
        for (int i = 0; i < selectedDates.size(); i++) {
            datesList = datesList + selectedDates.get(i) + ",";
        }
        datesList = datesList.substring(0, datesList.length() - 1);
        System.out.println(datesList);
        System.out.println(propertyID);
        System.out.println(mediaID);
        uploadRequest(identityID + String.valueOf(requestNumber), category, description, mediaID,
                propertyID, datesList, "Incomplete", " ", identityID);
        requestNumber++;
        userInfo.put("RequestNumber", String.valueOf(requestNumber));
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
