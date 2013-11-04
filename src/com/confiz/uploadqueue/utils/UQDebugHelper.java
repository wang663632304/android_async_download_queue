/*
 * Property : Confiz Solutions
 * Created by : Arslan Anwar
 * Updated by : Arslan Anwar
 */

package com.confiz.uploadqueue.utils;

import android.content.Context;
import android.util.Log;


/**
 * The Class UQDebugHelper.
 */
public class UQDebugHelper {


	/** The mode debug. */
	private static boolean MODE_DEBUG = true;

	/** The Constant TAG. */
	private static final String TAG = "UQDebugHelper";


	/**
	 * Prints the and track exception.
	 * 
	 * @param TAG
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackError(String TAG, Error exception) {

		UQDebugHelper.printAndTrackException(null, TAG, exception);
	}


	/**
	 * Prints the and track error.
	 * 
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackError(Error exception) {

		UQDebugHelper.printAndTrackException(null, UQDebugHelper.TAG, exception);
	}


	/**
	 * Prints the and track exception.
	 * 
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackException(Exception exception) {

		UQDebugHelper.printAndTrackException(null, UQDebugHelper.TAG, exception);
	}


	/**
	 * Prints the and track exception.
	 * 
	 * @param TAG
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackException(String TAG, Exception exception) {

		UQDebugHelper.printAndTrackException(null, TAG, exception);
	}


	/**
	 * Prints the and track exception.
	 * 
	 * @param context
	 *            the context
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackException(Context context, Exception exception) {

		UQDebugHelper.printAndTrackException(context, UQDebugHelper.TAG, exception);
	}


	/**
	 * Prints the and track exception.
	 * 
	 * @param context
	 *            the context
	 * @param TAG
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackException(Context context, String TAG, Error exception) {

		if (UQDebugHelper.MODE_DEBUG == true) {
			UQDebugHelper.printData(TAG, "Exception = " + exception.toString());
			exception.printStackTrace();
		}
	}


	/**
	 * Prints the and track exception.
	 * 
	 * @param context
	 *            the context
	 * @param TAG
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printAndTrackException(Context context, String TAG, Exception exception) {

		if (UQDebugHelper.MODE_DEBUG == true) {
			exception.printStackTrace();
			UQDebugHelper.printData(TAG, "Exception = " + exception.toString());

		}
	}


	/**
	 * Prints the data.
	 * 
	 * @param tag
	 *            the tag
	 * @param data
	 *            the data
	 */
	public static void printData(String tag, String data) {

		if (UQDebugHelper.MODE_DEBUG == true) {
			Log.e("" + tag, "UQDebugHelper = " + data);
		}
	}


	/**
	 * Prints the data.
	 * 
	 * @param data
	 *            the data
	 */
	public static void printData(String data) {

		if (UQDebugHelper.MODE_DEBUG == true) {
			Log.e(UQDebugHelper.TAG, "UQDebugHelper = " + data);
		}
	}


	/**
	 * Prints the exception.
	 * 
	 * @param tag
	 *            the tag
	 * @param string
	 *            the string
	 */
	public static void printException(String tag, String string) {

		UQDebugHelper.printData(tag, string);
	}


	/**
	 * Prints the exception.
	 * 
	 * @param tag
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printException(String tag, Exception exception) {

		UQDebugHelper.printException(null, tag, exception);
	}


	/**
	 * Prints the exception.
	 * 
	 * @param exception
	 *            the exception
	 */
	public static void printException(Exception exception) {

		UQDebugHelper.printException(null, UQDebugHelper.TAG, exception);
	}


	/**
	 * Prints the exception.
	 * 
	 * @param context
	 *            the context
	 * @param exception
	 *            the exception
	 */
	public static void printException(Context context, Exception exception) {

		UQDebugHelper.printException(context, UQDebugHelper.TAG, exception);
	}


	/**
	 * Prints the exception.
	 * 
	 * @param context
	 *            the context
	 * @param TAG
	 *            the tag
	 * @param exception
	 *            the exception
	 */
	public static void printException(Context context, String TAG, Exception exception) {

		if (UQDebugHelper.MODE_DEBUG == true) {
			exception.printStackTrace();
			UQDebugHelper.printData(TAG, "Exception = " + exception.toString());
		}
	}


	/**
	 * Prints the data.
	 * 
	 * @param tAG2
	 *            the t a g2
	 * @param string
	 *            the string
	 * @param exception
	 *            the exception
	 */
	public static void printData(String tAG2, String string, Exception exception) {

		UQDebugHelper.printException(null, tAG2, exception);
	}
}
