package com.fixx.fixx.fixx;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.cognito.Record;
import com.amazonaws.mobileconnectors.cognito.SyncConflict;
import com.amazonaws.mobileconnectors.cognito.exceptions.DataStorageException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.simpledb.model.Attribute;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class loginActivity extends ActionBarActivity {

    EditText firstNameInput;
    EditText lastNameInput;
    EditText numberOfOccupants;
    EditText accessCodeInput;
    Spinner roleSelect;
    Button nextButton;
    CheckBox hasPetCheckBox;

    // Variables for Amazon Cognito and user authentication
    private Dataset userInfo = MainMenuActivity.userInfo;
    //private CognitoSyncManager syncManager;
    private String identityID = MainMenuActivity.credentialsProvider.getIdentityId();

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connect to UI
        firstNameInput = (EditText)findViewById(R.id.firstNameInput);
        lastNameInput = (EditText)findViewById(R.id.lastNameInput);
        roleSelect = (Spinner)findViewById(R.id.roleSelect);
        nextButton = (Button)findViewById(R.id.registerButton);
        numberOfOccupants = (EditText)findViewById(R.id.numberOfOccupants);
        hasPetCheckBox = (CheckBox)findViewById(R.id.hasPets);
        accessCodeInput = (EditText)findViewById(R.id.accessCodeInput);

        // Hide the Tenant options
        numberOfOccupants.setVisibility(View.GONE);
        hasPetCheckBox.setVisibility(View.GONE);
        accessCodeInput.setVisibility(View.GONE);

        roleSelect.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selection = roleSelect.getSelectedItem().toString();
                if (selection.equals("Tenant")) {
                    numberOfOccupants.setVisibility(View.VISIBLE);
                    hasPetCheckBox.setVisibility(View.VISIBLE);
                    accessCodeInput.setVisibility(View.VISIBLE);
                } else {
                    numberOfOccupants.setVisibility(View.GONE);
                    hasPetCheckBox.setVisibility(View.GONE);
                    accessCodeInput.setVisibility(View.GONE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void uploadUserToDatabase (String firstName, String lastName, String roleID, String userID) {
        Map<String, AttributeValue> attributes = new HashMap<>(7);
        attributes.put("UserID", new AttributeValue(userID));
        attributes.put("PropertyID", new AttributeValue(""));
        attributes.put("FirstName", new AttributeValue(firstName));
        attributes.put("LastName", new AttributeValue(lastName));
        attributes.put("HasPet", new AttributeValue(""));
        attributes.put("NumberOfOccupants", new AttributeValue(""));
        attributes.put("RoleID", new AttributeValue(roleID));
        PutItemRequest request = new PutItemRequest("FixxUsers", attributes);
        dynamo.putItemAsync(request);
    }

    private void uploadUserToDatabase (String firstName, String lastName, String roleID,
                                       String userID, String propertyID, boolean hasPet,
                                       String numberOfOccupants) {
        Map<String, AttributeValue> attributes = new HashMap<>(7);
        attributes.put("UserID", new AttributeValue(userID));
        attributes.put("PropertyID", new AttributeValue(propertyID));
        attributes.put("FirstName", new AttributeValue(firstName));
        attributes.put("LastName", new AttributeValue(lastName));
        attributes.put("HasPet", new AttributeValue(String.valueOf(hasPet)));
        attributes.put("NumberOfOccupants", new AttributeValue(numberOfOccupants));
        attributes.put("RoleID", new AttributeValue(roleID));
        PutItemRequest request = new PutItemRequest("FixxUsers", attributes);
        dynamo.putItemAsync(request);
    }

    public void login (View v) {
        userInfo.put("FirstName", firstNameInput.getText().toString());
        userInfo.put("LastName", lastNameInput.getText().toString());
        userInfo.put("RoleID", roleSelect.getSelectedItem().toString());
        if (roleSelect.getSelectedItem().toString().equals("Tenant")) {
            userInfo.put("RequestNumber", "0");
            userInfo.put("PropertyID", accessCodeInput.getText().toString());
            userInfo.put("NumberOfOccupants", numberOfOccupants.getText().toString());
            userInfo.put("HasPet", String.valueOf(hasPetCheckBox.isChecked()));
            uploadUserToDatabase(firstNameInput.getText().toString(),
                    lastNameInput.getText().toString(), roleSelect.getSelectedItem().toString(),
                    identityID, accessCodeInput.getText().toString(), hasPetCheckBox.isChecked(),
                    numberOfOccupants.getText().toString());
        } else if (roleSelect.getSelectedItem().toString().equals("Technician")) {
            uploadUserToDatabase(firstNameInput.getText().toString(),
                    lastNameInput.getText().toString(), roleSelect.getSelectedItem().toString(),
                    identityID);
        }
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
        returnToMainMenu();
        finish();
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
