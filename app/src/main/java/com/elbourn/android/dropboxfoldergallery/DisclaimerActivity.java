package com.elbourn.android.dropboxfoldergallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import androidx.appcompat.app.AppCompatActivity;

public class DisclaimerActivity extends AppCompatActivity {
    static String APP = BuildConfig.APPLICATION_ID;
    static String TAG = "IntroActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        Context context = getApplicationContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
        Boolean disclaimerCheckBox = sharedPreferences.getBoolean("disclaimerCheckBox", false);
        Log.i(TAG, "disclaimerCheckBox: " + disclaimerCheckBox);
        if (disclaimerCheckBox) {;
            startIntroActivity();
        } else {
            setContentView(R.layout.activity_disclaimer);
            setupButtons();
        }
        Log.i(TAG, "end onCreate");
    }

    void startIntroActivity() {
        Context context = getApplicationContext();
        Intent iA = new Intent(context, IntroActivity.class);
        startActivity(iA);
    }

    void setupButtons() {
        CheckBox disclaimerAgreedBox = findViewById(R.id.disclaimerCheckBox);
        disclaimerAgreedBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "disclaimerAgreedBox clicked");
                CheckBox checkBox = (CheckBox)v;
                Context context = getApplicationContext();
                SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
                if(checkBox.isChecked()){
                    Log.i(TAG, "disclaimerAgreedBox true");
                    sharedPreferences.edit().putBoolean("disclaimerCheckBox", true).apply();
                    startIntroActivity();
                }
                Log.i(TAG, "disclaimerAgreedBox: " + disclaimerAgreedBox);
            }
        });
    }
}