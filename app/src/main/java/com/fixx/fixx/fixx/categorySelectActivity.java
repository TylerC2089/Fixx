package com.fixx.fixx.fixx;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class categorySelectActivity extends ActionBarActivity {

    String mode = "picture";
    String category = "Property";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_select);

        Intent thisIntent = this.getIntent();
        mode = thisIntent.getStringExtra("mode");

        setTitle("Select Category");
    }

    public void setCategory (View v) {
        Button btn = (Button)v;
        category = btn.getText().toString();

        Intent descriptionInputIntent = new Intent(this, descriptionInputActivity.class);
        descriptionInputIntent.putExtra("mode", mode);
        descriptionInputIntent.putExtra("category", category);
        startActivity(descriptionInputIntent);
        finish();
    }
}
