package com.elbourn.android.dropboxfoldergallery;

import com.dropbox.core.v2.DbxClientV2;

public class DropboxData {
    DbxClientV2 client;
    Boolean hasMore;
    String cursor;
    DropboxData(DbxClientV2 ct, String cr, Boolean hM) {
        client = ct;
        cursor = cr;
        hasMore = hM;
    }
}