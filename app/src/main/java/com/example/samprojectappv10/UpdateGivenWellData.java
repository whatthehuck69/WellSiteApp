package com.example.samprojectappv10;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import org.w3c.dom.Text;

public class UpdateGivenWellData extends AppCompatActivity {
    private TextView wellIDText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_given_well_data);
        wellIDText = findViewById(R.id.wellIDText);
        Intent prevIntent = getIntent();
        int wellID = prevIntent.getIntExtra("Well ID",-1);
        wellIDText.setText("You are altering data of well #"+wellID);

    }
}
