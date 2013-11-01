package com.confiz.Utils;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Author: raheel.arif@confiz.com
 * Date: 10/30/13
 */
public class DownloadUtils {

    public static final String DOWNLOAD_DIRECTORY = "AsyncDownloadManager";

    private DownloadUtils() {
    }

    public static String getDownloadDirectory(Context context) {

        String dirPath;

        String sdCardRoot = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
        String sdCardFileRoot = sdCardRoot + DOWNLOAD_DIRECTORY + "/";

        if (!isSDCardPresent()) {
            dirPath = context.getDir(DOWNLOAD_DIRECTORY, Context.MODE_WORLD_WRITEABLE).getAbsolutePath() + "/";
        } else {
            dirPath = sdCardFileRoot;
        }
        return dirPath;
    }

    /**
     * Searches for directory if not present then create directory
     */
    public static void createDirectory(Context context) {

        File file = new File(getDownloadDirectory(context));
        if (!file.exists() || !file.isDirectory())
            file.mkdir();

    }

    private static boolean isSDCardPresent() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }


}
