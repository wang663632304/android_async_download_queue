
package com.confiz.uploadqueue.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class UQUtilityNetwork {


	private static String TAG = "UQUtilityNetwork";


	public static boolean isNetworkAvailable(Context context) {

		boolean available = false;
		try {

			ConnectivityManager connectivity = (ConnectivityManager) context
			        .getSystemService(Context.CONNECTIVITY_SERVICE);

			if (connectivity != null) {
				NetworkInfo[] info = connectivity.getAllNetworkInfo();
				if (info != null) {
					for (int i = 0; i < info.length; i++) {
						if (info[i].getState() == NetworkInfo.State.CONNECTED) {
							available = true;
						}
					}
				}
			}
			if (available == false) {
				NetworkInfo wiMax = connectivity.getNetworkInfo(6);

				if (wiMax != null && wiMax.isConnected()) {
					available = true;
				}
			}
		} catch (Exception e) {
			UQDebugHelper.printException(TAG, e);
		}

		return available;
	}


	public static boolean isConnectedToWifi(Context context) {

		try {
			ConnectivityManager connManager = (ConnectivityManager) context
			        .getSystemService(Context.CONNECTIVITY_SERVICE);
			NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

			if (mWifi.isConnected()) {
				return true;
			}
		} catch (Exception e) {
			UQDebugHelper.printException(TAG, e);
		}
		return false;
	}


	public static String getServerResponse(String urlRequest) {

		Log.d("urlRequest", urlRequest);
		String response = "";
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(urlRequest).openConnection();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			response = read(conn.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		Log.d("response", response);
		return response.trim();
	}


	private static String read(InputStream in) throws IOException {

		StringBuilder sb = new StringBuilder();
		BufferedReader r = new BufferedReader(new InputStreamReader(in), 1000);
		for (String line = r.readLine(); line != null; line = r.readLine()) {
			sb.append(line);
		}
		in.close();
		return sb.toString();
	}


	/**
	 * Gets the uploading estimates.
	 * 
	 * @param statTime
	 *            the stat time
	 * @param expectedBytes
	 *            the expected bytes
	 * @param bytesReceived
	 *            the bytes received
	 * @param reallTotal
	 *            the reall total
	 * @return the uploading estimates
	 */
	public static String[] getUploadingEstimates(long statTime, int expectedBytes, int bytesReceived,
	        long reallTotal) {

		final String facts[] = new String[5];
		try {
			String totalBytesStr;
			String bytesRecStr;
			String speedStr;
			String timeRemainingStr;

			if (bytesReceived < 1024) {
				bytesRecStr = String.format("%d B", bytesReceived);
			} else if (bytesReceived < 1024 * 1024) {
				final float value = (float) bytesReceived / 1024;
				bytesRecStr = String.format("%1.2f KB", value);
			} else {
				final float value = (float) bytesReceived / (1024 * 1024);
				bytesRecStr = String.format("%1.2f MB", value);
			}

			if (expectedBytes < 1024) {
				totalBytesStr = String.format("%d B", expectedBytes);
			} else if (expectedBytes < 1024 * 1024) {
				final float value = (float) expectedBytes / 1024;
				totalBytesStr = String.format("%1.2f KB", value);
			} else {
				final float value = (float) expectedBytes / (1024 * 1024);
				totalBytesStr = String.format("%1.2f MB", value);
			}

			long timeInterval = System.currentTimeMillis() - statTime;

			timeInterval = timeInterval / 1000;
			if (timeInterval == 0) {
				timeInterval = 1;
			}
			float speed = 0.0f;

			if (reallTotal > 0) {
				speed = reallTotal / timeInterval;
			} else {
				speed = bytesReceived / timeInterval;
			}

			if (speed < 0) {
				speed = speed * -1;
			}

			if (speed < 1024) {
				speedStr = String.format("%1.2f Bytes/s", speed);
			} else if (speed < 1024 * 1024) {
				final float value = speed / 1024;
				speedStr = String.format("%1.2f KB/s", value);
			} else {
				final float value = speed / (1024 * 1024);
				speedStr = String.format("%1.2f MB/s", value);
			}

			int timeRemaining = new Float((expectedBytes - bytesReceived) / speed).intValue();
			if (timeRemaining < 0) {
				timeRemaining = timeRemaining * -1;
			}

			if (timeRemaining < 60) {
				timeRemainingStr = String.format("%d seconds remaining", timeRemaining);
			} else if (timeRemaining < 3600) {
				final int seconds = (timeRemaining % 60);
				timeRemainingStr = String
				        .format("%d minutes, %d seconds remaining", timeRemaining / 60, seconds);
			} else {
				final int minutes = (timeRemaining / 60 % 60);
				timeRemainingStr = String
				        .format("%d hours, %d minutes remaining", timeRemaining / 3600, minutes);
			}

			facts[0] = bytesRecStr;
			facts[1] = totalBytesStr;
			facts[2] = speedStr;
			facts[3] = timeRemainingStr;
		} catch (final Exception exception) {
			UQDebugHelper.printAndTrackException(UQUtilityNetwork.TAG, exception);
		}
		return facts;
	}
}
