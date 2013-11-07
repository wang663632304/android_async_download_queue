package com.confiz.uploadqueue.interfaces;

import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQErrors;

public interface UQResponseListener {

	public void onUploadStart(String key);

	public void updateUploadingProgress(String key, int progress);

	public void onUploadingFailer(String key, UQErrors errorNo);

	public void onUploadingCompleted(String key);

	public void updateUploadingEstimates(String key, String details[]);

	public void onUploadingDataUpdated();

	public void updateUploadingStatusOf(UQRequest UploadingReques);

	public UQRequest getUploadingRequester();

	public void updateUploadingStatusInDB(UQRequest dRequest);
}