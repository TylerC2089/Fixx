package com.fixx.fixx.fixx;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class descriptionInputActivity extends ActionBarActivity {

    EditText descriptionInput;

    String mode = "picture";
    String category = "Property";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description_input);

        setTitle("Enter Problem Description");

        // Get extras from prior activity
        Intent thisIntent = this.getIntent();
        mode = thisIntent.getStringExtra("mode");
        category = thisIntent.getStringExtra("category");

        // Connect to UI elements
        descriptionInput = (EditText)findViewById(R.id.description);
    }

    public void next (View v) {
        Intent datePickerIntent = new Intent(this, datePickerActivity.class);
        datePickerIntent.putExtra("description", descriptionInput.getText().toString());
        datePickerIntent.putExtra("mode", mode);
        datePickerIntent.putExtra("category", category);
        startActivity(datePickerIntent);
        finish();
    }
}
