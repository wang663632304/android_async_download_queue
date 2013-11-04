
package com.confiz.uploadqueue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AbortMultipartUploadRequest;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.CompleteMultipartUploadResult;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.UploadPartRequest;
import com.amazonaws.services.s3.model.UploadPartResult;
import com.confiz.uploadqueue.interfaces.UQRequestHanlder;
import com.confiz.uploadqueue.interfaces.UQResponseListener;
import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.SharedPreferencesCompat;
import com.confiz.uploadqueue.utils.SharedPreferencesUtils;
import com.confiz.uploadqueue.utils.UQDebugHelper;
import com.confiz.uploadqueue.utils.UQErrors;
import com.confiz.uploadqueue.utils.UQExternalStorageHandler;
import com.confiz.uploadqueue.utils.UQUtilityNetwork;
import com.confiz.uploadqueue.utils.UploadIterruptedException;

public class UQFileUploader extends Thread {


	/** The tag. */
	protected final String TAG = "UQFileUploader";

	/** The context. */
	protected Context mContext = null;

	/** The caller. */
	// private UQRequestHanlder mCaller = null;

	protected String uploadedFileURL = null;

	protected long fileSize = -1;

	/** The current completed progress. */
	protected int currentCompletedProgress = 0;

	/** The which message to display. */
	public UQErrors whichMessageToDisplay = UQErrors.UNABLE_TO_DOWNLOAD_FILE;

	/** The stop uploading. */
	public boolean stopUploading = false;

	/** The should delete file. */
	public boolean shouldDeleteFile = false;

	public UQResponseListener queueListener = null;

	public UQRequest request = null;
	
	public boolean uploadingSucess = false;

	private static final long MIN_DEFAULT_PART_SIZE = 5 * 1024 * 1024;

	private static final String PREFS_NAME = "preferences_simpl3r";
	private static final String PREFS_UPLOAD_ID = "_uploadId";
	private static final String PREFS_ETAGS = "_etags";
	private static final String PREFS_ETAG_SEP = "~~";

	private AmazonS3Client s3Client;
	private String s3bucketName;
	private String s3FileName;
	private File file;

	private SharedPreferences prefs;
	private long partSize = MIN_DEFAULT_PART_SIZE;
	private long bytesUploaded = 0;
	private boolean userInterrupted = false;
	private boolean userAborted = false;


	public UQFileUploader(Context context, UQRequestHanlder listener, UQRequest data, UQResponseListener queueListener) {

		mContext = context;
		request = data;
		this.queueListener = queueListener;
		request.currentError = UQErrors.NO_ERROR;
		if(request != null){
			s3bucketName = request.getS3BucketName();
			s3FileName = request.getFileName();
			
			s3Client = new AmazonS3Client(
					new BasicAWSCredentials("", ""));
		}
	}


	@Override
	public void run() {

		super.run();
		onPreExecute();

		if (UQManager.isConfiguredNetworkAvailable(mContext) == false) {
			whichMessageToDisplay = UQErrors.NETWORK_WEAK;
			uploadingSucess = false;
		}
		request.setUploading(true);

		Boolean uploadStatus = false;
		File destinationFile = new File(request.getFilePath());
		if(destinationFile != null){
			fileSize = destinationFile.length();
		}
		
		request.setTotalSize(fileSize);
		this.queueListener.onUploadStart(request.getKey());
		try {
			// initialize
			List<PartETag> partETags = new ArrayList<PartETag>();
			final long contentLength = destinationFile.length();
			long filePosition = 0;
			int startPartNumber = 1;

			userInterrupted = false;
			userAborted = false;
			bytesUploaded = 0;

			// check if we can resume an incomplete upload
			String uploadId = request.getKey();

			if (uploadId != null) {
				// we can resume the upload
				Log.i(TAG, "resuming upload for " + uploadId);

				// get the cached etags
				List<PartETag> cachedEtags = getCachedPartEtags();
				partETags.addAll(cachedEtags);

				// calculate the start position for resume
				startPartNumber = cachedEtags.size() + 1;
				filePosition = (startPartNumber - 1) * partSize;
				bytesUploaded = filePosition;

				Log.i(TAG, "resuming at part " + startPartNumber + " position "
						+ filePosition);
				startingTime = System.currentTimeMillis();
			} else {
				// initiate a new multi part upload
				Log.i(TAG, "initiating new upload");

				InitiateMultipartUploadRequest initRequest = new InitiateMultipartUploadRequest(
						s3bucketName, s3FileName);
				configureInitiateRequest(initRequest);
				InitiateMultipartUploadResult initResponse = s3Client
						.initiateMultipartUpload(initRequest);
				uploadId = initResponse.getUploadId();
				startingTime = System.currentTimeMillis();
			}

			final AbortMultipartUploadRequest abortRequest = new AbortMultipartUploadRequest(
					s3bucketName, s3FileName, uploadId);

			for (int k = startPartNumber; filePosition < contentLength; k++) {

				long thisPartSize = Math.min(partSize,
						(contentLength - filePosition));

				Log.i(TAG, "starting file part " + k + " with size " + thisPartSize);

				UploadPartRequest uploadRequest = new UploadPartRequest()
						.withBucketName(s3bucketName).withKey(s3FileName)
						.withUploadId(uploadId).withPartNumber(k)
						.withFileOffset(filePosition).withFile(file)
						.withPartSize(thisPartSize);
				
				boolean uploadSuccess = false;
				File file = null;
				int updateCounter = 0;
				final long startinTime = System.currentTimeMillis();
				final int updateAfterThisTimeIteration = 75;
				long currentUploadBytes = 0;
				long previouslyUploadedFileSize = 0;
				
				com.amazonaws.services.s3.model.ProgressListener s3progressListener = new com.amazonaws.services.s3.model.ProgressListener() {
					public void progressChanged(ProgressEvent progressEvent) {

						// bail out if user cancelled
						// TODO calling shutdown too brute force?
						if (userInterrupted) {
							s3Client.shutdown();
							throw new UploadIterruptedException("User interrupted");
						} else if (userAborted) {
							// aborted requests cannot be resumed, so clear any
							// cached etags
							clearProgressCache();
							s3Client.abortMultipartUpload(abortRequest);
							s3Client.shutdown();
						}

						bytesUploaded += progressEvent.getBytesTransferred();
						realTotal += progressEvent.getBytesTransferred();
						onProgressRecived();
					}
				};

				uploadRequest.setProgressListener(s3progressListener);

				UploadPartResult result = s3Client.uploadPart(uploadRequest);

				partETags.add(result.getPartETag());

				// cache the part progress for this upload
				if (k == 1) {
					initProgressCache(uploadId);
				}
				// store part etag
				cachePartEtag(result);

				filePosition += thisPartSize;
			}

			CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
					s3bucketName, s3FileName, uploadId, partETags);

			CompleteMultipartUploadResult result = s3Client
					.completeMultipartUpload(compRequest);
			bytesUploaded = 0;

			Log.i(TAG, "upload complete for " + uploadId);

			clearProgressCache();

			uploadedFileURL =  result.getLocation();

		} catch (final Exception exception) {
			UQDebugHelper.printAndTrackException(this.mContext, exception);
		}
		uploadingSucess = uploadStatus;

		onPostExecute(uploadStatus);
	}
	
	int updateCounter = 0;
	long startingTime = System.currentTimeMillis();
	int updateAfterThisTimeIteration = 75;
	long currentUploadBytes = 0;
	long previouslyUploadedFileSize = 0;
	long realTotal = 0;
	
	private void onProgressRecived(){
		// Log.d(TAG, "bytesUploaded=" + bytesUploaded);
		if (updateCounter >= this.updateAfterThisTimeIteration) {
			updateCounter = 0;
			float fpercent = ((bytesUploaded * 100) / fileSize);
			int percent = Math.round(fpercent);
			queueListener.updateProgress(request.getKey() , percent);
			final String[] allEstimatedData = UQUtilityNetwork
			        .getUploadingEstimates(startingTime, (int) fileSize, (int) bytesUploaded, realTotal);
			allEstimatedData[4] = "" + percent;
			queueListener.updateUploadingEstimates(request.getKey() , allEstimatedData);
		}
	}


	protected void onPostExecute(Boolean UploadingCompleted) {

		try {
			request.setUploading(false);
			request.setSaved(UploadingCompleted);
			updateRequestStatus();
			updateRequestItemNewData();

			String key = request.getKey();

			this.updateStatusAndDeletFile(this.shouldDeleteFile);
			
			if (UploadingCompleted) {
				this.queueListener.onComplete(request.getKey());
			} else {

				if (whichMessageToDisplay == UQErrors.NO_ERROR && request.currentError != UQErrors.NO_ERROR) {
					whichMessageToDisplay = request.currentError;
				}
				queueListener.onErrorOccurred(request.getKey(), whichMessageToDisplay);
			}
		} catch (Exception exception) {
			UQDebugHelper.printAndTrackException(mContext, TAG, exception);
		}
	}


	protected void onPreExecute() {

		whichMessageToDisplay = UQErrors.UNABLE_TO_DOWNLOAD_FILE;
		if (queueListener != null) {
			queueListener.onUploadStart(request.getKey());
		}
	}


	public void doWorkForCancel() {

		stopUploading = true;
		updateStatusAndDeletFile(shouldDeleteFile);
	}


	public void updateStatusAndDeletFile(boolean shouldDeleteFile) {

		request.setUploading(false);
		String filePath = request.getFilePath();
		if (filePath != null && filePath.length() > 0) {
			File fileToDelete = new File(filePath);

			if (fileToDelete.exists() && fileToDelete.isFile()) {
				if (shouldDeleteFile == false) {
					if (fileToDelete.length() > UQExternalStorageHandler.SIZE_KB * 300) {
						request.setPartialUploaded(true);
						// UQDBAdapter.getInstance(mContext).updateFileExistanceStatusInDB(request);
						request.setUploadedSize(fileToDelete.length());
						updateRequestItemNewData();
					}
				} else {
					fileToDelete.delete();
					request.setPartialUploaded(false);
					request.setTotalSize(0);
					request.setUploadedSize(0);
					updateRequestItemData();
				}
			}
		}
	}


	public void updateProgress(int progressPercentage) {

		if (progressPercentage < 0) {
			progressPercentage = 0;
		} else if (progressPercentage > 100) {
			progressPercentage = 100;
		}
		currentCompletedProgress = progressPercentage;
		if (queueListener != null) {
			queueListener.updateProgress(request.getKey(), progressPercentage);
			request.setProgress(progressPercentage);
		}
	}


	public long getFileSize() {

		return fileSize;
	}


	public void setFileSize(long fileSize) {

		this.fileSize = fileSize;
	}


	public void updateUploadingDetails(String details[]) {

		if (queueListener != null) {
			this.queueListener.updateUploadingEstimates(request.getKey(), details);
		}
	}


	public int getCurrentCompletedProgress() {

		return currentCompletedProgress;
	}


	public void updateRequestStatus() {

		String filePath = request.getFilePath();
		if (filePath != null && filePath.length() > 0) {
			File fileToDelete = new File(filePath);

			if (fileToDelete.exists() && fileToDelete.isFile()) {
				if (shouldDeleteFile == false) {
					if (fileToDelete.length() > UQExternalStorageHandler.SIZE_KB * 300) {
						request.setUploadedSize(fileToDelete.length());
						if (fileSize > 0) {
							request.setTotalSize(fileSize);
						}
					}
				}
			}
		}
	}


	private void updateRequestItemNewData() {

		if (request.getTotalSize() > 0) {
			UQManager.getInstance(mContext).updateUploadTotalSize(request, mContext);
		}

		if (request.getUploadedSize() > 0) {
			UQManager.getInstance(mContext).updateUploadedSize(request, mContext);
		}
	}


	private void updateRequestItemData() {

		UQManager.getInstance(mContext).updateUploadTotalSize(request, mContext);
		UQManager.getInstance(mContext).updateUploadedSize(request, mContext);
	}

	/**
	 * @param b
	 */
	public void cancel(boolean shouldKill) {

		stopUploading = true;
		updateStatusAndDeletFile(shouldDeleteFile);
	}
	

	private List<PartETag> getCachedPartEtags() {
		List<PartETag> result = new ArrayList<PartETag>();
		// get the cached etags
		ArrayList<String> etags = SharedPreferencesUtils.getStringArrayPref(
				prefs, s3FileName + PREFS_ETAGS);
		for (String etagString : etags) {
			String partNum = etagString.substring(0,
					etagString.indexOf(PREFS_ETAG_SEP));
			String partTag = etagString
					.substring(etagString.indexOf(PREFS_ETAG_SEP) + 2,
							etagString.length());

			PartETag etag = new PartETag(Integer.parseInt(partNum), partTag);
			result.add(etag);
		}
		return result;
	}

	private void cachePartEtag(UploadPartResult result) {
		String serialEtag = result.getPartETag().getPartNumber()
				+ PREFS_ETAG_SEP + result.getPartETag().getETag();
		ArrayList<String> etags = SharedPreferencesUtils.getStringArrayPref(
				prefs, s3FileName + PREFS_ETAGS);
		etags.add(serialEtag);
		SharedPreferencesUtils.setStringArrayPref(prefs, s3FileName + PREFS_ETAGS,
				etags);
	}

	private void initProgressCache(String uploadId) {
		// store uploadID
		Editor edit = prefs.edit().putString(s3FileName + PREFS_UPLOAD_ID, uploadId);
		SharedPreferencesCompat.apply(edit);
		// create empty etag array
		ArrayList<String> etags = new ArrayList<String>();
		SharedPreferencesUtils.setStringArrayPref(prefs, s3FileName + PREFS_ETAGS,
				etags);
	}

	private void clearProgressCache() {
		// clear the cached uploadId and etags
		Editor edit = prefs.edit();
		edit.remove(s3FileName + PREFS_UPLOAD_ID);
		edit.remove(s3FileName + PREFS_ETAGS);
		SharedPreferencesCompat.apply(edit);
	}

	public void interrupt() {
		userInterrupted = true;
	}

	public void abort() {
		userAborted = true;
	}

	/**
	 * Override to configure the multipart upload request.
	 * 
	 * By default uploaded files are publicly readable.
	 * 
	 * @param initRequest
	 *            S3 request object for the file to be uploaded
	 */
	protected void configureInitiateRequest(
			InitiateMultipartUploadRequest initRequest) {
		initRequest.setCannedACL(CannedAccessControlList.PublicRead);
	}

	public void setPrefs(SharedPreferences prefs) {
		this.prefs = prefs;
	}

	public long getPartSize() {
		return partSize;
	}

	public void setPartSize(long partSize) {
		if (partSize < MIN_DEFAULT_PART_SIZE) {
			throw new IllegalStateException(
					"Part size is less than S3 minimum of "
							+ MIN_DEFAULT_PART_SIZE);
		} else {
			this.partSize = partSize;
		}
	}

	public void setProgressListener(UQResponseListener progressListener) {
		this.queueListener = progressListener;
	}
	
}
