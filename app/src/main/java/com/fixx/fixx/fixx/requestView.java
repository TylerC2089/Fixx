package com.fixx.fixx.fixx;

import android.content.Intent;
import android.net.Uri;
import android.opengl.Visibility;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class requestView extends ActionBarActivity {

    //UI Variables
    ImageView image1;
    ImageView image2;
    ImageView image3;
    VideoView video;
    TextView addressLine1;
    TextView addressLine2;
    TextView tenantName;
    TextView hasPet;
    TextView numberOfOccupants;
    TextView jobDetails;
    TextView dates;

    String mediaID = "";
    String requestID = "";

    String url = "";
    String mediaType = "";

    // Variables for Dynamo DB
    private AmazonDynamoDBAsyncClient dynamo = MainMenuActivity.dynamo;
    Dataset userInfo = MainMenuActivity.userInfo;

    //Variables for S3
    private AmazonS3Client s3 = MainMenuActivity.s3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_view);

        mediaID = getIntent().getStringExtra("MediaID");
        requestID = getIntent().getStringExtra("RequestID");

        // Create global configuration and initialize ImageLoader with this config
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        // Connect to UI
        image1 = (ImageView)findViewById(R.id.image1);
        image2 = (ImageView)findViewById(R.id.image2);
        image3 = (ImageView)findViewById(R.id.image3);
        video = (VideoView)findViewById(R.id.videoView2);
        addressLine1 = (TextView)findViewById(R.id.addressLine1);
        addressLine2 = (TextView)findViewById(R.id.addressLine2);
        tenantName = (TextView)findViewById(R.id.tenantName);
        hasPet = (TextView)findViewById(R.id.hasPetIndication);
        numberOfOccupants = (TextView)findViewById(R.id.occupantCountIndication);
        jobDetails = (TextView)findViewById(R.id.jobDetails);
        dates = (TextView)findViewById(R.id.dates);

        // Set job request information
        addressLine1.setText(getIntent().getStringExtra("AddressLine1"));
        addressLine2.setText(getIntent().getStringExtra("AddressLine2"));
        tenantName.setText(getIntent().getStringExtra("TenantName"));
        hasPet.setText(getIntent().getStringExtra("HasPet"));
        numberOfOccupants.setText(getIntent().getStringExtra("NumberOfOccupants"));
        jobDetails.setText(getIntent().getStringExtra("Details"));
        dates.setText(getIntent().getStringExtra("RepairDate"));

        String[] mediaIDArray = mediaID.split(",");
        for (int i = 0; i < mediaIDArray.length; i++) {
            Map<String, AttributeValue> key = new HashMap<>(1);
            key.put("MediaID", new AttributeValue(mediaIDArray[i]));
            GetItemRequest req = new GetItemRequest("FixxMedia", key);
            Future activeRequest = dynamo.getItemAsync(req, new AsyncHandler<GetItemRequest, GetItemResult>() {
                @Override
                public void onError(Exception e) {
                    System.out.println("Could not get media: " + e.getMessage());
                }

                @Override
                public void onSuccess(GetItemRequest request, GetItemResult getItemResult) {
                    Map<String, AttributeValue> mediaItem = getItemResult.getItem();
                    mediaType = mediaItem.get("Type").getS();
                    if (mediaType.equals("picture")) {
                        System.out.println("Got picture!");
                        url = mediaItem.get("LocationURL").getS();
                    } else if (mediaType.equals("video")) {
                        System.out.println("Got video!");
                        url = mediaItem.get("LocationURL").getS();
                    }
                }
            });

            while (!activeRequest.isDone()) { }

            if (mediaType.equals("picture")) {
                System.out.println(url);
                ImageLoader imageLoader = ImageLoader.getInstance();
                if (image1.getVisibility() == View.INVISIBLE) {
                    imageLoader.displayImage(url, image1);
                    image1.setVisibility(View.VISIBLE);
                } else if (image2.getVisibility() == View.INVISIBLE) {
                    imageLoader.displayImage(url, image2);
                    image2.setVisibility(View.VISIBLE);
                } else if (image3.getVisibility() == View.INVISIBLE) {
                    imageLoader.displayImage(url, image3);
                    image3.setVisibility(View.VISIBLE);
                }
            } else if (mediaType.equals("video")) {
                //Use a media controller so that you can scroll the video contents
                //and also to pause, start the video.
                MediaController mediaController = new MediaController(this);
                video.setVideoURI(Uri.parse(url));
                mediaController.setAnchorView(video);
                mediaController.setMediaPlayer(video);
                video.setMediaController(mediaController);
                video.start();
            }
        }
    }

    public void acceptJob (View v) {
        String requestRegistry = userInfo.get("RequestRegistry");
        if (requestRegistry != null) {
            userInfo.put("RequestRegistry", requestRegistry + "," + requestID);
        } else {
            userInfo.put("RequestRegistry", requestID);
        }
        userInfo.put(requestID + ":AddressLine1", addressLine1.getText().toString());
        userInfo.put(requestID + ":AddressLine2", addressLine2.getText().toString());
        userInfo.put(requestID + ":TenantName", tenantName.getText().toString());
        userInfo.put(requestID + ":HasPet", hasPet.getText().toString());
        userInfo.put(requestID + ":NumberOfOccupants", numberOfOccupants.getText().toString());
        userInfo.put(requestID + ":JobDetails", jobDetails.getText().toString());
        userInfo.put(requestID + ":RepairDate", dates.getText().toString());
        userInfo.put(requestID + ":TimeRange", "");
        userInfo.put(requestID + ":MediaID", mediaID);
        Intent scheduleRequestIntent = new Intent(this, technicianCalendar.class);
        scheduleRequestIntent.putExtra("RequestID", requestID);
        scheduleRequestIntent.putExtra("Mode", "scheduleRequest");
        startActivity(scheduleRequestIntent);
    }

    public void declineJob (View v) {
        finish();
    }
}
