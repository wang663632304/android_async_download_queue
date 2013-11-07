/*
 * Property : Confiz Solutions
 * Created by : Arslan Anwar
 * Updated by : Arslan Anwar
 */

package com.confiz.uploadqueue;

import java.util.ArrayList;

import com.confiz.uploadqueue.interfaces.UQResponseListener;
import com.confiz.uploadqueue.model.UQRequest;
import com.confiz.uploadqueue.utils.UQErrors;


/**
 * The listener interface for receiving dataSetChange events.
 * The class that is interested in processing a dataSetChange
 * event implements this interface, and the object created
 * with that class is registered with a component using the
 * component's <code>addDataSetChangeListener<code> method. When
 * the dataSetChange event occurs, that object's appropriate
 * method is invoked.
 * 
 * @see DataSetChangeEvent
 */
class UQResponseHolder {


	private static UQResponseHolder instance;

	/** The listener list. */
	private ArrayList<UQResponseListener> listenerList = null;


	/**
	 * Instantiates a new data set change listener.
	 */
	private UQResponseHolder() {

		listenerList = new ArrayList<UQResponseListener>();
	}


	public static UQResponseHolder getInstance() {

		if (instance == null) {
			instance = new UQResponseHolder();
		}
		return instance;
	}


	/**
	 * Adds the listener.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void addListener(UQResponseListener listener) {

		this.listenerList.add(listener);
	}


	/**
	 * Contain.
	 * 
	 * @param listener
	 *            the listener
	 * @return true, if successful
	 */
	public boolean contain(UQResponseListener listener) {

		return this.listenerList.contains(listener);
	}


	/**
	 * Replace.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void replace(UQResponseListener listener) {

		if (this.contain(listener)) {
			this.listenerList.remove(listener);
		}
		this.addListener(listener);
	}


	/**
	 * Removes the listener.
	 * 
	 * @param listener
	 *            the listener
	 */
	public void removeListener(UQResponseListener listener) {

		this.listenerList.remove(listener);
	}


	/**
	 * Removes the all listener.
	 */
	public void removeAllListener() {

		this.listenerList.clear();
	}


	/**
	 * Destory object.
	 */
	public void destoryObject() {

		this.removeAllListener();
		this.listenerList = null;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#onUploadStart(java.lang.String)
	 */

	public void onUploadStart(String key) {

		for (final UQResponseListener change : listenerList) {
			change.onUploadStart(key);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#onUploadStart(java.lang.String, int)
	 */

	public void onUploadStart(String key, int totalSize) {

		for (final UQResponseListener change : listenerList) {
			change.onUploadStart(key);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#updateProgress(java.lang.String, int)
	 */

	public void updateProgress(String key, int progress) {

		for (final UQResponseListener change : listenerList) {
			change.updateUploadingProgress(key, progress);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#onErrorOccurred(java.lang.String,
	 * com.confiz.uploadqueue.utils.UQErrors)
	 */

	public void onErrorOccurred(String key, UQErrors errorNo) {

		for (final UQResponseListener change : listenerList) {
			change.onUploadingFailer(key, errorNo);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#onComplete(java.lang.String)
	 */

	public void onComplete(String key) {

		for (final UQResponseListener change : listenerList) {
			change.onUploadingCompleted(key);
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#updateUploadingEstimates(java.lang.String,
	 * java.lang.String[])
	 */

	public void updateUploadingEstimates(String key, String[] details) {

		for (final UQResponseListener change : listenerList) {
			change.updateUploadingEstimates(key, details);
		}
	}


	/**
	 * On data updated.
	 */
	public void onDataUpdated() {

		for (final UQResponseListener change : listenerList) {
			change.onUploadingDataUpdated();
		}
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see com.confiz.uploadqueue.DQResponseListener#getUploadingRequester()
	 */

	public UQRequest getUploadingRequester() {

		return null;
	}


	/**
	 * @param uploadingRequest
	 */
	public void updateStatusOf(UQRequest uploadingRequest) {

		for (final UQResponseListener change : listenerList) {
			change.updateUploadingStatusOf(uploadingRequest);
		}
	}


	/**
	 * @param dRequest
	 */
	public void updateFileExistanceStatusInDB(UQRequest dRequest) {

		for (final UQResponseListener change : listenerList) {
			change.updateUploadingStatusInDB(dRequest);
		}
	}
}