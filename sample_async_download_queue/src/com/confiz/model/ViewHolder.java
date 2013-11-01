package com.confiz.model;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.example.test.R;

import java.util.HashMap;

/**
 * Author: raheel.arif@confiz.com
 * Date: 10/31/13
 */
public class ViewHolder {

    public static final int KEY_URL = 0;
    public static final int KEY_SPEED = 1;
    public static final int KEY_PROGRESS = 2;
    public static final int KEY_IS_PAUSED = 3;
    public static final int KEY_TITLE = 4;
    public static final int ESTIMATED_TIME = 5;
    public static final int KEY = 5;

    public TextView titleText;
    public TextView estimatedTime;
    public ProgressBar progressBar;
    public TextView speedText;
    public Button deleteButton;
    private boolean hasInitiated = false;

    public ViewHolder(View parentView) {
        if (parentView != null) {
            titleText = (TextView) parentView.findViewById(R.id.title);
            speedText = (TextView) parentView.findViewById(R.id.speed);
            estimatedTime = (TextView) parentView.findViewById(R.id.estimated_time);
            progressBar = (ProgressBar) parentView
                    .findViewById(R.id.progress_bar);
            deleteButton = (Button) parentView.findViewById(R.id.btn_delete);
            hasInitiated = true;
        }
    }

    public void setData(String title, String progress, String speed, String estimatedTime) {
        if (hasInitiated) {

            titleText.setText(title);
            speedText.setText(speed);
            this.estimatedTime.setText(estimatedTime);
            if (TextUtils.isEmpty(progress)) {
                progressBar.setProgress(0);
            } else {
                progressBar.setProgress(Integer.parseInt(progress));
            }

        }
    }

    public static HashMap<Integer, String> getItemDataMap(String url, String title,
                                                          String speed, String progress, String isPaused, String key) {
        HashMap<Integer, String> item = new HashMap<Integer, String>();
        item.put(KEY_URL, url);
        item.put(KEY_TITLE, title);
        item.put(KEY_SPEED, speed);
        item.put(KEY_PROGRESS, progress);
        item.put(KEY_IS_PAUSED, isPaused);
        item.put(KEY, key);
        return item;
    }

    public void setData(HashMap<Integer, String> item) {
        if (hasInitiated) {
            speedText.setText(item.get(KEY_SPEED));
            String progress = item.get(KEY_PROGRESS);
            if (TextUtils.isEmpty(progress)) {
                progressBar.setProgress(0);
            } else {
                progressBar.setProgress(Integer.parseInt(progress));
            }
            if (Boolean.parseBoolean(item.get(KEY_IS_PAUSED))) {
                onPause();
            }
        }
    }


    private void onPause() {

    }
}
