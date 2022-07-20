package com.elbourn.andriod.dropboxfoldergallery;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
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
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GetPictureActivity extends AppCompatActivity implements SelectPictureViewAdapter.ItemClickListener, SelectPictureViewAdapter.ItemLongClickListener {
    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "GetPictureActivity";
    SelectPictureViewAdapter adapter = null;
    ArrayList<GraphicData> adapterImages = null;
    DbxClientV2 client;
//    String[] permissions = {
//            "android.permission.READ_EXTERNAL_STORAGE",
//            "android.permission.INTERNET",
//            "android.permission.WRITE_EXTERNAL_STORAGE"
//    };
String[] permissions = {
        "android.permission.INTERNET",};
    DropboxData dropboxData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        setContentView(R.layout.activity_show_pictures);
        Context context = getApplicationContext();
        if (Permissions.hasPermissions(context, permissions)) {
            processGraphicData();
        } else {
            Permissions.requestPermissions(this, permissions, 1);
        }
        Log.i(TAG, "end onCreate");
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
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
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
            folders = client.files().listFolderBuilder(folder).withLimit((long) 12).withRecursive(true);
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
        if (graphicDataList.size() == 0 && !hasMore) {
            Bitmap stopImage = convertToBitmap(getDrawable(R.drawable.stop_foreground), 64, 64);
            GraphicData graphicData = new GraphicData(null, getString(R.string.stop), null, null, stopImage);
            graphicDataList.add(graphicData);
        }
        Boolean setupRVviewNeeded = true;
        if (graphicDataList.size() != 0) {
            setupRView(graphicDataList);
            setupRVviewNeeded = false;
        }
        dropboxData = new DropboxData(client, cursor, hasMore);
        while (dropboxData.hasMore) {
            ArrayList<GraphicData> moreGraphicData = getMoreGraphicData();
            if (setupRVviewNeeded) {
                if (moreGraphicData.size() != 0) {
                    setupRView(moreGraphicData);
                    setupRVviewNeeded = false;
                }
            } else {
                if (moreGraphicData.size() != 0) {
                    updateRView(moreGraphicData);
                }
            }
        }
        int totalImages = adapterImages.size();
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = totalImages + " images found.";
                Log.i(TAG, "msg: " + msg);
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
        Log.i(TAG, "end getGraphicData");
    }

    ArrayList<GraphicData> getMoreGraphicData() {
        Log.i(TAG, "start getMoreGraphicData");
        Context context = getApplicationContext();
        ArrayList<GraphicData> moreGraphicData = null;
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
        Log.i(TAG, "end getMoreGraphicData");
        return moreGraphicData;
    }

    Boolean isImage(String fileName) {
        Log.i(TAG, "start isImage");
        Log.i(TAG, "fileName: " + fileName);

//        JPEG (or JPG) - Joint Photographic Experts Group
//        PNG - Portable Network Graphics
//        GIF - Graphics Interchange Format
//        TIFF - Tagged Image File
//        PSD - Photoshop Document
//        EPS - Encapsulated Postscript

        Boolean isImage = false;
        String extension = fileName.substring(fileName.lastIndexOf("."));
        Log.i(TAG, "extension: " + extension);
        switch (extension) {
            case ".jpg":
            case ".jpeg":
            case ".png":
            case ".tiff":
            case ".psd":
            case ".eps":
                isImage = true;
                break;
        }
        Log.i(TAG, "end isImage");
        return isImage;
    }

    public ArrayList<GraphicData> getActualGraphicData(Context context, List<Metadata> entries) {
        Log.i(TAG, "start getActualGraphicData");
        ArrayList<GraphicData> graphicDataList = new ArrayList<>();
        Log.i(TAG, "entries: " + entries);
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Downloading " + entries.size() + " more files. please wait.";
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
            }
        });
        for (Metadata fileMetadata : entries) {
            if (fileMetadata instanceof FileMetadata) {
                // get thumbnail
                try {
//                    File path = context.getExternalFilesDir("");
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
                        GraphicData graphicData = new GraphicData(path, onLinePath, onLineFolder, fileName, bitmap);
                        graphicDataList.add(graphicData);
                        Log.i(TAG, "added onlinePath path/fileName: " + onLinePath);
                    } else {
                        Log.i(TAG, "not added onlinePath path/fileName: " + onLinePath);
                    }
                } catch (DbxException | IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Log.i(TAG, "end getActualFolderData");
        return graphicDataList;
    }

    // set up the RecyclerView
    public void setupRView(ArrayList<GraphicData> myImages) {
        Collections.sort(myImages, (lhs, rhs) -> lhs.onlineFolder.compareTo(rhs.onlineFolder));
        Log.i(TAG, "start setupRView");
        Context context = getApplicationContext();
        Log.i(TAG, "myImages.size(): " + myImages.size());
        adapterImages = myImages;
        adapter = new SelectPictureViewAdapter(context, adapterImages);
        adapter.setClickListener(this);
        adapter.setLongClickListener(this);
        RecyclerView recyclerView = findViewById(R.id.myImages);
        runOnUiThread(new Runnable() {
            public void run() {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(adapter);
            }
        });
        dumpAdapterToLog();
        Log.i(TAG, "end setupRView");
    }

    // Update RecyclerView
    public void updateRView(ArrayList<GraphicData> newImages) {
        Log.i(TAG, "start updateRView");
        Log.i(TAG, "newImages.size(): " + newImages.size());
        Log.i(TAG, "adapterImages.size(): " + adapterImages.size());
        Collections.sort(newImages, (lhs, rhs) -> lhs.onlineFolder.compareTo(rhs.onlineFolder));
        int adapterImagesOldSize = adapterImages.size();
        int position = 0;
        while (position < adapterImages.size()) {
            String folder = adapterImages.get(position).onlineFolder;
            if (newImages.size() != 0) {
                String newFolder = newImages.get(0).onlineFolder;
                if (newFolder.compareTo(folder) <= 0) {
                    adapterImages.add(position, newImages.get(0));
                    newImages.remove(0);
                    Log.i(TAG, "inserted position: " + position);
                }
            }
            position++;
        }
        Log.i(TAG, "inserted updated adapterImages.size(): " + adapterImages.size());
        if (newImages.size() != 0) {
            int updatePosition = adapterImages.size();
            int updatedSize = newImages.size();
            adapterImages.addAll(newImages);
        }
        Log.i(TAG, "appended updated adapterImages.size(): " + adapterImages.size());
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyItemRangeChanged(0, adapterImagesOldSize);
//                adapter.notifyItemRangeInserted(0, adapterImages.size());
            }
        });
        Log.i(TAG, "newImages.size(): " + newImages.size());
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
        String msg = onlinePath + " chosen";
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
//                        File galleryPath = getImagesDirectory();
//                        File file = new File(galleryPath, fileName);
                        ContentResolver resolver = getContentResolver();
                        ContentValues contentValues = new ContentValues();
                        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/*");
                        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + getString(R.string.app_name));
                        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                        Log.i(TAG, "Uri: " + imageUri);
//                        OutputStream outputStream = new FileOutputStream(file);
                        OutputStream outputStream = resolver.openOutputStream(Objects.requireNonNull(imageUri));
                        client.files().download(path).download(outputStream);
                        outputStream.close();
                        // Open Gallery to view the download
                        galleryScanThis(imageUri);
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setType("image/*");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        Log.i(TAG, "intent onItemClick");
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

    private static File getImagesDirectory() {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + APP);
        if (!file.mkdirs() && !file.isDirectory()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }

    public Bitmap convertToBitmap(Drawable drawable, int widthPixels, int heightPixels) {
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
        }
    }
}