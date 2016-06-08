package com.fixx.fixx.fixx;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.regions.Regions;

import java.util.List;

public class loginActivity extends ActionBarActivity {

    EditText accessCodeInput;

    // Variables for Amazon Cognito and user authentication
    private Dataset userInfo;
    private CognitoSyncManager syncManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connect to UI
        accessCodeInput = (EditText)findViewById(R.id.accessCodeInput);

        // TODO: replace identity pool ID
        CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),    /* get the context for the application */
                "us-east-1:c74e2b8e-7104-4a51-b028-70596fb9dbc8",    /* Identity Pool ID */
                Regions.US_EAST_1           /* Region for your identity pool--US_EAST_1 or EU_WEST_1*/
        );

        // Link the sync manager to the Cognito credentials
        syncManager = new CognitoSyncManager(this, Regions.US_EAST_1, credentialsProvider);
        userInfo = getUserInfo(syncManager, "UserInfo");
    }

    public void login (View v) {
        userInfo.put("AccessCode", accessCodeInput.getText().toString());
        userInfo.synchronize(new Dataset.SyncCallback() {
            @Override
            public void onSuccess(Dataset dataset, List<Record> list) {
                returnToMainMenu();
                finish();
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
                returnToMainMenu();
                finish();
            }
        });
    }

    private Dataset getUserInfo (CognitoSyncManager manager, String dataSetKey) {
        Dataset userData = manager.openOrCreateDataset(dataSetKey);
        return userData;
    }

    private void returnToMainMenu () {
        Intent mainMenuIntent = new Intent(this, MainMenuActivity.class);
        startActivity(mainMenuIntent);
    }
}
