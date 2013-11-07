
package com.confiz.uploadqueue;

import java.util.ArrayList;

import android.content.Context;
import android.content.Intent;

import com.confiz.downloadqueue.R;
import com.confiz.uploadqueue.db.UQDBAdapter;
import com.confiz.uploadqueue.interfaces.UQResponseListener;
import com.confiz.uploadqueue.model.UQActions;
import com.confiz.uploadqueue.model.UQQueue;
import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.model.UQUploadingStatus;
import com.confiz.uploadqueue.utils.UQAppConstants;
import com.confiz.uploadqueue.utils.UQAppPreference;
import com.confiz.uploadqueue.utils.UQAppUtils;
import com.confiz.uploadqueue.utils.UQDebugHelper;
import com.confiz.uploadqueue.utils.UQErrors;
import com.confiz.uploadqueue.utils.UQUtilityNetwork;

public class UQManager {


	//private final String TAG = "UploadingQueueManger";

	private UQQueue uploadQueue = null;

	private static UQManager uploadingManger = null;

	private boolean destroyQueue = false;

	private String s3AccessKey, s3SecretKey;


	private UQManager() {

		uploadQueue = UQQueue.getInstance();
	}


	public static UQManager getInstance(Context context) {

		if (uploadingManger == null) {
			uploadingManger = new UQManager();
			uploadingManger.getDataFromDatabse(context);
			uploadingManger.updateMangerForData(context);
			startUQService(context, UQActions.START_DOWNLOAD);
		}
		if (uploadingManger.uploadQueue.isEmpty()) {
			uploadingManger.getDataFromDatabse(context);
		}
		return uploadingManger;
	}


	public void getDataFromDatabse(Context context) {

		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		database.updateUploadStatus(userId);
		ArrayList<UQRequest> data = database.getUploadRequests("" + userId);
		if (data != null && data.size() > 0) {
			uploadQueue.addAll(data);
		}
	}


	public void addToQueue(UQRequest uploadRequest, Context context) {

		String userId = getCurrentUser(context);
		uploadRequest.setUserId(userId);
		UQDBAdapter database = UQDBAdapter.getInstance(context);

		if (database.isInUploadQueue(uploadRequest.getKey(), userId) == false) {
			boolean flag = database.insertRecord(uploadRequest);
			if (flag == true) {
				uploadQueue.add(uploadRequest);
			}
		}
		notifyDataUpdated();
		startUQService(context, UQActions.START_DOWNLOAD);
	}


	public boolean deleteUploadRequest(UQRequest itemToBeRemove, Context context) {

		boolean result = false;
		try {
			itemToBeRemove.setStatus(UQUploadingStatus.DELETED);
			if (itemToBeRemove != null) {
				String tempSku = itemToBeRemove.getKey();
				String userId = getCurrentUser(context);
				if (tempSku != null && tempSku.length() > 0) {
					result = UQDBAdapter.getInstance(context).deleteUQRequest(itemToBeRemove, userId);
					UQResponseHolder.getInstance().updateFileExistanceStatusInDB(itemToBeRemove);
					if (result == true) {
						uploadQueue.remove(itemToBeRemove);
						updateUploadPositions(context);
						notifyDataUpdated();
					} else {
						UQAppUtils.showDialogMessage(context, context
						        .getString(R.string.error_msg_failed_to_delete_download_request), context
						        .getString(R.string.dlg_title_error));
					}
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(exception);
		}
		return result;
	}


	public boolean deleteUploadQueue(Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.deleteUploadQueue(userId);
		if (flag == true) {
			uploadQueue.clear();
			notifyDataUpdated();
		}
		startUQService(context, UQActions.STOP_DQ);
		return flag;
	}


	public UQUploadingStatus getUploadStatus(String key, Context context) {

		UQUploadingStatus status = UQUploadingStatus.WAITING;
		if (key != null && key.length() > 0) {
			ArrayList<UQRequest> temp = uploadQueue;
			if (temp != null && temp.size() > 0) {
				for (int i = 0; i < temp.size(); i++) {
					UQRequest data = temp.get(i);
					if (data != null) {
						if (key.equals(data.getKey())) {
							status = data.getStatus();
						}
					}
				}
			}
		}
		return status;
	}


	public boolean isUploading(UQRequest uploadRequest, Context context) {

		String userId = getCurrentUser(context);
		uploadRequest.setUserId(userId);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		if (database.isInUploadQueue(uploadRequest.getKey(), userId)) {
			return true;
		}
		return false;
	}


	public boolean updateUploadRequestData(Context context, UQRequest dRequest) {

		boolean flag = false;
		try {
			String userId = getCurrentUser(context);
			if (context != null) {
				flag = UQDBAdapter.getInstance(context).updateUQRequestData(dRequest, userId);
				UQResponseHolder.getInstance().updateFileExistanceStatusInDB(dRequest);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(context, exception);
		}
		return flag;
	}


	public boolean updateUploadPositions(Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		ArrayList<UQRequest> temp = uploadQueue;
		if (temp != null && temp.size() > 0) {
			flag = UQDBAdapter.getInstance(context).updateUploadPositions(uploadQueue, userId);
		}
		return flag;
	}


	private boolean updatePositions(ArrayList<UQRequest> temp, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		if (uploadQueue != null && uploadQueue.size() > 0) {
			flag = UQDBAdapter.getInstance(context).updateUploadPositions(uploadQueue, userId);
		}
		return flag;
	}


	public boolean updateUploadPositions(ArrayList<UQRequest> temp, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		if (temp != null && temp.size() > 0) {
			synchronized (uploadQueue) {
				for (int i = 0; i < temp.size(); i++) {
					UQRequest data = temp.get(i);
					int index = uploadQueue.indexOf(data);
					UQRequest newData = uploadQueue.remove(index);
					uploadQueue.add(newData);
				}
			}
			flag = UQDBAdapter.getInstance(context).updateUploadPositions(uploadQueue, userId);
		}
		return flag;
	}


	public boolean updateUploadStatus(String key, UQUploadingStatus status, Context context) {

		UQRequest item = getItems(key);
		if (item != null) {
			item.setStatus(status);
		}
		return updateUploadStatus(item, context);
	}


	public boolean updateUploadStatus(UQRequest uploadRequest, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.updateUploadStatus(uploadRequest, userId);
		return flag;
	}


	public boolean updateErrorDescription(UQRequest uploadRequest, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.updateErrorDiscription(uploadRequest, userId);
		return flag;
	}


	public boolean updateUploadedSize(UQRequest uploadRequest, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.updateUploadedSize(uploadRequest, userId);
		return flag;
	}


	public boolean updateUploadTotalSize(UQRequest uploadRequest, Context context) {

		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.updateUploadTotalSize(uploadRequest, userId);
		return flag;
	}


	public boolean stopUploading(Context context, String key, boolean shouldDeleteFile) {

		try {
			UQRequest itemToBeStop = getItems(key);
			if (itemToBeStop == null || itemToBeStop.getStatus() == UQUploadingStatus.DELETE_REQUEST || itemToBeStop.getStatus() == UQUploadingStatus.DELETED) {
				return true;
			}
			itemToBeStop.setStatus(shouldDeleteFile ? UQUploadingStatus.DELETE_REQUEST : UQUploadingStatus.PAUSED_REQUEST);
			updateUploadStatus(itemToBeStop, context);
			if (shouldDeleteFile) {
				startUQService(context, UQActions.DELETE_ITEM);
			} else {
				startUQService(context, UQActions.PAUSE_ITEM);
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(exception);
		}
		return true;
	}


	public boolean startUploading(Context context, String key) {

		UQRequest itemToBeStart = getItems(key);
		itemToBeStart.setStatus(UQUploadingStatus.DOWNLOAD_REQUEST);
		updateUploadStatus(itemToBeStart, context);
		startUQService(context, UQActions.START_DOWNLOAD);
		return true;
	}


	public boolean startUploadingFromPause(Context context, String key) {

		UQRequest itemToBeStart = getItems(key);
		itemToBeStart.setStatus(UQUploadingStatus.DOWNLOAD_REQUEST);
		updateUploadStatus(itemToBeStart, context);
		startUQService(context, UQActions.START_DOWNLOAD_FROM_PAUSE);
		return true;
	}


	public ArrayList<UQRequest> getQueuedItemList() {

		return uploadQueue;
	}


	public boolean canUploadFurthurItems(Context context) {

		boolean flag = false;
		flag = isLimitAvailable(context);
		if (flag == true) {
			UQRequest data = getItemWithStatus(UQUploadingStatus.DOWNLOAD_REQUEST);
			if (data == null) {
				data = getItemWithStatus(UQUploadingStatus.WAITING);
				if (data == null) {
					if (UQUtilityNetwork.isNetworkAvailable(context) == true) {
						data = getItemWithStatus(UQUploadingStatus.FAILED);
						if (data == null) {
							flag = false;
						}
					} else {
						flag = false;
					}
				}
			}
		}
		return flag;
	}


	public UQRequest getNextItemToUpload(Context context) {

		UQRequest request = null;
		if (canUploadFurthurItems(context)) {
			for (int i = 0; i < uploadQueue.size(); i++) {
				UQRequest temp = uploadQueue.get(i);
				if (temp != null && (temp.getStatus() == UQUploadingStatus.DOWNLOAD_REQUEST)) {
					request = temp;
					break;
				}
			}
			if (request == null) {
				for (int i = 0; i < uploadQueue.size(); i++) {
					UQRequest temp = uploadQueue.get(i);
					if (temp != null && (temp.getStatus() == UQUploadingStatus.WAITING || temp.getStatus() == UQUploadingStatus.FAILED)) {
						request = temp;
						break;
					}
				}
			}
		}
		return request;
	}


	public int getUploadSection() {

		int index = -1;
		for (int i = 0; i < uploadQueue.size(); i++) {
			UQRequest temp = uploadQueue.get(i);
			if (temp != null && (temp.getStatus() == UQUploadingStatus.WAITING)) {
				index = i;
				break;
			}
		}
		return index;
	}


	public UQRequest getPausedUploadQequest(Context context) {

		UQRequest request = null;
		if (uploadQueue != null) {
			for (int i = 0; i < uploadQueue.size(); i++) {
				UQRequest temp = uploadQueue.get(i);
				if (temp != null && (temp.getStatus() == UQUploadingStatus.DOWNLOAD_REQUEST)) {
					request = temp;
					break;
				}
			}
		}
		return request;
	}


	public static void startUQService(Context context, UQActions action) {

		Intent intent = new Intent();
		intent.setClass(context, UQService.class);
		intent.putExtra("action", action.ordinal());
		context.startService(intent);
	}


	public boolean uploadedRequestCompleted(Context context, UQRequest itemToBeRemove) {

		boolean result = false;
		if (itemToBeRemove != null) {
			itemToBeRemove.setUploading(false);
			itemToBeRemove.setSaved(true);
			result = deleteUploadRequest(itemToBeRemove, context);
		}
		itemToBeRemove = null;
		return result;
	}


	public UQRequest getItems(String key) {

		UQRequest req = null;
		ArrayList<UQRequest> requets = UQQueue.getInstance();
		if (requets != null && requets.size() > 0) {
			for (int i = 0; i < requets.size(); i++) {
				UQRequest tempRequest = requets.get(i);
				if (tempRequest != null) {
					String reqKey = tempRequest.getKey();
					if (reqKey != null && reqKey.equals(key)) {
						req = tempRequest;
						break;
					}
				}
			}
		}
		return req;
	}


	public boolean isThereAnyItemWithStatus(UQUploadingStatus status) {

		boolean flag = false;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && (temp.getStatus() == status)) {
				flag = true;
				break;
			}
		}
		return flag;
	}


	public UQRequest getItemWithStatus(UQUploadingStatus status) {

		UQRequest request = null;
		ArrayList<UQRequest> requets = UQQueue.getInstance();
		if (requets != null && requets.size() > 0) {
			for (int i = 0; i < requets.size(); i++) {
				UQRequest tempRequest = requets.get(i);
				if (tempRequest != null && (tempRequest.getStatus() == status)) {
					request = tempRequest;
					break;
				}
			}
		}
		return request;
	}


	public void updateDBandQueue(Context context, String key, UQUploadingStatus status) {

		UQRequest itemToBeRemove = getItems(key);
		if (itemToBeRemove == null) {
			return;
		}
		switch (status) {
			case DELETED:
				itemToBeRemove.setUploading(false);
				itemToBeRemove.setSaved(false);
				itemToBeRemove.setPartialUploaded(false);
				deleteUploadRequest(itemToBeRemove, context);
				break;
			case PAUSED:
				itemToBeRemove.setUploading(true);
				updateUploadRequestData(context, itemToBeRemove);
				break;
			case COMPLETED:
				itemToBeRemove.setStatus(UQUploadingStatus.COMPLETED);
				uploadedRequestCompleted(context, itemToBeRemove);
				break;
			case FAILED:
				break;
			default:
				break;
		}
		startUQService(context, UQActions.START_DOWNLOAD);
		updateMangerAndNotify(key, itemToBeRemove);
	}


	public void updateMangerAndNotify(String key, UQRequest uploadingRequest) {

		if (uploadingRequest == null) {
			uploadingRequest = getItems(key);
		}
		if (uploadingRequest != null) {
			if (uploadingRequest.getStatus() != UQUploadingStatus.COMPLETED && uploadingRequest.getStatus() != UQUploadingStatus.DELETED && uploadingRequest
			        .getStatus() != UQUploadingStatus.DELETE_REQUEST) {
				uploadingRequest.setPartialUploaded(true);
			} else {
				uploadingRequest.setPartialUploaded(false);
			}
			if (uploadingRequest.getStatus() == UQUploadingStatus.FAILED || uploadingRequest.getStatus() == UQUploadingStatus.DOWNLOADING || uploadingRequest
			        .getStatus() == UQUploadingStatus.PAUSED || uploadingRequest.getStatus() == UQUploadingStatus.PAUSED_REQUEST) {
				uploadingRequest.setPartialUploaded(true);
			}

			UQResponseHolder.getInstance().updateStatusOf(uploadingRequest);
			notifyDataUpdated();
		}
	}


	public void updateMangerAndNotify(String key) {

		UQRequest uploadingRequest = getItems(key);
		updateMangerAndNotify(key, uploadingRequest);
	}


	public void errorOccured(Context mContext, String key, UQErrors errorNo) {

		UQRequest uploadingRequest = getItems(key);
		if (uploadingRequest != null) {
			uploadingRequest.setStatus(UQUploadingStatus.FAILED);
			updateUploadStatus(uploadingRequest, mContext);
			uploadingRequest.setErrorDiscription(mContext.getString(errorNo.value()));
			updateErrorDescription(uploadingRequest, mContext);
			int index = uploadQueue.indexOf(uploadingRequest);
			uploadQueue.remove(index);
			int newIndex = getUploadSection();
			if (newIndex != -1) {
				uploadQueue.add(newIndex, uploadingRequest);
			} else {
				uploadQueue.add(uploadingRequest);
			}
			updatePositions(uploadQueue, mContext);
		}
		updateDBandQueue(mContext, key, UQUploadingStatus.FAILED);
	}


	public void destroyManager() {

		uploadQueue.clear();
		destroyQueue = false;
		uploadingManger = null;
	}


	public boolean isAnyUploadingInProgressPending() {

		boolean flag = false;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && temp.getStatus() == UQUploadingStatus.DOWNLOADING) {
				flag = true;
				break;
			}
		}
		return flag;
	}


	public void updateMangerForData(Context context) {

		if (uploadQueue != null) {
			for (int i = 0; i < uploadQueue.size(); i++) {
				UQRequest request = uploadQueue.get(i);
				updateMangerAndNotify(request.getKey(), request);
			}
			startUQService(context, UQActions.START_DOWNLOAD);
		}
	}


	public static boolean isMangerAlive() {

		return uploadingManger == null ? false : true;
	}


	public void stopUploadingQueue(Context context) {

		destroyQueue = true;
		for (int index = 0; index < uploadQueue.size(); index++) {
			UQRequest temp = uploadQueue.get(index);
			if (temp != null && (temp.getStatus() == UQUploadingStatus.DOWNLOADING || temp.getStatus() == UQUploadingStatus.DOWNLOAD_REQUEST)) {
				temp.setStatus(UQUploadingStatus.PAUSED_REQUEST);
			}
		}
		startUQService(context, UQActions.PAUSE_ITEM);
	}


	public boolean deleteAllItemsFromQueue(Context context) {

		destroyQueue = true;
		boolean flag = false;
		String userId = getCurrentUser(context);
		UQDBAdapter database = UQDBAdapter.getInstance(context);
		flag = database.deleteUploadQueue(userId);
		if (flag == true) {
			uploadQueue.clear();
			notifyDataUpdated();
		}
		startUQService(context, UQActions.REMOVE_ITEM);
		return flag;
	}


	public boolean isDestoryQueue() {

		return destroyQueue;
	}


	public boolean isLimitAvailable(Context context) {

		boolean flag = false;
		int limit = getMaxParallelUploads(context);
		int curUploading = countCurrentUploading();
		if (curUploading < limit) {
			flag = true;
		}
		return flag;
	}


	public boolean isUploadingLimitAvailable(Context context) {

		boolean flag = false;
		int limit = getMaxParallelUploads(context);
		int curUploading = countCurrentUploadingStatus();
		if (curUploading < limit && curUploading < uploadQueue.size()) {
			flag = true;
		}
		return flag;
	}


	public int countCurrentUploadingStatus() {

		int curUploading = 0;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && (temp.getStatus() == UQUploadingStatus.DOWNLOADING)) {
				curUploading++;
			}
		}
		return curUploading;
	}


	public int countCurrentWatingOrFailed() {

		int curUploading = 0;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && (temp.getStatus() == UQUploadingStatus.FAILED || temp.getStatus() == UQUploadingStatus.WAITING)) {
				curUploading++;
			}
		}
		return curUploading;
	}


	public int countCurrentUploading() {

		int curUploading = 0;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && (temp.getStatus() == UQUploadingStatus.DOWNLOADING || temp.getStatus() == UQUploadingStatus.PAUSED || temp
			        .getStatus() == UQUploadingStatus.PAUSED_REQUEST)) {
				curUploading++;
			}
		}
		return curUploading;
	}


	public boolean putIntoUploadingSection(Context mContext, String key) {

		boolean flag = false;
		UQRequest uploadingItem = getItems(key);
		try {
			if (uploadingItem != null) {
				uploadingItem.setStatus(UQUploadingStatus.WAITING);
				flag = updateUploadStatus(uploadingItem, mContext);
				notifyDataUpdated();
			}
		} catch (Exception exception) {
			UQDebugHelper.printException(exception);
		}
		return flag;
	}


	public boolean canAddItems() {

		boolean flag = false;
		if (uploadQueue != null && uploadQueue.size() > 0) {
			int waiting = countCurrentWating();
			int limit = getMaxParallelUploads(null);
			if (waiting < limit) {
				flag = true;
			}
		} else {
			flag = true;
		}
		return flag;
	}


	public int countCurrentWating() {

		int curUploading = 0;
		for (int s = 0; s < uploadQueue.size(); s++) {
			UQRequest temp = uploadQueue.get(s);
			if (temp != null && !(temp.getStatus() == UQUploadingStatus.DOWNLOADING || temp.getStatus() == UQUploadingStatus.PAUSED || temp
			        .getStatus() == UQUploadingStatus.PAUSED_REQUEST)) {
				curUploading++;
			}
		}
		return curUploading;
	}


	/**
	 * @param context
	 */
	public static String getCurrentUser(Context context) {

		return UQAppPreference.getValue(context, UQAppConstants.KEY_USER_ID, UQAppConstants.VALUE_USER_ID);

	}


	/**
	 * @param context
	 */
	public static void setCurrentUser(Context context, String user) {

		UQAppPreference.saveValue(context, user, UQAppConstants.KEY_USER_ID);
	}


	/**
	 * @param context
	 */
	public static boolean isNotficationOn(Context context) {

		return UQAppPreference.getBoolean(context, UQAppConstants.KEY_SHOW_NOTIFICATION, UQAppConstants.VALUE_SHOW_NOTIFICATION);

	}


	/**
	 * @param context
	 */
	public static void setNotificationOn(Context context, boolean flag) {

		UQAppPreference.saveBoolean(context, flag, UQAppConstants.KEY_SHOW_NOTIFICATION);
	}


	/**
	 * @param context
	 */
	public static boolean isAutoStartOnNetworkConnect(Context context) {

		return UQAppPreference
		        .getBoolean(context, UQAppConstants.KEY_AUTO_START_ON_NETWORK_CONNECTED, UQAppConstants.VALUE_AUTO_START_ON_NETWORK_CONNECTED);

	}


	/**
	 * @param context
	 */
	public static void setAutoStartOnNetworkConnect(Context context, boolean flag) {

		UQAppPreference.saveBoolean(context, flag, UQAppConstants.KEY_AUTO_START_ON_NETWORK_CONNECTED);
	}


	/**
	 * @param context
	 */
	public static boolean isAutoStartOnAppStart(Context context) {

		return UQAppPreference
		        .getBoolean(context, UQAppConstants.KEY_AUTO_START_ON_APP_START, UQAppConstants.VALUE_AUTO_START_ON_APP_START);

	}


	/**
	 * @param context
	 */
	public static void setAutoStartOnAppStart(Context context, boolean flag) {

		UQAppPreference.saveBoolean(context, flag, UQAppConstants.KEY_AUTO_START_ON_NETWORK_CONNECTED);
	}


	/**
	 * @param context
	 */
	public static boolean isUploadOnlyOnWifi(Context context) {

		return UQAppPreference.getBoolean(context, UQAppConstants.KEY_ONLY_ON_WIFI, UQAppConstants.VALUE_ONLY_ON_WIFI);

	}


	/**
	 * @param context
	 */
	public static void setUploadOnlyOnWifi(Context context, boolean flag) {

		UQAppPreference.saveBoolean(context, flag, UQAppConstants.KEY_ONLY_ON_WIFI);
	}


	/**
	 * @param context
	 */
	public static boolean isNewItemGoOnTop(Context context) {

		return UQAppPreference
		        .getBoolean(context, UQAppConstants.KEY_PIORATIES_NEW_ITEM_TO_TOP, UQAppConstants.VALUE_PIORATIES_NEW_ITEM_TO_TOP);

	}


	/**
	 * @param context
	 */
	public static void setNewItemGoOnTop(Context context, boolean flag) {

		UQAppPreference.saveBoolean(context, flag, UQAppConstants.KEY_PIORATIES_NEW_ITEM_TO_TOP);
	}


	/**
	 * @param context
	 */
	public static int getMaxParallelUploads(Context context) {

		return UQAppPreference
		        .getInt(context, UQAppConstants.KEY_MAX_PARALLEL_DOWNLOADS, UQAppConstants.VALUE_MAX_PARALLEL_DOWNLOADS);
	}


	public static void setMaxParallelUploads(Context context, int max) {

		if (max < 4) {
			UQAppPreference.saveInt(context, max, UQAppConstants.KEY_MAX_PARALLEL_DOWNLOADS);
		} else {
			throw new RuntimeException("Max parallel upload must be less then 5");
		}
	}


	/**
	 * @param context
	 */
	public static int getMaxQueueItemLimit(Context context) {

		return UQAppPreference.getInt(context, UQAppConstants.KEY_MAX_QUEUE_LIMIT, UQAppConstants.VALUE_MAX_QUEUE_LIMIT);
	}


	public static void setMaxQueueItemLimit(Context context, int max) {

		if (max <= UQAppConstants.VALUE_MAX_QUEUE_LIMIT) {
			UQAppPreference.saveInt(context, max, UQAppConstants.KEY_MAX_QUEUE_LIMIT);
		} else {
			throw new RuntimeException("Max item limit must be less then " + UQAppConstants.VALUE_MAX_QUEUE_LIMIT);
		}
	}


	/**
	 * @param context
	 */
	public static int getMaxRetries(Context context) {

		return UQAppPreference.getInt(context, UQAppConstants.KEY_NO_OF_RETRIES, UQAppConstants.VALUE_NO_OF_RETRIES);
	}


	public static void setMaxRetries(Context context, int max) {

		if (max < UQAppConstants.VALUE_NO_OF_RETRIES) {
			UQAppPreference.saveInt(context, max, UQAppConstants.KEY_NO_OF_RETRIES);
		} else {
			throw new RuntimeException("Max retries must be less then " + UQAppConstants.VALUE_NO_OF_RETRIES);
		}
	}


	/**
	 * @param context
	 */
	public static int getMaxFileSizeAllowed(Context context) {

		return UQAppPreference.getInt(context, UQAppConstants.KEY_MAX_FILE_SIZE, UQAppConstants.VALUE_MAX_FILE_SIZE);
	}


	/**
	 * @param context
	 * 
	 * @param maxSize
	 *            set -1 if you for no limit
	 */
	public static void setMaxFileSizeAllowed(Context context, int maxSize) {

		if (maxSize < 0) {
			UQAppPreference.saveInt(context, maxSize, UQAppConstants.KEY_MAX_FILE_SIZE);
		} else {
			throw new RuntimeException("Max retries must be less then 10");
		}
	}


	/**
	 * @param mContext
	 * @return
	 */
	public static boolean isConfiguredNetworkAvailable(Context mContext) {

		boolean isNetwrokOn = false;
		if (UQUtilityNetwork.isNetworkAvailable(mContext)) {
			boolean onlyOnWifi = UQManager.isUploadOnlyOnWifi(mContext);
			if (onlyOnWifi) {
				if (UQUtilityNetwork.isConnectedToWifi(mContext)) {
					isNetwrokOn = true;
				}
			} else {
				isNetwrokOn = true;
			}
		}
		return isNetwrokOn;
	}


	public void addUQResponseListiner(UQResponseListener listener) {

		UQResponseHolder.getInstance().addListener(listener);
	}


	public void removeUQResponseListiner(UQResponseListener listener) {

		UQResponseHolder.getInstance().removeListener(listener);
	}


	public void removeAllUQResponseListiner(UQResponseListener listener) {

		UQResponseHolder.getInstance().removeAllListener();
	}
	
	public boolean contain(UQResponseListener listener) {
		return UQResponseHolder.getInstance().contain(listener);
	}


	/**
	 * Replace.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void replace(UQResponseListener listener) {

		UQResponseHolder.getInstance().replace(listener);
	}



	private void notifyDataUpdated() {

		UQResponseHolder.getInstance().onDataUpdated();
	}


	/**
	 * @return the s3AccessKey
	 */
	public String getS3AccessKey() {

		return s3AccessKey;
	}


	/**
	 * @return the s3SecretKey
	 */
	public String getS3SecretKey() {

		return s3SecretKey;
	}


	/**
	 * @param s3AccessKey
	 *            the s3AccessKey to set
	 */
	public void setS3AccessKey(String s3AccessKey) {

		this.s3AccessKey = s3AccessKey;
	}


	/**
	 * @param s3SecretKey
	 *            the s3SecretKey to set
	 */
	public void setS3SecretKey(String s3SecretKey) {

		this.s3SecretKey = s3SecretKey;
	}

	/**
	 * @param s3AccessKey
	 * @param s3SecretKey
	 */
	public void setS3Credientials(String s3AccessKey , String s3SecretKey) {

		this.s3AccessKey = s3AccessKey;
		this.s3SecretKey = s3SecretKey;
	}
}
