
package com.confiz.uploadqueue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.confiz.uploadqueue.interfaces.UQResponseListener;
import com.confiz.uploadqueue.model.UQActions;
import com.confiz.uploadqueue.model.UQUploadingStatus;
import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQDebugHelper;
import com.confiz.uploadqueue.utils.UQErrors;
import com.confiz.uploadqueue.utils.UQUtilityNetwork;

public class UQService extends Service {


	// private String TAG = "UQService";
	private Context mContext = null;

	private final String extraParameterName = "action";

	HashMap<String, UQFileUploader> dqTheadHashmap = new HashMap<String, UQFileUploader>();

	HashMap<String, UQRequest> dqKeysRequestsHashMap = new HashMap<String, UQRequest>();


	@Override
	public IBinder onBind(Intent intent) {

		return null;
	}


	@Override
	public void onCreate() {

		super.onCreate();
		mContext = this;
	}


	@Override
	public void onDestroy() {

		Log.i("LTDUploadingQueue", "Uploading service is destroying");
		super.onDestroy();
	}


	@Override
	public int onStartCommand(Intent intent, int flag , int startId) {
		super.onStartCommand(intent, flag, startId);
		
		try {
			if (mContext == null) {
				mContext = this;
			}
			if (intent == null || intent.getExtras() == null || intent.hasExtra(extraParameterName) == false) {
				return -1;
			}
			UQActions action = UQActions.get(intent.getIntExtra(extraParameterName, 0));
			UQManager dqm = UQManager.getInstance(mContext);
			switch (action) {
				case START_DOWNLOAD:
					startUpload(dqm);
					break;
				case START_DOWNLOAD_FROM_PAUSE:
					startUploadFromPause(dqm);
					break;
				case STOP_DQ:
					stopCompleteUpload();
					break;
				case UPDATE_DQ:
					break;
				case PAUSE_ITEM:
					makeItemPause(dqm);
					break;
				case DELETE_ITEM:
					deleteItem(dqm);
					break;
				case REMOVE_ITEM:
					stopCompleteUpload();
					if (dqm.isDestoryQueue()) {
						dqm.destroyManager();
					}
					break;
			}

		} catch (NullPointerException exception) {
			UQDebugHelper.printException(mContext, exception);
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(mContext, exception);
		}
		return -1;
	}


	public void startUpload(UQManager dqm) {

		// verifyUploadingItems(dqm);
		if (dqm.canUploadFurthurItems(mContext)) {
			while (dqm.canUploadFurthurItems(mContext)) {
				UQRequest dr = dqm.getNextItemToUpload(mContext);
				if (UQUtilityNetwork.isNetworkAvailable(mContext) == false) {
					break;
				}

				UQFileUploader atQueue = null;
				if (dqKeysRequestsHashMap.containsKey(dr.getKey()) == false) {
					atQueue = new UQFileUploader(mContext, null, dr, uploadQueue);
					atQueue.start();
					dqKeysRequestsHashMap.put(dr.getKey(), dr);
					dqTheadHashmap.put(dr.getKey(), atQueue);
					dr.setStatus(UQUploadingStatus.DOWNLOADING);
					dqm.updateUploadStatus(dr, mContext);
					dqm.updateMangerAndNotify(dr.getKey(), dr);
				} else {
					dr.setStatus(UQUploadingStatus.DOWNLOADING);
					dqm.updateUploadStatus(dr, mContext);
					dqm.updateMangerAndNotify(dr.getKey(), dr);
				}
			}
		}
	}


	private void verifyUploadingItems(UQManager dqm) {

		try {
			ArrayList<UQRequest> uploadingItems = dqm.getQueuedItemList();
			if (uploadingItems != null && uploadingItems.isEmpty() == false) {
				for (UQRequest temp : uploadingItems) {
					if (temp.getStatus() == UQUploadingStatus.DOWNLOADING) {
						if (dqTheadHashmap.containsKey(temp.getKey()) == false) {
							UQDebugHelper.printData("Restarting this = " + temp.getKey());
							temp.setStatus(UQUploadingStatus.WAITING);
							dqm.updateUploadStatus(temp, mContext);
						}
					}
				}
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(exception);
		}

	}


	public void deleteItem(UQManager dqm) {

		while (dqm.isThereAnyItemWithStatus(UQUploadingStatus.DELETE_REQUEST)) {
			UQRequest dr = dqm.getItemWithStatus(UQUploadingStatus.DELETE_REQUEST);
			String key = dr.getKey();
			if (dqKeysRequestsHashMap.containsKey(dr.getKey())) {
				UQFileUploader temp = dqTheadHashmap.get(key);
				temp.shouldDeleteFile = true;
				temp.doWorkForCancel();
				temp.cancel(true);
				temp.updateRequestStatus();
				dqTheadHashmap.remove(key);
				dqKeysRequestsHashMap.remove(key);
			}
			dr.setStatus(UQUploadingStatus.DELETED);
			dqm.updateUploadStatus(dr, mContext);
			dr.setUploading(false);
			dr.setSaved(false);
			dr.setPartialUploaded(false);
			dqm.updateDBandQueue(mContext, key, UQUploadingStatus.DELETED);
		}
	}


	public void makeItemPause(UQManager dqm) {

		while (dqm.isThereAnyItemWithStatus(UQUploadingStatus.PAUSED_REQUEST)) {
			UQRequest dr = dqm.getItemWithStatus(UQUploadingStatus.PAUSED_REQUEST);
			String key = dr.getKey();
			if (dqKeysRequestsHashMap.containsKey(dr.getKey())) {
				UQFileUploader temp = dqTheadHashmap.get(key);
				temp.shouldDeleteFile = false;
				temp.doWorkForCancel();
				temp.cancel(true);
				temp.updateRequestStatus();
				dqTheadHashmap.remove(key);
				dqKeysRequestsHashMap.remove(key);
				dr.setDataEstimations(null);
				dr.setTimeEstimations(null);
			}
			dr.setStatus(UQUploadingStatus.PAUSED);
			dqm.updateUploadStatus(dr, mContext);
			dqm.updateDBandQueue(mContext, key, UQUploadingStatus.PAUSED);
		}
		if (dqm.isDestoryQueue()) {
			dqm.destroyManager();
		}
	}


	public void startUploadFromPause(UQManager dqm) {

		if (dqm.isUploadingLimitAvailable(mContext) == true) {
			UQRequest dr = dqm.getPausedUploadQequest(mContext);
			if (UQUtilityNetwork.isNetworkAvailable(mContext) == false) {
				return;
			}
			if (dr == null) {
				return;
			}
			UQFileUploader atQueue = null;
			if (dqKeysRequestsHashMap.containsKey(dr.getKey()) == false) {
				atQueue = new UQFileUploader(mContext, null, dr, uploadQueue);
				atQueue.start();
				dqKeysRequestsHashMap.put(dr.getKey(), dr);
				dqTheadHashmap.put(dr.getKey(), atQueue);
				dr.setStatus(UQUploadingStatus.DOWNLOADING);
				dqm.updateUploadStatus(dr, mContext);
				dqm.updateMangerAndNotify(dr.getKey(), dr);
			} else {
				dr.setStatus(UQUploadingStatus.DOWNLOADING);
				dqm.updateUploadStatus(dr, mContext);
				dqm.updateMangerAndNotify(dr.getKey(), dr);
			}
		}
	}


	public void stopCompleteUpload() {

		Collection<UQFileUploader> cAsyncTasks = dqTheadHashmap.values();
		for (UQFileUploader asyncFileUploaderForQueue : cAsyncTasks) {
			asyncFileUploaderForQueue.cancel(true);
			asyncFileUploaderForQueue.doWorkForCancel();
		}
	}

	UQResponseListener uploadQueue = new UQResponseListener() {


		@Override
		public void updateUploadingProgress(String key, int progress) {

			if (UQResponseHolder.getInstance() != null) {
				UQResponseHolder.getInstance().updateProgress(key, progress);
			}
			UQManager manager = UQManager.getInstance(mContext);
			if (manager != null) {
				UQRequest item = manager.getItems(key);
				if (item != null) {
					item.setProgress(progress);
				}
			}
		}


		@Override
		public void updateUploadingEstimates(String key, String[] details) {

			if (UQResponseHolder.getInstance() != null) {
				UQResponseHolder.getInstance().updateUploadingEstimates(key, details);
			}

			UQManager manager = UQManager.getInstance(mContext);
			if (manager != null) {
				UQRequest item = manager.getItems(key);
				if (item != null) {
					item.setEstimates(details);
				}
			}
		}


		@Override
		public void onUploadingFailer(String key, UQErrors errorNo) {

			if (dqKeysRequestsHashMap != null) {
				if (dqKeysRequestsHashMap.containsKey(key)) {
					dqKeysRequestsHashMap.remove(key);
				}
				if (dqTheadHashmap.containsKey(key)) {
					dqTheadHashmap.remove(key);
				}
			}

			try {
				// UQRequest itemToBeDell = UQManager.getInstance(mContext).getItems(key)
				// .getRequestedObject();
			} catch (Exception exception) {
				exception.printStackTrace();
			}

			UQManager manager = UQManager.getInstance(mContext);
			manager.errorOccured(mContext, key, errorNo);
			if (UQUtilityNetwork.isNetworkAvailable(mContext) == false) {
				if (manager.isAnyUploadingInProgressPending() == false) {
					// manager.destroyManger();
					stopSelf();
				}
			} else if (UQUtilityNetwork.isNetworkAvailable(mContext) == true && manager.canUploadFurthurItems(mContext) == false) {
				// manager.destroyManger();
				stopSelf();
			}
			if (UQResponseHolder.getInstance() != null) {
				UQResponseHolder.getInstance().onErrorOccurred(key, errorNo);
			}
		}

		@Override
		public void onUploadStart(String key) {

			if (UQResponseHolder.getInstance() != null) {
				UQResponseHolder.getInstance().onUploadStart(key);
			}
			UQManager.getInstance(mContext).updateMangerAndNotify(key);
		}


		@Override
		public void onUploadingCompleted(String key) {

			UQManager manager = UQManager.getInstance(mContext);
			UQRequest testRequest = manager.getItems(key);
			if (testRequest != null) {
				testRequest.setStatus(UQUploadingStatus.COMPLETED);
				manager.updateUploadStatus(testRequest, mContext);
			}

			manager.updateDBandQueue(mContext, key, UQUploadingStatus.COMPLETED);
			if (UQUtilityNetwork.isNetworkAvailable(mContext) == false) {
				if (manager.isAnyUploadingInProgressPending() == false) {
					// manager.destroyManger();
					stopSelf();
				}
			} else if (UQUtilityNetwork.isNetworkAvailable(mContext) == true && manager.canUploadFurthurItems(mContext) == false) {
				// manager.destroyManger();
				stopSelf();
			}
			if (dqTheadHashmap.containsKey(key)) {
				dqTheadHashmap.remove(key);
			}
			if (UQResponseHolder.getInstance() != null) {
				UQResponseHolder.getInstance().onComplete(key);
			}
			if (dqKeysRequestsHashMap != null) {
				if (dqKeysRequestsHashMap.containsKey(key)) {
					UQRequest temp = dqKeysRequestsHashMap.remove(key);
					// if (UQManager.isNotficationOn(mContext)) {
					// UQUtilityNotifiacation.showNotification(mContext, "" + temp.getTitle(),
					// "Uploaded Successfully");
					// }
				}
			}

		}


		@Override
		public UQRequest getUploadingRequester() {

			return null;
		}


		@Override
		public void onUploadingDataUpdated() {

		}


		@Override
		public void updateUploadingStatusOf(UQRequest uploadingReques) {

			// TODO Auto-generated method stub

		}


		@Override
		public void updateUploadingStatusInDB(UQRequest dRequest) {

			// TODO Auto-generated method stub

		}
	};
}