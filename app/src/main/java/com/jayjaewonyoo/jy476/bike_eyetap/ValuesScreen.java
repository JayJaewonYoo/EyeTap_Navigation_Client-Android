package com.jayjaewonyoo.jy476.bike_eyetap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class ValuesScreen extends Activity {

    Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_values);
        final TextView accelerationValue = findViewById(R.id.accelerationValue);
        final TextView currSpeedValue = findViewById(R.id.currentSpeedValue);
        final TextView maxSpeedValue = findViewById(R.id.maxSpeedValue);
        final TextView avgSpeedValue = findViewById(R.id.avgSpeedValue);
        final TextView timeValue = findViewById(R.id.timeValue);
        final TextView distanceValue = findViewById(R.id.distanceValue);
        final TextView angleValue = findViewById(R.id.angleValue);
        final TextView distanceToValue = findViewById(R.id.distanceToValue);

        handler.postDelayed(new Runnable() {
            public void run() {

            accelerationValue.setText(MapsActivity.acceleration);
            currSpeedValue.setText(MapsActivity.currSpeed);
            maxSpeedValue.setText(MapsActivity.maxSpeed);
            distanceValue.setText(MapsActivity.distance);
            avgSpeedValue.setText(MapsActivity.avgSpeed);
            timeValue.setText(MapsActivity.time);
            angleValue.setText(MapsActivity.mapAngle);
            distanceToValue.setText(MapsActivity.mapDistance);

            handler.postDelayed(this, 10);
            }
        }, 10);
    }

    public void valuesToMap(View view) {
        Intent getMapActivityIntent = new Intent(this, MapsActivity.class);
        getMapActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final int result = 1;
        startActivityForResult(getMapActivityIntent, result);
    }

    public void valuesToSettings(View view) {
        if(!MapsActivity.running) {
            Intent getSettingsActivityIntent = new Intent(this, SettingsScreen.class);
            getSettingsActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final int result = 1;
            startActivityForResult(getSettingsActivityIntent, result);
        } else {
            Toast.makeText(getApplicationContext(), "Press stop to see settings.", Toast.LENGTH_SHORT).show();
        }
    }
}
