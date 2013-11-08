package com.confiz.samples.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import com.confiz.samples.R;
import com.confiz.samples.model.ViewHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author: raheel.arif@confiz.com
 * Date: 10/31/13
 */
public class DownloadListAdapter extends BaseAdapter {

    private final Context context;
    private List<HashMap<Integer, String>> dataList;

    public DownloadListAdapter(Context context) {
        this.context = context;
        dataList = new ArrayList<HashMap<Integer, String>>();
    }

    @Override
    public int getCount() {
        return dataList.size();
    }

    @Override
    public Object getItem(int position) {
        return dataList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public void addItem(String url, String title, boolean isPaused, String key) {
        HashMap<Integer, String> item = ViewHolder.getItemDataMap(url, title, null,
                null, isPaused + "", key);
        dataList.add(item);
        this.notifyDataSetChanged();
    }

    public void removeItem(String key) {
        String tmp;
        for (int i = 0; i < dataList.size(); i++) {
            tmp = dataList.get(i).get(ViewHolder.KEY);
            if (tmp.equals(key)) {
                dataList.remove(i);
                this.notifyDataSetChanged();
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(
                    R.layout.downloading_list_item, null);
        }

        HashMap<Integer, String> itemData = dataList.get(position);
        String key = itemData.get(ViewHolder.KEY);
        convertView.setTag(key);

        ViewHolder viewHolder = new ViewHolder(convertView, context, key,this);

        viewHolder.setData(itemData);

        return convertView;
    }

}
