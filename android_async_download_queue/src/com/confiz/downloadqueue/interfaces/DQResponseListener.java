
package com.confiz.downloadqueue.interfaces;

import com.confiz.downloadqueue.model.DQRequest;
import com.confiz.downloadqueue.utils.DQErrors;

/**
 * Listener for getting active download progress and state
 */


public interface DQResponseListener {

    /**
     * Called when download is started
     *
     * @param key identifier for download
     */
    public void onDownloadStart(String key);

    /**
     * Called when download is started
     *
     * @param key       identifier for download
     * @param totalSize
     */
    public void onDownloadStart(String key, int totalSize);

    /**
     * Called when files download progresses
     *
     * @param key      identifier for download
     * @param progress
     */
    public void updateProgress(String key, int progress);

    /**
     * Called when an error is occurred during download
     *
     * @param key     identifier for download
     * @param errorNo
     */
    public void onErrorOccurred(String key, DQErrors errorNo);

    /**
     * Called when download is completed
     *
     * @param key identifier for download
     */
    public void onComplete(String key);

    /**
     * Called when files download progresses
     *
     * @param key     identifier for download
     * @param details is arrayList having following data
     *                details[0]="current downloaded data"
     *                details[1]="total size"
     *                details[2]="download speed"
     *                download[3] ="estimated time"
     *                download[4]= "percentage"
     */
    public void updateDownloadingEstimates(String key, String details[]);

    /**
     * TODO ask from arslan
     */
    public void onDataUpdated();

    /**
     * TODO ask from arslan
     *
     * @param downloadingReques
     */
    public void updateStatusOf(DQRequest downloadingReques);

    /**
     * TODO ask from arslan
     *
     * @return
     */
    public DQRequest getDownloadingRequester();

    /**
     * TODO ask from arslan
     *
     * @param dRequest
     */
    public void updateFileExistanceStatusInDB(DQRequest dRequest);
}