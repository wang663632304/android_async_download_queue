package com.confiz.uploadqueue.model;

public enum UQActions {
	
	START_DOWNLOAD,UPDATE_DQ,STOP_DQ,PAUSE_ITEM,DELETE_ITEM,REMOVE_ITEM, START_DOWNLOAD_FROM_PAUSE;
	
	public static UQActions get(int i){
		return values()[i];
	}
}
