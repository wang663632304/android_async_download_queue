/*
 * Property    : Confiz Solutions
 * Created by  : Arslan Anwar
 * Updated by  : Arslan Anwar
 * 
 */

package com.confiz.uploadqueue.interfaces;

import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQErrors;


/**
 * The Interface UQRequestHanlder.
 */
public interface UQRequestHanlder {

    /**
     * On upload start.
     */
    public void onUploadStart();

    /**
     * Update progress.
     * 
     * @param progress
     *            the progress
     */
    public void updateProgress(int progress);

    /**
     * On error occurred.
     * 
     * @param errorNo
     *            the error no
     */
    public void onErrorOccurred(UQErrors errorNo);

    /**
     * On complete.
     */
    public void onComplete();

    /**
     * Update uploading estimates.
     * 
     * @param details
     *            the details
     */
    public void updateUploadingEstimates(String details[]);

    /**
     * Gets the uploading requester.
     * 
     * @return the uploading requester
     */
    public UQRequest getUploadingRequester();
}
