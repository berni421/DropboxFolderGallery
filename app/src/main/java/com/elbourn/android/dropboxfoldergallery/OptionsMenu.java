package com.elbourn.android.dropboxfoldergallery;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.File;
import java.util.function.IntToDoubleFunction;

public class OptionsMenu extends AppCompatActivity {

    private String TAG = "OptionsMenu";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        Context context = getApplicationContext();
        MenuItem introCheckBox = menu.findItem(R.id.menuIntroOff);
        introCheckBox.setChecked(IntroActivity.getIntroCheckBox(context));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menuIntroOff:
                setIntroductionOff(item);
                return true;
            case R.id.menuDonate:
                startDonationWebsite();
                return true;
            case R.id.menuClearCacheFiles:
                startClearCacheFiles();
                return true;
            case R.id.menuDisconnectDropbox:
                startDisconnextDropbox();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    void setIntroductionOff(MenuItem item) {
        Context context = getApplicationContext();
        Boolean subscriptionsIntroOff = !item.isChecked();
        item.setChecked(subscriptionsIntroOff);
        IntroActivity.setIntroCheckBox(context, subscriptionsIntroOff);
        Log.i(TAG, "subscriptionsIntroOff: " + subscriptionsIntroOff);
    }

    void startDonationWebsite() {
        Log.i(TAG, "start startDonationWebsite");
        Context context = getApplicationContext();
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Starting browser to feed the cat ...";
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
        String url = "https://www.elbourn.com/feed-the-cat/";
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
        Log.i(TAG, "end startDonationWebsite");
    }


    void startDisconnextDropbox() {
        Log.i(TAG, "logout Dropbox clicked");
        // clear shared preferences to disconnect dropbox link
        Context context = getApplicationContext();
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Disconnecting from dropbox - restart to reconnect.";
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
        AuthActivity.disconnectDropbox(context);
        finishAffinity();
    }

    void startClearCacheFiles() {
        Log.i(TAG, "clearCacheFiles clicked");
        // clean cache
        File dir = new File(getCacheDir(), "");
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                Log.i(TAG, "Delete f:" + f);
                f.delete();
            }
        }
        Context context = getApplicationContext();
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Cache files deleted";
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
    }
}