package com.jayjaewonyoo.jy476.bike_eyetap;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

public class SettingsScreen extends Activity implements AdapterView.OnItemClickListener{

    SharedPreferences.Editor editor;

    Handler handler = new Handler();

    private BluetoothList bluetoothList;
    ListView listViewBluetoothDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_settings);

        editor = MapsActivity.sharedPreferences.edit();

        CheckBox accelerationCheckbox = (CheckBox) findViewById(R.id.accelerationCheckbox);
        CheckBox currSpeedCheckbox = (CheckBox) findViewById(R.id.currSpeedCheckbox);
        CheckBox maxSpeedCheckbox = (CheckBox) findViewById(R.id.maxSpeedCheckbox);
        CheckBox avgSpeedCheckbox = (CheckBox) findViewById(R.id.avgSpeedCheckbox);
        CheckBox timeCheckbox = (CheckBox) findViewById(R.id.timeCheckbox);
        CheckBox distanceCheckbox = (CheckBox) findViewById(R.id.distanceCheckbox);

        accelerationCheckbox.setChecked(MapsActivity.accelerationChecked);
        currSpeedCheckbox.setChecked(MapsActivity.currSpeedChecked);
        maxSpeedCheckbox.setChecked(MapsActivity.maxSpeedChecked);
        avgSpeedCheckbox.setChecked(MapsActivity.avgSpeedChecked);
        timeCheckbox.setChecked(MapsActivity.timeChecked);
        distanceCheckbox.setChecked(MapsActivity.distanceChecked);

        listViewBluetoothDevices = (ListView)findViewById(R.id.listViewDevices);
        listViewBluetoothDevices.setOnItemClickListener(SettingsScreen.this);

        handler.postDelayed(new Runnable() {
            public void run() {

                listViewBluetoothDevices.setAdapter(MapsActivity.bluetoothList);

                handler.postDelayed(this, 10);
            }
        }, 10);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public void settingsToMap(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        Intent getMapActivityIntent = new Intent(this, MapsActivity.class);
        getMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final int result = 1;
        startActivityForResult(getMapActivityIntent, result);
    }

    public void settingsToValues(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        Intent getValuesActivityIntent = new Intent(this, ValuesScreen.class);
        getValuesActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final int result = 1;
        startActivityForResult(getValuesActivityIntent, result);
    }

    public void onAccelerationClick(View view) {
        MapsActivity.accelerationChecked = !MapsActivity.accelerationChecked;
        editor.putBoolean("accelerationPreference", MapsActivity.accelerationChecked);
        editor.commit();
    }

    public void onCurrSpeedClick(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        MapsActivity.currSpeedChecked = !MapsActivity.currSpeedChecked;
        editor.putBoolean("currSpeedPreference", MapsActivity.currSpeedChecked);
        editor.commit();
    }

    public void onMaxSpeedClick(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        MapsActivity.maxSpeedChecked = !MapsActivity.maxSpeedChecked;
        editor.putBoolean("maxSpeedPreference", MapsActivity.maxSpeedChecked);
        editor.commit();
    }

    public void onAvgSpeedClick(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        MapsActivity.avgSpeedChecked = !MapsActivity.avgSpeedChecked;
        editor.putBoolean("avgSpeedPreference", MapsActivity.avgSpeedChecked);
        editor.commit();
    }

    public void onTimeClick(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        MapsActivity.timeChecked = !MapsActivity.timeChecked;
        editor.putBoolean("timePreference", MapsActivity.timeChecked);
        editor.commit();
    }

    public void onDistanceClick(View view) {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        MapsActivity.distanceChecked = !MapsActivity.distanceChecked;
        editor.putBoolean("distancePreference", MapsActivity.distanceChecked);
        editor.commit();
    }

    public void discoverBluetoothButton(View view) {
        //enableDiscoverable();
        discoverDevices();
    }

    public void enableDiscoverable() {
        MapsActivity.bluetoothSend = true;
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(MapsActivity.bluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(MapsActivity.bluetoothBroadcastReceiverEnableDiscoverable, intentFilter);
    }

    public void discoverDevices() {
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }
        verifyBluetoothPermissions();
        MapsActivity.bluetoothAdapter.startDiscovery();
    }

    private void verifyBluetoothPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }
            if(permissionCheck != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1000);
                }
            }
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Log.d("Select Pair", "Clicked.");
        if(MapsActivity.bluetoothAdapter.isDiscovering()) {
            MapsActivity.bluetoothAdapter.cancelDiscovery();
        }

        MapsActivity.bluetoothBonded = true;
        MapsActivity.bluetoothDevices.get(i).createBond();
        editor.putString("bluetoothAddressPreference", MapsActivity.bluetoothDevices.get(i).getAddress());
        editor.commit();

        /*if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
            MapsActivity.bluetoothDevices.get(i).createBond();
        }*/
    }
}
