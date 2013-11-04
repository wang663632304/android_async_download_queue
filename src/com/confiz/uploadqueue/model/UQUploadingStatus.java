
package com.confiz.uploadqueue.model;

public enum UQUploadingStatus {

	DOWNLOADING("Uploading", "Uploading..."), PAUSED("Paused", "Paused"), WAITING("Waiting", "Queued"), FAILED(
	        "Failed", "Failed"), PAUSED_REQUEST("Paused_request", "Pausing"), DOWNLOAD_REQUEST(
	        "Upload_request", "Uploading..."), DELETE_REQUEST("Delete_request", "Deleting..."), DELETED(
	        "Deleted", "Deleted"), COMPLETED("Completed", "Saved"), MAX_TIRES_DONE("Falied",
	        "Max tries exceded. Please delete this file and try again later"), SIZE_OVERLOADED(
	        "Limit exceed", "Size limit exceeded");


	private String status = null;

	private String message = null;


	UQUploadingStatus(String status, String message) {

		this.status = status;
		this.message = message;
	}


	public String value() {

		return status;
	}


	public String message() {

		return message;
	}


	public static UQUploadingStatus get(int i) {

		return values()[i];
	}
}
