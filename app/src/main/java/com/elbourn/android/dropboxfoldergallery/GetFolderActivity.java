package com.elbourn.android.dropboxfoldergallery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.dropbox.core.DbxException;
import com.dropbox.core.oauth.DbxCredential;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FolderMetadata;
import com.dropbox.core.v2.files.ListFolderBuilder;
import com.dropbox.core.v2.files.ListFolderContinueErrorException;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class GetFolderActivity extends OptionsMenu implements SelectFolderViewAdapter.ItemClickListener {

    private static String APP = BuildConfig.APPLICATION_ID;
    private static String TAG = "GetFolderActivity";
    SelectFolderViewAdapter adapter;
    ArrayList<String> adapterFolders = null;
    String[] permissions = {
            "android.permission.INTERNET",
    };
    DropboxData dropboxData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "start onCreate");
        setContentView(R.layout.activity_show_folders);
        Context context = getApplicationContext();
        if (Permissions.hasPermissions(context, permissions)) {
            processFolderData();
        } else {
            Permissions.requestPermissions(this, permissions, 2);
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
        if (requestCode == 2 && Permissions.hasPermissions(context, permissions)) {
            processFolderData();
        } else {
            String msg = "permissions not granted - folders will not show";
            Log.i(TAG, msg);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
        Log.i(TAG, "start onRequestPermissionsResult");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "start onResume");
        if (adapterFolders != null) {
            setupRView(adapterFolders);
        }
        Log.i(TAG, "end onResume");
    }

    @Override
    public void onBackPressed() {
        String folder = getIntent().getStringExtra("folder");
        if (folder == null) {
//        this.finishAffinity();
            finishAffinity();
        } else {
            finish();
        }
    }

    protected void processFolderData() {
        Context context = getApplicationContext();
        DbxCredential accessToken = AuthActivity.getLocalCredential(context);
        Log.i(TAG, "accessToken: " + accessToken);
        if (accessToken != null) {
            NetworkTasksThread(context, accessToken);
        }
    }

    public void NetworkTasksThread(Context context, DbxCredential accessToken) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                DbxClientV2 client = null;
                try {
                    client = AuthActivity.getDropboxClient(context, accessToken);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (client != null) {
                    getFolderData(context, client);
                } else {
                    final DbxClientV2 c = client;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            if (c == null) {
                                String msg = "No folders found - check network";
                                Log.i(TAG, "msg: " + msg);
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        }).start();
    }

    public void getFolderData(Context context, DbxClientV2 client) {
        String folder = getIntent().getStringExtra("folder");
        if (folder == null) {
            folder = "";
        }
        Log.i(TAG, "folder: " + folder);
        // Get list of folders and files
        ListFolderBuilder folders = null;
        try {
            folders = client.files().listFolderBuilder(folder);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Metadata> entries = null;
        ArrayList<String> folderData = null;
        String cursor = null;
        Boolean hasMore = null;
        try {
            entries = folders.start().getEntries();
            cursor = folders.start().getCursor();
            hasMore = folders.start().getHasMore();
        } catch (DbxException e) {
            e.printStackTrace();
        }
        if (entries != null) {
            folderData = getActualFolderData(context, entries);
            if (folder == "" ) {
                folderData.add(0, getString(R.string.everywhare));
            }
            if (folderData.size() == 0)
            {
                folderData.add(getString(R.string.nosubfoldershere));
            }
            setupRView(folderData);
        }
        dropboxData = new DropboxData(client, cursor, hasMore);
        ImageButton moreButton = findViewById(R.id.more);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "more clicked");
                getMoreFolderData();
            }
        });
        final Boolean hM = hasMore;
        runOnUiThread(new Runnable() {
            public void run() {
                if (null != hM) {
                    if (hM) {
                        moreButton.setVisibility(View.VISIBLE);
                    } else {
                        moreButton.setVisibility(View.GONE);
                    }
                } else {
                    moreButton.setVisibility(View.GONE);
                }
            }
        });
    }

    void getMoreFolderData() {
        Log.i(TAG, "start getMoreFolderData");
        Context context = getApplicationContext();
        ImageButton moreButton = findViewById(R.id.more);
        if (dropboxData.hasMore) {
            try {
                ListFolderResult more = dropboxData.client.files().listFolderContinue(dropboxData.cursor);
                List<Metadata> moreEntries = more.getEntries();
                dropboxData.cursor = more.getCursor();
                dropboxData.hasMore = more.getHasMore();
                ArrayList<String> folderData = getActualFolderData(context, moreEntries);
                updateRView(folderData);
            } catch (ListFolderContinueErrorException e) {
                e.printStackTrace();
            } catch (DbxException e) {
                e.printStackTrace();
            }
        } else {
            runOnUiThread(new Runnable() {
                public void run() {
                    moreButton.setVisibility(View.GONE);
                    String msg = "No more folders";
                    Log.i(TAG, "msg: " + msg);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
        Log.i(TAG, "end getMoreFolderData");
    }

    public ArrayList<String> getActualFolderData(Context context, List<Metadata> entries) {
        Log.i(TAG, "start getActualFolderData");
        ArrayList<String> folderDataList = new ArrayList<>();
        runOnUiThread(new Runnable() {
            public void run() {
                String msg = "Downloading " + entries.size() + " items. please wait.";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        for (Metadata fileMetadata : entries) {
            if (fileMetadata instanceof FolderMetadata) {
                // get folder name
                String folderName = fileMetadata.getName();
                folderDataList.add(folderName);
                Log.i(TAG, "added folderName: " + folderName);
            }
        }
        Log.i(TAG, "end getActualFolderData");
        return folderDataList;
    }

    // set up the RecyclerView
    public void setupRView(ArrayList<String> folderDataList) {
        Log.i(TAG, "start setupRView");
        Collections.sort(folderDataList, (lhs, rhs) -> lhs.compareTo(rhs));
        Context context = GetFolderActivity.this;
        RecyclerView recyclerView = findViewById(R.id.myFolders);
        adapterFolders = folderDataList;
        adapter = new SelectFolderViewAdapter(context, adapterFolders);
        adapter.setClickListener(this);
        runOnUiThread(new Runnable() {
            public void run() {
                recyclerView.setLayoutManager(new LinearLayoutManager(context));
                recyclerView.setAdapter(adapter);
            }
        });
        dumpAdapterToLog();
        // initiate and perform click event on button's
        ImageButton more = findViewById(R.id.more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "more clicked");
                // more folders...
                getMoreFolderData();
            }
        });
//        ImageButton admin = findViewById(R.id.admin);
//        admin.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.i(TAG, "admin clicked");
//                // start admin activity
//                new Thread(new Runnable() {
//                    @Override
//                    public void run() {
//                        try {
//                            Intent aA = new Intent(context, AdminActivity.class);
//                            startActivity(aA);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }).start();
//            }
//        });
        Log.i(TAG, "end setupRView");
    }

    // Update RecyclerView
    public void updateRView(ArrayList<String> folderDataList) {
        Log.i(TAG, "start updateRView");
        int insertIndex = adapterFolders.size();
        adapterFolders.addAll(insertIndex, folderDataList);
        Collections.sort(adapterFolders, (lhs, rhs) -> lhs.compareTo(rhs));
        runOnUiThread(new Runnable() {
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
        dumpAdapterToLog();
        Log.i(TAG, "end updateRView");
    }

    private void dumpAdapterToLog() {
        for (int i=0; i<adapterFolders.size(); i++) {
            Log.i(TAG, "adapterFolder[" + i + "]: " + adapterFolders.get(i));
        }
    }

    @Override
    public void onItemClick(View view, int position) {
        Log.i(TAG, "start onItemClick");
        Context context = getApplicationContext();
        Log.i(TAG, "SelectFolderViewAdapter.columns: " + SelectFolderViewAdapter.columns);
        int row = position / SelectFolderViewAdapter.columns;
        int column = position % SelectFolderViewAdapter.columns;
        Log.i(TAG, "row: " + row);
        Log.i(TAG, "column: " + column);
        String f = adapter.getItem(row);
        String msg = f + " downloading.";
        Log.i(TAG, "msg: " + msg);
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
        if (f == getString(R.string.everywhare)) {
            f = "";
        } else if (f == getString(R.string.nosubfoldershere)) {
            f = "";
            finish();
        } else {
                f = "/" + f;
        }
        String path = getIntent().getStringExtra("folder");
        if (path == null) {
            path = "";
        }
        final String folder = path + f;
        Log.i(TAG, "next folder: " + folder);
        switch (column) {
            case 0:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent gFA = new Intent(context, GetFolderActivity.class);
                            gFA.putExtra("folder", folder);
                            Log.i(TAG, "end onItemClick");
                            startActivity(gFA);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
            case 1:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Intent gPA = new Intent(context, GetPictureActivity.class);
                            gPA.putExtra("folder", folder);
                            Log.i(TAG, "end onItemClick");
                            startActivity(gPA);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                break;
        }
    }
}