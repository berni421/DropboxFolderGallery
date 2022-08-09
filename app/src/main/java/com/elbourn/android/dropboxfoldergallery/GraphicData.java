package com.elbourn.android.dropboxfoldergallery;

import android.graphics.Bitmap;

import java.text.SimpleDateFormat;

public class GraphicData {

//    File path;
    String onlineFolder;
    String onlinePath;
    String fileName;
    Bitmap thumbnail;
    String sortDate;
    String displayDate;
    String shortFileName;

    GraphicData(String oP, String oF, String fN, Bitmap t, Long time) {
//        path = p;
        onlineFolder = oF;
        onlinePath = oP;
        fileName = fN;
        int spacePos = fileName.indexOf(" ");
        if (spacePos > 0) {
            shortFileName = fileName.substring(0, fileName.indexOf(" "));
        } else {
            shortFileName = fileName;
        }
        thumbnail = t;
        if (time == null) {
            displayDate = "No images here, go back and try again.";
            sortDate = "";
        } else {
            SimpleDateFormat ddf = new SimpleDateFormat("dd MMM yyyy");
            displayDate = ddf.format(time);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            sortDate = sdf.format(time);
        }

    }

    static String getDisplayText(GraphicData item, String sortOrder) {
        String displayText = item.onlineFolder;
        if (sortOrder == "fileTimes") {
            displayText = item.displayDate;
        } else if (sortOrder == "fileNames") {
            displayText = item.shortFileName;
        }
        return displayText;
    }
}