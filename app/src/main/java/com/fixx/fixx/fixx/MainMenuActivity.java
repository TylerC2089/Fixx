package com.fixx.fixx.fixx;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.regions.Regions;
import com.amazonaws.mobileconnectors.cognito.Dataset;

import java.util.List;

public class MainMenuActivity extends ActionBarActivity {

    // Variables for Amazon Cognito and user authentication
    CognitoCachingCredentialsProvider credentialsProvider;
    private Dataset userInfo;
    private CognitoSyncManager syncManager;
    private String accessCode;
    private String userID;

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
        // Get the unique identity ID for the user
        getUserID.execute();

        // Link the sync manager to the Cognito credentials
        syncManager = new CognitoSyncManager(this, Regions.US_EAST_1, credentialsProvider);
        userInfo = getUserInfo(syncManager, "UserInfo");
        userInfo.synchronize(new Dataset.SyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, List<Record> list) {
                accessCode = userInfo.get("AccessCode");
                System.out.println(accessCode);
                if (accessCode == null) {
                    Intent loginIntent = new Intent(getApplicationContext(), loginActivity.class);
                    startActivity(loginIntent);
                    finish();
                }
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

    }

    public void openCamera (View v) {
        Intent cameraIntent = new Intent(this, CameraCaptureActivity.class);
        startActivity(cameraIntent);
    }

    public void openCalendar (View v) {
        Intent calendarIntent = new Intent(this, scheduledDatesActivity.class);
        startActivity(calendarIntent);
    }

    private AsyncTask getUserID = new AsyncTask() {
        @Override
        protected Object doInBackground(Object[] params) {
            userID = credentialsProvider.getIdentityId();
            return null;
        }
    };

    private Dataset getUserInfo (CognitoSyncManager manager, String dataSetKey) {
        Dataset userData = manager.openOrCreateDataset(dataSetKey);
        return userData;
    }
}
