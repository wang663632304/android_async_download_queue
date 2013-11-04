package com.confiz.uploadqueue.interfaces;

import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQErrors;

public interface UQResponseListener {

	public void onUploadStart(String key);

	public void updateProgress(String key, int progress);

	public void onErrorOccurred(String key, UQErrors errorNo);

	public void onComplete(String key);

	public void updateUploadingEstimates(String key, String details[]);

	public void onDataUpdated();

	public void updateStatusOf(UQRequest UploadingReques);

	public UQRequest getUploadingRequester();

	public void updateFileExistanceStatusInDB(UQRequest dRequest);
}