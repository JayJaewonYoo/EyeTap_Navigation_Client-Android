package com.jayjaewonyoo.jy476.bike_eyetap;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class BluetoothList extends ArrayAdapter<BluetoothDevice> {
    private LayoutInflater layoutInflater;
    private ArrayList<BluetoothDevice> devices;
    private int viewResourceID;

    public BluetoothList(Context context, int textViewResourceID, ArrayList<BluetoothDevice> inputDevices) {
        super(context, textViewResourceID, inputDevices);
        this.devices = inputDevices;
        layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewResourceID = textViewResourceID;
    }

    public View getView(int position, View res, ViewGroup parent) {
        res = layoutInflater.inflate(viewResourceID, null);
        BluetoothDevice bluetoothDevice = devices.get(position);
        if(bluetoothDevice != null) {
            TextView deviceName = (TextView)res.findViewById(R.id.textViewDeviceName);
            TextView deviceAddress = (TextView)res.findViewById(R.id.textViewDeviceAddress);

            if(deviceName != null) {
                deviceName.setText(bluetoothDevice.getName());
            }
            if(deviceAddress != null) {
                deviceAddress.setText(bluetoothDevice.getAddress());
            }
        }

        return res;
    }
}
