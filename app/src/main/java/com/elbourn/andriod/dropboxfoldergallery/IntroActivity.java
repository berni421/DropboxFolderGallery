package com.elbourn.andriod.dropboxfoldergallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;

public class IntroActivity extends AppCompatActivity {

    static String APP = BuildConfig.APPLICATION_ID;
    static String TAG = "IntroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
        Boolean introCheckBox = sharedPreferences.getBoolean("introCheckBox", false);
        Log.i(TAG, "introCheckBox: " + introCheckBox);
        if (introCheckBox) {;
            startAuthActivity();
        } else {
            setContentView(R.layout.activity_intro);
            setupButtons();
        }
        Log.i(TAG, "end onCreate");
    }

    void startAuthActivity() {
        Context context = getApplicationContext();
        Intent aA = new Intent(context, AuthActivity.class);
        startActivity(aA);
    }

    void setupButtons() {
        ImageButton introImageButton = findViewById(R.id.introImageButton);
        introImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "introImageButton clicked");
                // now do auth activity
                startAuthActivity();
            }
        });
        CheckBox introCheckBox = findViewById(R.id.introCheckBox);
        introCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "introCheckBox clicked");
                CheckBox checkBox = (CheckBox)v;
                Context context = getApplicationContext();
                SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
                if(checkBox.isChecked()){
                    Log.i(TAG, "introCheckBox true");
                    sharedPreferences.edit().putBoolean("introCheckBox", true).apply();
                } else {
                    Log.i(TAG, "introCheckBox false");
                    sharedPreferences.edit().putBoolean("introCheckBox", false).apply();
                }
                Boolean introCheckBox = sharedPreferences.getBoolean("introCheckBox", false);
                Log.i(TAG, "introCheckBox: " + introCheckBox);
            }
        });
    }
}