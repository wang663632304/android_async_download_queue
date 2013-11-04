package com.confiz.uploadqueue.interfaces;

import java.util.ArrayList;

import com.confiz.uploadqueue.model.UQUploadingStatus;
import com.confiz.uploadqueue.model.UQRequest;

public interface UQRequestDBInterface {

	public boolean insertUploadRequest(UQRequest dRequest, String userId);

	public ArrayList<UQRequest> getUploadRequests(String userId);

	public boolean deleteUploadRequest(UQRequest dRequest, String userId);

	public boolean deleteUploadQueue(String userId);

	public UQUploadingStatus getUploadStatus(String key, String userId);

	public boolean isInUploadQueue(String key, String userId);

	public boolean updateUploadPositions(ArrayList<UQRequest> list, String userId);

	public boolean updateUploadStatus(UQRequest dRequest, String userId);

	public boolean updateUploadedSize(UQRequest dRequest, String userId);

	public boolean updateUploadTotalSize(UQRequest dRequest, String userId);

	public boolean updateErrorDiscription(UQRequest dRequest, String userId);
}
