package com.example.wghcwc.eeg_manager;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by wghcwc on 18-3-30.
 */

public class BTDeviceAdapter extends BaseAdapter {
    Context mContext;
    ArrayList<BluetoothDevice> mDeviceList;

    public BTDeviceAdapter(Context context,
                           ArrayList<BluetoothDevice> deviceArrayList) {
        mContext = context;
        mDeviceList = deviceArrayList;

    }

    @Override
    public int getCount() {
        return mDeviceList.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        if (view == null) {
            view = View.inflate(mContext, R.layout.dialog_text_layout, null);
        }
        TextView tv_devices = (TextView) view.findViewById(R.id.tv_devices);

        tv_devices.setText(mDeviceList.get(i).getName());
        return view;
    }
}
