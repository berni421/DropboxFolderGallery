package com.elbourn.android.dropboxfoldergallery;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.android.Auth;
import com.dropbox.core.json.JsonReadException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;

import java.util.Arrays;
import java.util.Collection;

import androidx.appcompat.app.AppCompatActivity;

public class AuthActivity extends AppCompatActivity {

    static String APP = BuildConfig.APPLICATION_ID;
    static String TAG = "AuthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        Context context = getApplicationContext();
        DbxCredential accessToken = getLocalCredential(context);
        if (accessToken == null) {
            // New login
            Log.i(TAG, "new login");
            String applicationKey = getString(R.string.dbx_api_app_key);
            String clientIdentifier = getString(R.string.client_identifier);
            Collection<String> scopes = Arrays.asList("account_info.read", "files.content.read");
            DbxRequestConfig requestConfig = new DbxRequestConfig(clientIdentifier);
            Auth.startOAuth2PKCE(context, applicationKey, requestConfig, scopes);
        }
        if (accessToken != null) {
            if (accessToken.aboutToExpire()) {
                // Try to refresh the token
                Log.i(TAG, "old accessToken: " + accessToken);
                accessToken = new DbxCredential(accessToken.getAccessToken(), -1L, accessToken.getRefreshToken(), accessToken.getAppKey());
                if (accessToken != null) {
                    Log.i(TAG, "new accessToken: " + accessToken);
                    storeCredentialLocally(context, accessToken);
                }
            }
        }
        Log.i(TAG, "end onCreate");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "start onResume");
        Context context = getApplicationContext();
        DbxCredential accessToken = getLocalCredential(context);
        if (accessToken == null) {
            accessToken = Auth.getDbxCredential();
            Log.i(TAG, "new login accessToken: " + accessToken);
        }
        if (accessToken != null) {
            Log.i(TAG, "valid accessToken: " + accessToken);
            storeCredentialLocally(context, accessToken);
            Intent gFA = new Intent(context, GetFolderActivity.class);
            startActivity(gFA);
        }
        Log.i(TAG, "end onResume");
    }

    //get credential from SharedPreferences if it exists
    public static DbxCredential getLocalCredential(Context context) {
        Log.i(TAG, "start getLocalCredential");
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
        String serializedCredentialText = sharedPreferences.getString("credential", null);
        DbxCredential serializedCredential = null;
        if (serializedCredentialText != null) {
            try {
                serializedCredential = DbxCredential.Reader.readFully(serializedCredentialText);
            } catch (JsonReadException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "serializedCredential: " + serializedCredential);
        Log.i(TAG, "end getLocalCredential");
        return serializedCredential;
    }

    //store store credential in SharedPreferences
    private void storeCredentialLocally(Context context, DbxCredential dbxCredential) {
        Log.i(TAG, "start storeCredentialLocally");
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
        sharedPreferences.edit().putString("credential", dbxCredential.toString()).apply();
        Log.i(TAG, "end storeCredentialLocally");
    }

    //clear SharedPreferences
    public static void disconnectDropbox(Context context) {
        Log.i(TAG, "start disconnectDropbox");
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP, MODE_PRIVATE);
        sharedPreferences.edit().remove("credential").apply();
        Log.i(TAG, "end disconnectDropbox");
    }

    public static DbxClientV2 getDropboxClient(Context context, DbxCredential accessToken) {
        DbxClientV2 client = null;
        try {
            String clientIdentifier = context.getString(R.string.client_identifier);
            DbxRequestConfig requestConfig = new DbxRequestConfig(clientIdentifier);
            client = new DbxClientV2(requestConfig, accessToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return client;
    }
}