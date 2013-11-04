package com.confiz.uploadqueue.model;

import java.util.ArrayList;

public class UQQueue extends ArrayList<UQRequest> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8795974568256285345L;
	
	private static UQQueue uploadingQueue = null;

	private UQQueue() {
	}

	public static UQQueue getInstance() {
		if (uploadingQueue == null) {
			uploadingQueue = new UQQueue();
		}
		return uploadingQueue;
	}
}
