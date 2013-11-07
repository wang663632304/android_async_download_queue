
package com.confiz.uploadqueue;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.confiz.uploadqueue.db.UQDBAdapter;
import com.confiz.uploadqueue.model.UQActions;
import com.confiz.uploadqueue.utils.UQDebugHelper;
import com.confiz.uploadqueue.utils.UQUtilityNetwork;

public class UQNetworkManger extends BroadcastReceiver {


	private final String TAG = "UQNetworkManger";


	@Override
	public void onReceive(Context context, Intent intent) {

		UQDebugHelper.printData(TAG, "Receiver receive notification");

		boolean canConnect = UQManager.isAutoStartOnNetworkConnect(context);

		if (canConnect) {
			String userId = "-1";// get current user id to support multiple user
			// queue;
			if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
				if (intent.getAction().equals(android.net.ConnectivityManager.CONNECTIVITY_ACTION)) {
					boolean noConnectivity = intent
					        .getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);

					if (noConnectivity) {
						Log.i("Network", "Network is now DISCONNECTED");
					} else {
						Log.i("Network", "Network is now connected");
						boolean onlyOnWifi = UQManager.isUploadOnlyOnWifi(context);
						if (onlyOnWifi) {
							if (UQUtilityNetwork.isConnectedToWifi(context)) {
								startUploadQueue(context.getApplicationContext(), userId);
							}
						} else {
							startUploadQueue(context.getApplicationContext(), userId);
						}
					}
				}
			}
		}
	}


	private void startUploadQueue(Context context, String userId) {

		if (context != null) {
			boolean flag = UQDBAdapter.getInstance(context).isItemAvilableForUpload(userId);
			if (flag == true) {
				UQManager.getInstance(context);
				UQManager.startUQService(context, UQActions.START_DOWNLOAD);
			}
		}
	}
}
