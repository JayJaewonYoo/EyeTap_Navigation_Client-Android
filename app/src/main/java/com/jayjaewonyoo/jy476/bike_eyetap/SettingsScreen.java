package com.jayjaewonyoo.jy476.bike_eyetap;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

public class SettingsScreen extends Activity {

    SharedPreferences.Editor editor;

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
    }

    public void settingsToMap(View view) {
        Intent getMapActivityIntent = new Intent(this, MapsActivity.class);
        getMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final int result = 1;
        startActivityForResult(getMapActivityIntent, result);
    }

    public void settingsToValues(View view) {
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
        MapsActivity.currSpeedChecked = !MapsActivity.currSpeedChecked;
        editor.putBoolean("currSpeedPreference", MapsActivity.currSpeedChecked);
        editor.commit();
    }

    public void onMaxSpeedClick(View view) {
        MapsActivity.maxSpeedChecked = !MapsActivity.maxSpeedChecked;
        editor.putBoolean("maxSpeedPreference", MapsActivity.maxSpeedChecked);
        editor.commit();
    }

    public void onAvgSpeedClick(View view) {
        MapsActivity.avgSpeedChecked = !MapsActivity.avgSpeedChecked;
        editor.putBoolean("avgSpeedPreference", MapsActivity.avgSpeedChecked);
        editor.commit();
    }

    public void onTimeClick(View view) {
        MapsActivity.timeChecked = !MapsActivity.timeChecked;
        editor.putBoolean("timePreference", MapsActivity.timeChecked);
        editor.commit();
    }

    public void onDistanceClick(View view) {
        MapsActivity.distanceChecked = !MapsActivity.distanceChecked;
        editor.putBoolean("distancePreference", MapsActivity.distanceChecked);
        editor.commit();
    }

    public void enableDiscoverable(View view) {
        MapsActivity.bluetoothSend = true;
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        IntentFilter intentFilter = new IntentFilter(MapsActivity.bluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(MapsActivity.bluetoothBroadcastReceiver2, intentFilter);
    }
}
