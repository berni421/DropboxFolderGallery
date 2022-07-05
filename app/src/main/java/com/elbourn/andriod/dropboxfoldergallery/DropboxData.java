package com.elbourn.andriod.dropboxfoldergallery;

import android.content.Context;

import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;

import java.util.ArrayList;
import java.util.List;

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