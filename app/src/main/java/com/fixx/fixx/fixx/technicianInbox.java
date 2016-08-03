package com.fixx.fixx.fixx;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.amazonaws.services.dynamodbv2.model.QueryResult;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class technicianInbox extends ActionBarActivity {

    Context activityContext;

    private LinearLayout inboxContainer;

    private List<Map<String, AttributeValue>> jobRequests = new ArrayList<>();
    private List<String> items = new ArrayList<>();

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;
    Dataset userInfo = MainMenuActivity.userInfo;
    String identityID = MainMenuActivity.identityID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_technician_inbox);

        activityContext = this;

        inboxContainer = (LinearLayout)findViewById(R.id.inboxContainer);

        ScanRequest req = new ScanRequest("FixxRequests");
        req.addExpressionAttributeNamesEntry("#T", "TechnicianID");
        req.addExpressionAttributeValuesEntry(":i", new AttributeValue(identityID));
        req.setFilterExpression("#T = :i");
        dynamo.scanAsync(req, new AsyncHandler<ScanRequest, ScanResult>() {
            @Override
            public void onError(Exception e) {
                System.out.println("Error: Could not get job request: " + e.getMessage());
            }

            @Override
            public void onSuccess(ScanRequest request, final ScanResult queryResult) {
                System.out.println("Got the request!");
                final List<Map<String, AttributeValue>> requestItems = queryResult.getItems();
                for (final Map<String, AttributeValue> requestItem : requestItems) {
                    Map<String, AttributeValue> key = new HashMap<String, AttributeValue>(1);
                    key.put("UserID", requestItem.get("TenantID"));
                    GetItemRequest req = new GetItemRequest("FixxUsers", key);
                    dynamo.getItemAsync(req, new AsyncHandler<GetItemRequest, GetItemResult>() {
                        @Override
                        public void onError(Exception e) {
                            System.out.println("Error: Could not get tenant: " + e.getMessage());
                        }

                        @Override
                        public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
                            System.out.println("Got the tenant!");
                            final Map<String, AttributeValue> tenantItem = getItemResult.getItem();
                            Map<String, AttributeValue> key = new HashMap<String, AttributeValue>(1);
                            key.put("PropertyID", requestItem.get("PropertyID"));
                            GetItemRequest req = new GetItemRequest("FixxProperties", key);
                            dynamo.getItemAsync(req, new AsyncHandler<GetItemRequest, GetItemResult>() {
                                @Override
                                public void onError(Exception e) {
                                    System.out.println("Could not get property: " + e.getMessage());
                                }

                                @Override
                                public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
                                    System.out.println("Got the property!");
                                    Map<String, AttributeValue> propertyItem = getItemResult.getItem();
                                    Map<String, AttributeValue> jobRequest = new HashMap<String, AttributeValue>(23);
                                    jobRequest.putAll(requestItem);
                                    jobRequest.putAll(tenantItem);
                                    jobRequest.putAll(propertyItem);
                                    jobRequests.add(jobRequest);
                                    items.add(jobRequest.get("Details").getS());
                                }
                            });
                        }
                    });
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        final ViewGroup vg = (ViewGroup)inboxContainer;
                        for (int i = 0; i < vg.getChildCount(); i++) {
                            vg.removeView(vg.getChildAt(i));
                        }
                        for (int i = 0; i < items.size(); i++) {
                            final Button entry = new Button(activityContext);
                            entry.setWidth(inboxContainer.getWidth());
                            entry.setHeight(100);
                            entry.setText(items.get(i).toString());
                            entry.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    int index = vg.indexOfChild(entry);
                                    viewJobRequest(jobRequests.get(index));
                                }
                            });
                            vg.addView(entry);
                        }
                    }
                });
            }
        });
    }

    private void viewJobRequest (Map<String, AttributeValue> jobRequest) {
        Intent viewRequest = new Intent(this, requestView.class);
        String addressLine1 = jobRequest.get("Address").getS();
        String aptNumber = jobRequest.get("AptNumber").getS();
        if (aptNumber != null) {
            addressLine1 = addressLine1 + " Apt." + aptNumber;
        }
        String addressLine2 = jobRequest.get("City").getS() + ", " + jobRequest.get("State").getS() +
                " " + jobRequest.get("ZipCode").getS();
        viewRequest.putExtra("AddressLine1", addressLine1);
        viewRequest.putExtra("AddressLine2", addressLine2);
        viewRequest.putExtra("TenantName", jobRequest.get("FirstName").getS() + " " + jobRequest.get("LastName").getS());
        viewRequest.putExtra("HasPet", jobRequest.get("HasPet").getS());
        viewRequest.putExtra("NumberOfOccupants", jobRequest.get("NumberOfOccupants").getS());
        viewRequest.putExtra("MediaID", jobRequest.get("MediaID").getS());
        viewRequest.putExtra("Details", jobRequest.get("Details").getS());
        viewRequest.putExtra("RepairDate", jobRequest.get("RepairDate").getS());
        viewRequest.putExtra("RequestID", jobRequest.get("RequestID").getS());
        startActivity(viewRequest);
    }
}
