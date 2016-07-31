package com.fixx.fixx.fixx;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraCaptureActivity extends Activity {

    Camera camera;
    SurfaceView cameraPreviewSurface;
    TextView countdownView;
    List<byte[]> imageData = new ArrayList<byte[]>();
    List<String> videoLocations = new ArrayList<String>();
    MediaRecorder recorder;

    ImageButton imageCaptureButton;
    int pictureCountdown = 3;
    int videoCountdown = 30;

    TimerTask videoCountdownTimer = new TimerTask() {
        @Override
        public void run() {
            videoCountdown--;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    countdownView.setText(String.valueOf(videoCountdown));
                }
            });
            if (videoCountdown <= 0) {
                startSubmissionActivity("video");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set window properties for camera preview
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_camera_capture);

        // Connect to the camera preview surface
        cameraPreviewSurface = (SurfaceView)findViewById(R.id.cameraPreview);
        // Connect to the capture buttons
        imageCaptureButton = (ImageButton)findViewById(R.id.imageCaptureButton);
        // Connect to the countdown indicator
        countdownView = (TextView)findViewById(R.id.countdownText);

        // Add listener to take picture when the user taps the capture button
        imageCaptureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureImage();
            }
        });

        // Add listener to begin recording when the user holds the capture button
        imageCaptureButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                System.out.println("Long clicked");
                startRecordingVideo();
                // Add listener to stop recording when the user lifts his/her finger
                imageCaptureButton.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            stopRecordingVideo(recorder);
                        }
                        return false;
                    }
                });
                return false;
            }
        });

        // Store the SurfaceHolder associated with the camera preview surface
        SurfaceHolder holder = cameraPreviewSurface.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // Open the device camera
                camera = Camera.open();
                // Set preview surface for the camera
                try {
                    camera.setPreviewDisplay(holder);
                } catch (IOException exception) {
                    camera.release();
                }
                camera.setDisplayOrientation(90);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // Start outputting the camera feed to the preview surface
                camera.startPreview();
                camera.setDisplayOrientation(90);
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // Stop previewing the camera feed
                camera.stopPreview();
                // Release the camera hardware
                camera.release();
            }
        });
    }

    private void captureImage () {
        System.out.println("Capturing Image");
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // Store the path of the Fixx folder on the device
                String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Fixx/";
                File dir = new File(path);
                if(!dir.exists()) {
                    dir.mkdirs();
                }
                String filePath = path + "Image" + String.valueOf(pictureCountdown) + ".jpg";
                // Write JPEG image to system memory
                File imageFile = new File(filePath);
                FileOutputStream fileOut;
                try {
                    fileOut = new FileOutputStream(imageFile);
                    fileOut.write(data);
                    fileOut.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Decrement the countdown and update the UI
                pictureCountdown--;
                countdownView.setText(String.valueOf(pictureCountdown));

                if (pictureCountdown <= 0) {
                    startSubmissionActivity("picture");
                }
                camera.stopPreview();
                camera.startPreview();
            }
        });
    }

    private void startRecordingVideo () {
        System.out.println("Capturing video");

        recorder = new MediaRecorder();
        camera.unlock();
        recorder.setCamera(camera);
        recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Fixx/";
        File dir = new File(path);
        if(!dir.exists()) {
            dir.mkdirs();
        }
        String filePath = path + "Video" + ".3gp";

        recorder.setOutputFile(filePath);
        videoLocations.add(filePath);

        try {
            recorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        recorder.start();

        Timer videoTimer = new Timer();
        videoTimer.schedule(videoCountdownTimer, 0, 1000);
        imageCaptureButton.setColorFilter(Color.RED);
    }

    private void stopRecordingVideo (MediaRecorder recorder) {
        videoCountdownTimer.cancel();
        imageCaptureButton.setColorFilter(Color.TRANSPARENT);
        recorder.stop();
        startSubmissionActivity("video");
        finish();
    }

    private void startSubmissionActivity (String mode) {
        Intent categorySelectIntent = new Intent(this, categorySelectActivity.class);
        categorySelectIntent.putExtra("mode", mode);
        startActivity(categorySelectIntent);
        finish();
    }

    private int getScreenOrientation () {
        return this.getWindowManager().getDefaultDisplay().getRotation();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (camera != null) {
            try {
                camera.reconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
