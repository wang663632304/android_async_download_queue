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
    public static final int KEY = 5;

    public TextView titleText;
    public ProgressBar progressBar;
    public TextView speedText;
    public Button deleteButton;

    private long startTime;
    private long totalSize;
    private long startPercentage;


    private boolean hasInitiated = false;

    public ViewHolder(View parentView, long totalSize) {
        if (parentView != null) {
            titleText = (TextView) parentView.findViewById(R.id.title);
            speedText = (TextView) parentView.findViewById(R.id.speed);
            progressBar = (ProgressBar) parentView
                    .findViewById(R.id.progress_bar);
            deleteButton = (Button) parentView.findViewById(R.id.btn_delete);
            hasInitiated = true;
            startTime = System.currentTimeMillis();
            this.totalSize = totalSize;
        }
    }

    public ViewHolder(View parentView) {
        if (parentView != null) {
            titleText = (TextView) parentView.findViewById(R.id.title);
            speedText = (TextView) parentView.findViewById(R.id.speed);
            progressBar = (ProgressBar) parentView
                    .findViewById(R.id.progress_bar);
            deleteButton = (Button) parentView.findViewById(R.id.btn_delete);
            hasInitiated = true;
            startTime = System.currentTimeMillis();
        }
    }

    public HashMap<Integer, String> getItemDataMap(String title, String progress, String isPaused, String key) {
        HashMap<Integer, String> item = new HashMap<Integer, String>();
        item.put(KEY_TITLE, title);
        item.put(KEY_SPEED, calculateSpeed(Integer.parseInt(progress)));
        item.put(KEY_PROGRESS, progress);
        item.put(KEY_IS_PAUSED, isPaused);
        item.put(KEY, key);
        return item;
    }

    public void setData(String title, String key, String progress) {
        setData(title, key, progress, false);
    }

    public void setData(String title, String key, String progress,
                        Boolean isPaused) {
        if (hasInitiated) {
            HashMap<Integer, String> item = getItemDataMap(title,
                    progress, isPaused.toString(),key);

            titleText.setText(title);
            speedText.setText(calculateSpeed(Integer.parseInt(progress)));
            if (TextUtils.isEmpty(progress)) {
                progressBar.setProgress(0);
            } else {
                progressBar
                        .setProgress(Integer.parseInt(item.get(KEY_PROGRESS)));
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

    private String calculateSpeed(int progress) {
        double duration = System.currentTimeMillis() - startTime;
        this.startTime = System.currentTimeMillis();
        duration /= 1000;
        double durationData = (progress - startPercentage) * totalSize;
        durationData /= 1024;
        durationData /= 1024;
        this.startPercentage = progress;
        Double speed = durationData / duration;
        return speed.intValue() + "";

    }

    private void onPause() {

    }
}
