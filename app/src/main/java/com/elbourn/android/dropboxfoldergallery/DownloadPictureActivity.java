package com.elbourn.android.dropboxfoldergallery;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;

public class DownloadPictureActivity extends AppCompatActivity {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "DownloadPictureActivity";
    DbxClientV2 client = null;
    ActivityResultLauncher<Intent> saveActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        completeSaveImage(data);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getApplicationContext();
        DbxCredential accessToken = AuthActivity.getLocalCredential(context);
        client = AuthActivity.getDropboxClient(context, accessToken);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaStoreDownload();
        } else {
            directDownload();
        }
    }

    MediaScannerConnection.OnScanCompletedListener startGallery() {
        Log.i(TAG, "start startGallery");
        // Open app to view the download
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setType("image/*");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        Log.i(TAG, "end startGallery");
        finish();
        return null;
    }

    void mediaStoreDownload() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void run() {
                try {
                    Log.i(TAG, "start mediaStoreDownload");
                    String onlinePath = getIntent().getStringExtra("onlinePath");
                    Metadata pathMetadata = client.files().getMetadata(onlinePath);
                    String chosenFileName = pathMetadata.getName().toLowerCase();
                    String chosenPath = pathMetadata.getPathLower();

                    // Configure media store config
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, chosenFileName);
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/*");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name));
                    contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                    contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                    Log.i(TAG, "contentValues: " + contentValues);

                    // Determine download location
                    Context context = getApplicationContext();
                    ContentResolver resolver = context.getContentResolver();
                    Uri imageUri = resolver.insert(getContentUri(context), contentValues);
                    Log.i(TAG, "imageUri: " + imageUri);

                    // Download the full sized image into location
                    if (imageUri != null) {
                        String msg = onlinePath + " downloading.";
                        Log.i(TAG, "msg: " + msg);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            }
                        });
                        OutputStream outputStream = resolver.openOutputStream(imageUri);
                        client.files().download(chosenPath).download(outputStream);
                        outputStream.close();
                        MediaScannerConnection.scanFile(context, new String[]{imageUri.getPath()}, null, startGallery());
                    } else {
                        String msg = "Failed to download image - try again later.";
                        Log.i(TAG, "msg: " + msg);
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "end mediaStoreDownload");
            }
        }).start();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getContentUri(Context context) {
        Log.i(TAG, "start getContentUri");
        Set<String> externalVolumeNames = MediaStore.getExternalVolumeNames(context);

        // Pick first one which is not external
        Uri uri = null;
        try {
            String[] vol = externalVolumeNames.toArray(new String[0]);
            for (int i = 0; i < vol.length; i++) {
                String volume = vol[i];
                if (!volume.contains(MediaStore.VOLUME_EXTERNAL)) {
                    uri = MediaStore.Images.Media.getContentUri(vol[i]);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // fallback to internal storage
        if (uri == null) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }
        Log.i(TAG, "externalVolumeNames: " + externalVolumeNames);
        Log.i(TAG, "uri: " + uri);
        Log.i(TAG, "end getContentUri");
        return uri;
    }

    void directDownload() {
        Log.i(TAG, "start directDownload");
        Context context = getApplicationContext();
        String[] permissions = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};
        if (Permissions.hasPermissions(context, permissions)) {
            startSaveImage();
        } else {
            Permissions.requestPermissions(this, permissions, 1);
        }
        Log.i(TAG, "end directDownload");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "start onRequestPermissionsResult");
        Context context = getApplicationContext();
        if (requestCode == 1 && Permissions.hasPermissions(context, permissions)) {
            startSaveImage();
        } else {
            String msg = "Permissions not granted. Unable to download pictures.";
            Log.i(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "end onRequestPermissionsResult");
    }

    public void startSaveImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.i(TAG, "start startSaveImage");
                    String onlinePath = getIntent().getStringExtra("onlinePath");
                    Metadata pathMetadata = client.files().getMetadata(onlinePath);
                    String chosenFileName = pathMetadata.getName().toLowerCase();

                    // Setup picker for user to choose location to save file
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    intent.putExtra(Intent.EXTRA_TITLE, chosenFileName);
                    saveActivityResultLauncher.launch(intent);
                } catch (DbxException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "end startSaveImage");
            }
        }).start();
    }

    @SuppressLint("WrongConstant")
    protected void completeSaveImage(Intent data) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "start completeSaveImage");
                try {
                    Uri uri = data.getData();
                    int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                    String onlinePath = getIntent().getStringExtra("onlinePath");
                    Metadata pathMetadata = client.files().getMetadata(onlinePath);
                    String chosenFileName = pathMetadata.getName().toLowerCase();
                    String chosenPath = pathMetadata.getPathLower();
                    Context context = getApplicationContext();
                    String msg = chosenFileName + " downloading.";
                    Log.i(TAG, "msg: " + msg);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                    FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                    client.files().download(chosenPath).download(fileOutputStream);
                    fileOutputStream.close();
                    MediaScannerConnection.scanFile(context, new String[]{pfd.getFileDescriptor().toString()}, null, startGallery());
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "end completeSaveImage");
            }
        }).start();
    }
}