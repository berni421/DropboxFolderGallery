package com.elbourn.android.dropboxfoldergallery;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static androidx.core.content.FileProvider.getUriForFile;

public class GetPictureActivity extends AppCompatActivity implements SelectPictureViewAdapter.ItemClickListener, SelectPictureViewAdapter.ItemLongClickListener {
    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "GetPictureActivity";
    SelectPictureViewAdapter adapter = null;
    ArrayList<GraphicData> adapterImages = null;
    DbxClientV2 client = null;
    DropboxData dropboxData = null;
    Boolean stopNetworkingThread = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        setContentView(R.layout.activity_show_pictures);
        CheckPermissions();
        Log.i(TAG, "end onCreate");
    }

    void CheckPermissions() {
        String[] permissions = new String[]{"android.permission.INTERNET"};
        if (Build.VERSION.SDK_INT < 29) {
            permissions = new String[]{
                    "android.permission.WRITE_EXTERNAL_STORAGE",
                    "android.permission.READ_EXTERNAL_STORAGE",
                    "android.permission.INTERNET"
            };
        }
        Context context = getApplicationContext();
        if (Permissions.hasPermissions(context, permissions)) {
            processGraphicData();
        } else {
            Permissions.requestPermissions(this, permissions, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "start onRequestPermissionsResult");
        Context context = getApplicationContext();
        if (requestCode == 1 && Permissions.hasPermissions(context, permissions)) {
            processGraphicData();
        } else {
            String msg = "permissions not granted - pictures will not show";
            Log.i(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "start onRequestPermissionsResult");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "start onResume");
        if (adapter != null) {
            setupRView(adapterImages);
        }
        Log.i(TAG, "end onResume");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.i(TAG, "start onBackPressed");
        stopNetworkingThread = true;
        Log.i(TAG, "stopNetworking: " + stopNetworkingThread);
        finish();
        Log.i(TAG, "end onBackPressed");
    }

    protected void processGraphicData() {
        Log.i(TAG, "start processGraphicData");
        Context context = getApplicationContext();
        DbxCredential accessToken = AuthActivity.getLocalCredential(context);
        Log.i(TAG, "accessToken: " + accessToken);
        if (accessToken != null) {
            NetworkUITasksThread(context, accessToken);
        }
        Log.i(TAG, "end processGraphicData");
    }

    public void NetworkUITasksThread(Context context, DbxCredential accessToken) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    client = AuthActivity.getDropboxClient(context, accessToken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "client: " + client);
                if (client != null) {
                    getGraphicData(context, client);
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (client == null) {
                                String msg = "No images found";
                                Log.i(TAG, "msg: " + msg);
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public void getGraphicData(Context context, DbxClientV2 client) {
        Log.i(TAG, "start getGraphicData");
        String folder = getIntent().getStringExtra("folder");
        if (folder == null) {
            folder = "";
        }
        Log.i(TAG, "folder: " + folder);
        // Get list of folders and files
        ListFolderBuilder folders = null;
        try {
            folders = client.files().listFolderBuilder(folder).withLimit((long) 12).withRecursive(true).withIncludeMediaInfo(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Metadata> entries = null;
        Boolean hasMore = null;
        String cursor = null;
        try {
            entries = folders.start().getEntries();
            cursor = folders.start().getCursor();
            hasMore = folders.start().getHasMore();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        ArrayList<GraphicData> graphicDataList = null;
        Log.i(TAG, "entries: " + entries);
        if (entries != null) {
            graphicDataList = getActualGraphicData(context, entries);
        }
        Boolean setupRVNeeded = true;
        if (graphicDataList.size() != 0) {
            setupRView(graphicDataList);
            setupRVNeeded = false;
        }
        dropboxData = new DropboxData(client, cursor, hasMore);
        ArrayList<GraphicData> moreGraphicDataList = null;
        while (dropboxData.hasMore) {
            moreGraphicDataList = getMoreGraphicData();
            if (moreGraphicDataList != null) {
                if (moreGraphicDataList.size() != 0) {
                    if (setupRVNeeded) {
                        setupRView(moreGraphicDataList);
                        setupRVNeeded = false;
                    } else {
                        updateRView(moreGraphicDataList);
                    }
                }
            }
        }
        if (adapterImages == null && setupRVNeeded) {
            Log.i(TAG, "setting empty image");
            Bitmap stopImage = convertToBitmap(AppCompatResources.getDrawable(context, R.drawable.stop_foreground), 64, 64);
            GraphicData graphicData = new GraphicData(getString(R.string.stop), "", "", stopImage, 0L);
            graphicDataList.add(graphicData);
            setupRView(graphicDataList);
        }
        if (adapterImages != null) {
            int totalImages = adapterImages.size();
            runOnUiThread(new Runnable() {
                public void run() {
                    String msg = totalImages + " images found.";
                    Log.i(TAG, "msg: " + msg);
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                }
            });
        }
        Log.i(TAG, "end getGraphicData");
    }

    ArrayList<GraphicData> getMoreGraphicData() {
        Log.i(TAG, "start getMoreGraphicData");
        Context context = getApplicationContext();
        ArrayList<GraphicData> moreGraphicData = null;
        if (stopNetworkingThread) {
            dropboxData.hasMore = false;
            Log.i(TAG, "stopNetworking: " + stopNetworkingThread);
        } else {
            try {
                ListFolderResult more = dropboxData.client.files().listFolderContinue(dropboxData.cursor);
                List<Metadata> moreEntries = more.getEntries();
                dropboxData.cursor = more.getCursor();
                dropboxData.hasMore = more.getHasMore();
                moreGraphicData = getActualGraphicData(context, moreEntries);
            } catch (ListFolderContinueErrorException e) {
                e.printStackTrace();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        }
        Log.i(TAG, "end getMoreGraphicData");
        return moreGraphicData;
    }

    Boolean isImage(String fileName) {
        Log.i(TAG, "start isImage");
        String mType = mimeType(fileName);
        Boolean isImage = (mType != null);
        Log.i(TAG, "isImage: " + isImage);
        Log.i(TAG, "end isImage");
        return isImage;
    }

    String mimeType(String fileName) {
        Log.i(TAG, "start mimeType");
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        String mType = null;
        Log.i(TAG, "extension: " + extension);
        switch (extension) {
            case "jpg":
            case "jpeg":
                mType = "image/jpeg";
                break;
            case "png":
            case "tiff":
            case "psd":
            case "eps":
                mType = "image/extension";
                break;
        }
        Log.i(TAG, "mType: " + mType);
        Log.i(TAG, "end mimeType");
        return mType;
    }

    public ArrayList<GraphicData> getActualGraphicData(Context context, List<Metadata> entries) {
        Log.i(TAG, "start getActualGraphicData");
        ArrayList<GraphicData> graphicDataList = new ArrayList<>();
        Log.i(TAG, "entries: " + entries);
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Downloading " + entries.size() + " more files. please wait.";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        for (Metadata fileMetadata : entries) {
            if (fileMetadata instanceof FileMetadata) {
                // get thumbnail
                try {
                    File path = context.getCacheDir();
                    String onLinePath = fileMetadata.getPathLower();
                    String onLineFolder = onLinePath.substring(0, onLinePath.lastIndexOf(File.separator));
                    if (isImage(onLinePath)) {
                        // download image thumbnail
                        String thumbnailName = File.separator + "thumbnail";
                        File file = new File(context.getCacheDir(), thumbnailName);
                        OutputStream outputStream = new FileOutputStream(file);
                        client.files().getThumbnail(fileMetadata.getPathLower()).download(outputStream);
                        outputStream.close();
                        // build graphic data arraylist
                        Bitmap bitmap = BitmapFactory.decodeFile(path + thumbnailName);
                        String fileName = fileMetadata.getName().toLowerCase();
                        Long clientModified = ((FileMetadata) fileMetadata).getClientModified().getTime(); // photo taken time not supported by Dropbox metadata
                        GraphicData graphicData = new GraphicData(onLinePath, onLineFolder, fileName, bitmap, clientModified);
                        graphicDataList.add(graphicData);
                        Log.i(TAG, "added onlinePath: " + onLinePath);
                    } else {
                        Log.i(TAG, "not added onlinePath: " + onLinePath);
                    }
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "end getActualGraphicData");
        return graphicDataList;
    }

    // set up the RecyclerView
    public void setupRView(ArrayList<GraphicData> myImages) {
        Log.i(TAG, "start setupRView");
        Context context = GetPictureActivity.this;
        adapterImages = myImages;
        Collections.sort(adapterImages, (lhs, rhs) -> lhs.onlineFolder.compareTo(rhs.onlineFolder));
        adapter = new SelectPictureViewAdapter(context, adapterImages, "folderNames");
        adapter.setClickListener(this);
        adapter.setLongClickListener(this);
        RecyclerView recyclerView = findViewById(R.id.myImages);
        runOnUiThread(new Runnable() {
            public void run() {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(adapter);
            }
        });
        Button fileTimes = findViewById(R.id.fileTimes);
        fileTimes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "fileTimes clicked");
                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.sortOrder = "fileTimes";
                        adapterSort();
                        adapter.notifyDataSetChanged();
//                        adapter.notifyItemRangeChanged(0, adapterImages.size());
                    }
                });
            }
        });
        Button fileNames = findViewById(R.id.fileNames);
        fileNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "fileNames clicked");

                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.sortOrder = "fileNames";
                        adapterSort();
                        adapter.notifyDataSetChanged();
//                        adapter.notifyItemRangeChanged(0, adapterImages.size());
                    }
                });
            }
        });
        Button folderNames = findViewById(R.id.folderNames);
        folderNames.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "folderNames clicked");

                runOnUiThread(new Runnable() {
                    public void run() {
                        adapter.sortOrder = "folderNames";
                        adapterSort();
                        adapter.notifyDataSetChanged();
//                        adapter.notifyItemRangeChanged(0, adapterImages.size());
                    }
                });
            }
        });
        dumpAdapterToLog();
        Log.i(TAG, "end setupRView");
    }

    void adapterSort() {
        if (adapterImages != null) {
            if (adapter.sortOrder == "fileTimes") {
                Collections.sort(adapterImages, (lhs, rhs) -> lhs.sortDate.compareTo(rhs.sortDate));
            } else if (adapter.sortOrder == "fileNames") {
                Collections.sort(adapterImages, (lhs, rhs) -> lhs.fileName.compareTo(rhs.fileName));
            } else {
                Collections.sort(adapterImages, (lhs, rhs) -> lhs.onlineFolder.compareTo(rhs.onlineFolder));
            }
        }
    }

    // Update RecyclerView
    public void updateRView(ArrayList<GraphicData> newImages) {
        Log.i(TAG, "start updateRView");
        Log.i(TAG, "newImages.size(): " + newImages.size());
        Log.i(TAG, "adapterImages.size(): " + adapterImages.size());
        if (null != newImages) {
            adapterImages.addAll(newImages);
        }
        adapterSort();
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyItemRangeChanged(0, adapterImages.size());
            }
        });
        Log.i(TAG, "adapterImages.size(): " + adapterImages.size());
        dumpAdapterToLog();
        Log.i(TAG, "end updateRView");
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.i(TAG, "start onItemClick");
        Context context = getApplicationContext();
        Log.i(TAG, "position: " + position);
        int items = adapterImages.size();
        Log.i(TAG, "items: " + items);
        String onlinePath = getString(R.string.blank);
        if (position < items) {
            onlinePath = adapter.getItem(position).onlinePath;
        }
        Log.i(TAG, "position: " + onlinePath);
        String msg = onlinePath + " downloading.";
        Log.i(TAG, "msg: " + msg);
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        if (onlinePath.equals(getString(R.string.stop)) || onlinePath.equals(getString(R.string.blank))) {
            Log.i(TAG, "blank or stop chosen onItemClick");
        } else {
            final String oP = onlinePath;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Metadata pathMetadata = client.files().getMetadata(oP);
                        String fileName = pathMetadata.getName().toLowerCase();
                        String path = pathMetadata.getPathLower();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Define media store
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
//                            contentValues.put(MediaStore.MediaColumns.ALBUM, getString(R.string.app_name));
                            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/*");
//                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name));
//                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM + File.separator + getString(R.string.app_name));
                            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name));
                            contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
                            contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000);
                            Log.i(TAG, "contentValues: " + contentValues);

                            // Determine download location
                            ContentResolver resolver = context.getContentResolver();
                            //                            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                            Uri imageUri = resolver.insert(getContentUri(context), contentValues);
                            Log.i(TAG, "imageUri: " + imageUri);

                            // Download the full sized image into location
                            if (imageUri != null) {
                                OutputStream outputStream = resolver.openOutputStream(imageUri);
                                client.files().download(path).download(outputStream);
                                outputStream.close();
                                MediaScannerConnection.scanFile(context,
                                        new String[]{imageUri.getPath()},
                                        null, null);
                            } else {
                                String msg = "Failed to download image - try again later.";
                                Log.i(TAG, "msg: " + msg);
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        } else {
                            // Download the full sized image into location
                            String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + getString(R.string.app_name);
                            File directory = new File(imagesDir);
                            directory.mkdirs();
                            File image = new File(imagesDir, fileName);
                            OutputStream outputStream = new FileOutputStream(image);
                            client.files().download(path).download(outputStream);
                            outputStream.close();
                            image.setLastModified((Long) System.currentTimeMillis() / 1000);
                            Log.i(TAG, "image: " + image);
                            MediaScannerConnection.scanFile(context,
                                    new String[]{image.toString()},
                                    null, null);
                        }

                        // Open app to view the download
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setType("image/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Log.i(TAG, "starting view intent");
                        startActivity(intent);

                    } catch (IOException | DbxException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        Log.i(TAG, "end onItemClick");
    }

    @Override
    public void onLongItemClick(View view, int position) {
        Log.i(TAG, "start onLongItemClick");
        Context context = getApplicationContext();
        Log.i(TAG, "position: " + position);
        int items = adapterImages.size();
        Log.i(TAG, "items: " + items);
        String onlinePath = getString(R.string.blank);
        if (position < items) {
            onlinePath = adapter.getItem(position).onlinePath;
        }
        Log.i(TAG, "position: " + onlinePath);
        String msg = onlinePath;
        Log.i(TAG, "msg: " + msg);
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        Log.i(TAG, "end onLongItemClick");
    }

    private void galleryScanThis(Uri uri) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(uri);
        this.sendBroadcast(mediaScanIntent);
    }

    static public Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
        Bitmap mutableBitmap = Bitmap.createBitmap(widthPixels, heightPixels, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mutableBitmap);
        drawable.setBounds(0, 0, widthPixels, heightPixels);
        drawable.draw(canvas);
        return mutableBitmap;
    }

    private void dumpAdapterToLog() {
        for (int i = 0; i < adapterImages.size(); i++) {
            Log.i(TAG, "adapterImages[" + i + "].onlinePath: " + adapterImages.get(i).onlinePath);
            Log.i(TAG, "adapterImages[" + i + "].onlineFolder: " + adapterImages.get(i).onlineFolder);
            Log.i(TAG, "adapterImages[" + i + "].date: " + adapterImages.get(i).displayDate);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private Uri getContentUri(Context context) {
        Log.i(TAG, "start getContentUri");
        Set<String> externalVolumeNames = MediaStore.getExternalVolumeNames(context);

        // Pick first one which is not external
        Uri uri = null;
        try {
            String[] vol = externalVolumeNames.toArray(new String[0]);
            for (int i=0; i<vol.length; i++) {
                String volume = vol[i];
                if (!volume.contains(MediaStore.VOLUME_EXTERNAL)) {
                    uri = MediaStore.Images.Media.getContentUri(vol[i]);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (uri == null) {
            uri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        }

        Log.i(TAG, "externalVolumeNames: " + externalVolumeNames);
        Log.i(TAG, "uri: " + uri);
        Log.i(TAG, "end getContentUri");
        return uri;
    }
}