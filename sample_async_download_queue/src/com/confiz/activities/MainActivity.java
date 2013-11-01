package com.confiz.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import com.confiz.Utils.DownloadUtils;
import com.confiz.adapters.DownloadListAdapter;
import com.confiz.downloadqueue.DQManager;
import com.confiz.downloadqueue.DQResponseHolder;
import com.confiz.downloadqueue.model.DQRequest;
import com.confiz.listeners.DownloadProgressListener;
import com.confiz.model.ViewHolder;
import com.example.test.R;

public class MainActivity extends Activity {
    private static final String DOWNLOAD_MANAGER = "download_manager";
    private BroadcastReceiver broadcastReceiver;
    private Context context;
    private long totalSize;
    private ListView downloadList;
    private TextView queueText;

    private DownloadListAdapter downloadListAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        context = this;
        registerBroadCastReceiver();

        downloadList = (ListView) findViewById(R.id.download_list);
        downloadListAdapter = new DownloadListAdapter(this);
        downloadList.setAdapter(downloadListAdapter);


        queueText = (TextView) findViewById(R.id.queueText);
        downloadListAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                if (downloadListAdapter.getCount() < 1) {
                    queueText.setVisibility(View.VISIBLE);
                } else {
                    queueText.setVisibility(View.GONE);
                }
            }
        });
        downloadListAdapter.notifyDataSetChanged(); //for first call to datasetobserver




    }

    public void startDownload(View view) {


        String url = "http://songs.pakheaven.com/paki2k/Vital%20Signs%20-%20Aitebar%20(Vol%20III)%20(1993)/12%20Dil%20Dil%20Pakistan.mp3";
        DQManager dqManager = DQManager.getInstance(this.getApplicationContext());
        DownloadUtils.createDirectory(context);
        String sdCardPath = DownloadUtils.getDownloadDirectory(this);

        String fileName = "song" + System.currentTimeMillis() + ".mp3";

        DQRequest dqRequest = new DQRequest(fileName, -1, fileName, false, fileName,
                sdCardPath + fileName, sdCardPath, url);

        dqManager.addToQueue(dqRequest, this);
        DownloadProgressListener downloadProgressListener = new DownloadProgressListener(this);
        DQResponseHolder.getInstance().addListener(downloadProgressListener);

        downloadListAdapter.addItem(url, fileName, false, fileName);


    }


    private void registerBroadCastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String event = intent.getStringExtra("event");
                Log.i(this.getClass().getSimpleName(), "event received: " + event);
                if (event.equals("onDownloadStart")) {
                    totalSize = intent.getIntExtra("totalSize", 0);

                } else if (event.equals("onComplete")) {
                    String key = intent.getStringExtra("key");
                    downloadListAdapter.removeItem(key);
                } else {
                    String key = intent.getStringExtra("key");
                    View taskListItem = downloadList.findViewWithTag(key);
                    ViewHolder viewHolder = new ViewHolder(taskListItem,totalSize);

                    int progress = intent.getIntExtra("progress", 0);

                    viewHolder.setData(key, key, Integer.toString(progress));


                }
            }
        };
        IntentFilter intentToReceiveFilter = new IntentFilter();
        intentToReceiveFilter.addAction(DOWNLOAD_MANAGER);
        registerReceiver(broadcastReceiver, intentToReceiveFilter);
    }



    @Override
    protected void onDestroy() {
        unregisterReceiver(broadcastReceiver);
        super.onDestroy();
    }
}
