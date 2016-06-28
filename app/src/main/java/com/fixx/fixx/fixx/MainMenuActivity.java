package com.fixx.fixx.fixx;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Parcelable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.auth.policy.resources.S3BucketResource;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.S3ClientCache;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.ec2.model.S3Storage;
import com.amazonaws.services.iot.model.S3Action;
import com.amazonaws.services.s3.AmazonS3Client;

import java.io.Serializable;
import java.util.List;

public class MainMenuActivity extends ActionBarActivity {

    // Variables for Amazon Cognito and user authentication
    public static CognitoCachingCredentialsProvider credentialsProvider;
    public static Dataset userInfo;
    private CognitoSyncManager syncManager;
    private String firstName;

    // Variables for Dynamo DB
    public static AmazonDynamoDBAsyncClient dynamo;

    // Variables for S3
    public static AmazonS3Client s3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        // TODO: replace identity pool ID
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "us-east-1:c74e2b8e-7104-4a51-b028-70596fb9dbc8",    /* Identity Pool ID */
                Regions.US_EAST_1           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );

        // Link DynamoDB Client to the Cognito credentials
        dynamo = new AmazonDynamoDBAsyncClient(credentialsProvider);
        // Link S3 Client to the Cognito credentials
        s3 = new AmazonS3Client(credentialsProvider);
        // Link the sync manager to the Cognito credentials
        syncManager = new CognitoSyncManager(this, Regions.US_EAST_1, credentialsProvider);
        userInfo = getUserInfo(syncManager, "UserInfo");
        userInfo.synchronizeOnConnectivity(new Dataset.SyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, List<Record> list) {
            }

            @Override
            public boolean onConflict(Dataset dataset, List<SyncConflict> list) {
                return false;
            }

            @Override
            public boolean onDatasetDeleted(Dataset dataset, String s) {
                return false;
            }

            @Override
            public boolean onDatasetsMerged(Dataset dataset, List<String> list) {
                return false;
            }

            @Override
            public void onFailure(DataStorageException e) {
            }
        });

        firstName = userInfo.get("FirstName");
        System.out.println("FirstName=" + firstName);
        if (firstName == null) {
            Intent loginIntent = new Intent(getApplicationContext(), loginActivity.class);
            startActivity(loginIntent);
            finish();
        }
    }

    public void openCamera (View v) {
        Intent cameraIntent = new Intent(this, CameraCaptureActivity.class);
        startActivity(cameraIntent);
    }

    public void openCalendar (View v) {
        Intent calendarIntent = new Intent(this, scheduledDatesActivity.class);
        startActivity(calendarIntent);
    }

    private Dataset getUserInfo (CognitoSyncManager manager, String dataSetKey) {
        Dataset userData = manager.openOrCreateDataset(dataSetKey);
        return userData;
    }
}
