package com.elbourn.andriod.dropboxfoldergallery;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import androidx.appcompat.app.AppCompatActivity;

public class AdminActivity extends AppCompatActivity {
    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "AdminActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        setContentView(R.layout.activity_admin);
        Context context = getApplicationContext();

        Button clearCacheFiles = findViewById(R.id.clearCacheFiles);
        clearCacheFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "clearCacheFiles clicked");
                // clean cache
                File dir = new File(getCacheDir(), "");
                if (dir.exists()) {
                    for (File f : dir.listFiles()) {
                        Log.i(TAG, "Delete f:" + f);
                        f.delete();
                    }
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        String msg = "Cache files deleted";
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    }
                });

            }
        });

        Button disconnectDropbox = findViewById(R.id.disconnectDropbox);
        disconnectDropbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "logout Dropbox clicked");
                // clear shared preferences to disconnect dropbox link
                Context context = getApplicationContext();
                runOnUiThread(new Runnable() {
                    public void run() {
                        String msg = "Disconnecting from dropbox - start Dropbox Folder Gallery to reconnect";
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    }
                });
                AuthActivity.disconnectDropbox(context);
                finishAffinity();
            }
        });
    }
}
