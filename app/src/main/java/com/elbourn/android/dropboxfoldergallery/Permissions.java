package com.elbourn.android.dropboxfoldergallery;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

class Permissions {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "Permissions";

    public static boolean hasPermissions(Context context, String[] permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        Log.i(TAG, "start requestPermissions");
        try {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "end requestPermissions");
    }
}

