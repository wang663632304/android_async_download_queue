package com.confiz.listeners;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.confiz.downloadqueue.interfaces.DQResponseListener;
import com.confiz.downloadqueue.model.DQRequest;
import com.confiz.downloadqueue.utils.DQErrors;

/**
 * Author: raheel.arif@confiz.com
 * Date: 10/30/13
 */
public class DownloadProgressListener implements DQResponseListener {

    private static final String DOWNLOAD_MANAGER = "download_manager";
    private final Context context;

    public DownloadProgressListener(Context context) {
        this.context = context;
    }

    @Override
    public void onDownloadStart(String key) {
        Log.i(this.getClass().getSimpleName(), "download started" + key);
    }

    @Override
    public void onDownloadStart(String key, int totalSize) {
        Log.i(this.getClass().getSimpleName(), "download started" + key + " size:" + totalSize);

        Intent broadCast = new Intent(DOWNLOAD_MANAGER);
        broadCast.putExtra("event", "onDownloadStart");
        broadCast.putExtra("key", key);
        broadCast.putExtra("totalSize", totalSize);
        context.sendBroadcast(broadCast);
    }

    @Override
    public void updateProgress(String key, int progress) {
        Intent broadCast = new Intent(DOWNLOAD_MANAGER);
        broadCast.putExtra("event", "updateProgress");
        broadCast.putExtra("key", key);
        broadCast.putExtra("progress", progress);
        context.sendBroadcast(broadCast);
    }

    @Override
    public void onErrorOccurred(String key, DQErrors errorNo) {
        Log.i(this.getClass().getSimpleName(), "onErrorOccurred  " + key);
    }

    @Override
    public void onComplete(String key) {
        Log.i(this.getClass().getSimpleName(), "onComplete  " + key);

        Intent broadCast = new Intent(DOWNLOAD_MANAGER);
        broadCast.putExtra("event", "onComplete");
        broadCast.putExtra("key", key);
        context.sendBroadcast(broadCast);
    }

    @Override
    public void updateDownloadingEstimates(String key, String[] details) {
    }

    @Override
    public void onDataUpdated() {
    }

    @Override
    public void updateStatusOf(DQRequest downloadingReques) {
    }

    @Override
    public DQRequest getDownloadingRequester() {
        return null;
    }

    @Override
    public void updateFileExistanceStatusInDB(DQRequest dRequest) {
    }
}
