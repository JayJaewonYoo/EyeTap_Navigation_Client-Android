package com.jayjaewonyoo.jy476.bike_eyetap;

import android.Manifest;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.Socket;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static com.jayjaewonyoo.jy476.bike_eyetap.App.CHANNEL_ID;

public class CalculationService extends Service implements SensorEventListener {

    public static int interval;

    public static double[] acceleration = new double[3];
    private static double[] tempAcceleration = new double[3];
    public static double[] currSpeed = new double[3];
    public static double maxSpeed;
    public static double distance;
    public static double avgSpeed;
    public static double time;
    public static boolean gravityCheck;

    public static boolean runningService;

    private static final String TAG = "CalculationService";
    public static final String BROADCAST_ACTION = "com.jayjaewonyoo.jy476.bike_eyetap.displayCalculation";
    private final Handler handler = new Handler();
    Intent intent;

    private SensorManager sensorManager;

    public Location currentLocation;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    //public ArrayList<LatLng> mapPoints = new ArrayList<LatLng>();
    //private PowerManager.WakeLock wakeLock;
    public static PolylineOptions mapLine = null;
    public static List<LatLng> directionPoints = null;
    LatLng tempLatLng;
    String angleString;
    String distanceMapString;

    Socket socket;
    DataOutputStream dataOutputStream;

    @Override
    public void onCreate() {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);
        /*PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");*/


        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocationResult(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        runningService = false;
        gravityCheck = false;
        interval = 10; // ms

        // Accelerometer initialization:
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); // Sensor manager.
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL); // Listener.
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY), SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MapsActivity.class);
        //notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("EyeTap: Bike").setContentText("EyeTap: Bike in progress.").setSmallIcon(R.drawable.android_icon).setContentIntent(pendingIntent).build();

        startForeground(1, notification);

        for (int i = 0; i < 3; i++) {
            acceleration[i] = 0;
            currSpeed[i] = 0;
        }
        maxSpeed = 0;
        distance = 0;
        avgSpeed = 0;
        time = 0;
        tempLatLng = null;
        angleString = "";
        distanceMapString = "";

        //runningService = !runningService;
        if(runningService) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            runningService = !runningService;
        } else {
            requestLocationUpdates();
            directionPoints = MapsActivity.directionsLine.getPoints();
            runningService = !runningService;
        }
        mapLine = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);

        /*if(!running && !mapPoints.isEmpty()) {
            mapPoints = new ArrayList<LatLng>();
        }*/

        handler.removeCallbacks(sendToUI);
        handler.postDelayed(sendToUI, interval);

        return Service.START_STICKY;
    }

    private Runnable sendToUI = new Runnable() {
        public void run() {
            displayInfo();
            handler.postDelayed(this, interval);
        }
    };

    private void displayInfo() {
        if (runningService) {
            // Calculations:
            for (int i = 0; i < 3; i++) {
                currSpeed[i] = acceleration[i] * interval/1000;
            }

            double accelerationMagnitude = magnitude(acceleration);
            double currSpeedMagnitude = magnitude(currSpeed);
            if (currSpeedMagnitude > maxSpeed) {
                maxSpeed = currSpeedMagnitude;
            }
            if (currSpeedMagnitude > (0.001 * interval)) // Change to 0.01 for interval = 10
                distance += currSpeedMagnitude + (0.5 * (interval/1000) * (interval/1000) * accelerationMagnitude);

            if(avgSpeed == 0) {
                avgSpeed = currSpeedMagnitude;
            } else {
                if(currSpeedMagnitude > 0.01) {
                    avgSpeed = (avgSpeed + currSpeedMagnitude) / 2;
                }
            }

            time += 0.01;

            // Testing:
            DecimalFormat df = new DecimalFormat("0.00");
            String stringAccelerationMagnitude = df.format(accelerationMagnitude);
            String stringCurrSpeedMagnitude = df.format(currSpeedMagnitude);
            String stringMaxSpeed = df.format(maxSpeed);
            String stringDistance = df.format(distance);
            String stringAvgSpeed = df.format(avgSpeed);
            String stringTime = df.format(time);

            //intent.putExtra("maxSpeed", String.valueOf(mapLine.getPoints().size() + 1));

            intent.putExtra("acceleration", stringAccelerationMagnitude);
            intent.putExtra("currSpeed", stringCurrSpeedMagnitude);
            intent.putExtra("maxSpeed", stringMaxSpeed);
            intent.putExtra("distance", stringDistance);
            intent.putExtra("avgSpeed", stringAvgSpeed);
            intent.putExtra("time", stringTime);

            // Sending data:
            /*intent.putExtra("acceleration", String.valueOf(accelerationMagnitude));
            intent.putExtra("currSpeed", String.valueOf(currSpeedMagnitude));
            intent.putExtra("maxSpeed", String.valueOf(maxSpeed));
            intent.putExtra("distance", String.valueOf(distance));*/
            /*intent.putExtra("currSpeed", String.valueOf(acceleration[0]));
            intent.putExtra("maxSpeed", String.valueOf(acceleration[1]));
            intent.putExtra("distance", String.valueOf(acceleration[2]));*/



            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    super.onLocationResult(locationResult);
                    onNewLocationResult(locationResult.getLastLocation());
                }
            };
            getLastLocation();

            intent.putExtra("latitude", currentLocation.getLatitude());
            intent.putExtra("longitude", currentLocation.getLongitude());

            LatLng currLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            if(MapsActivity.locationPermissionGranted) {
                //mapLine = new PolylineOptions().addAll(mapLine.getPoints()).add(currLatLng).width(5).color(Color.BLUE).geodesic(true); // Uncomment this one.
                //MapsActivity.mMap.addPolyline(new PolylineOptions().add(tempLatLng).add(currLatLng).width(5).color(Color.BLUE).geodesic(true)); // Uncomment this one.
                mapLine = new PolylineOptions().add(tempLatLng).add(currLatLng).width(5).color(Color.BLUE).geodesic(true);
            }
            MapsActivity.mapLine = mapLine; // Uncomment this one.

            /*PolylineOptions mapLine = new PolylineOptions().add(tempLatLng).add(currLatLng).width(5).color(Color.BLUE).geodesic(true);
            MapsActivity.mMap.addPolyline(mapLine);*/


            String angleDirection = "";


            if(!directionPoints.isEmpty()) {
                double currDistanceDifference = latLngDistance(currLatLng, directionPoints.get(0));
                if (currDistanceDifference <= 15) {
                    directionPoints.remove(0);
                } else {
                    if (directionPoints.size() == 0) {
                        angleString = "Reached";
                        distanceMapString = "0.00";
                    } else if(currDistanceDifference > 70) {
                        angleString = "Recalculating.";
                        distanceMapString = "Recalculating.";
                    } else {
                        if (directionPoints.size() == 0) {
                            angleString = "Reached";
                            distanceMapString = "0.00";
                        } else{
                            currDistanceDifference = latLngDistance(currLatLng, directionPoints.get(0));

                            if(currLatLng != tempLatLng) {
                                double[] currDirection = new double[2];
                                if (tempLatLng != null) {
                                    currDirection[0] = currLatLng.longitude - tempLatLng.longitude;
                                    currDirection[1] = currLatLng.latitude - tempLatLng.latitude;
                                } else {
                                    currDirection[0] = currLatLng.longitude;
                                    currDirection[1] = currLatLng.latitude;
                                }
                                double[] desiredDirection = {directionPoints.get(0).longitude - currLatLng.longitude, directionPoints.get(0).latitude - currLatLng.latitude};
                        /*double currDirectionMagnitude = magnitude(currDirection);
                        double desiredDirectionMagnitude = magnitude(desiredDirection);
                        for(int i = 0; i < 2; i++) {
                            currDirection[i] /= currDirectionMagnitude;
                            desiredDirection[i] /= desiredDirectionMagnitude;
                        }*/

                                // Positive result from angle means turn to the left, negative result means turn to the right, 0 means forward, PI means backwards.
                                double angle = Math.atan2((currDirection[0] * desiredDirection[1]) - (currDirection[1] * desiredDirection[0]), (currDirection[0] * desiredDirection[0]) + (currDirection[1] * desiredDirection[1]));
                                if(angle != 0 && angle != Math.PI) {
                                    angle *= (180/Math.PI);
                                    angleString = String.valueOf(angle);
                                    // Angle > 0 means turn right, angle < 0 means turn left.
                                }
                            }

                            distanceMapString = String.valueOf(currDistanceDifference);
                        }
                    }
                }
            } else {
                angleString = "Reached";
                distanceMapString = "0.00";
            }

            intent.putExtra("map angle", angleString);
            intent.putExtra("map distance", distanceMapString);

            tempLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

            sendBroadcast(intent);
        }
    }

    private double magnitude(double[] vector) {
        //double res = Math.pow((Math.pow(vector[0], 2) + Math.pow(vector[1], 2) + Math.pow(vector[2], 2)), 0.5);
        double res = 0;
        for(int i = 0; i < vector.length; i++) {
            res += Math.pow(vector[i], 2);
        }
        res = Math.pow(res, 0.5);
        return res;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER && !gravityCheck) {
            for (int i = 0; i < 3; i++) {
                tempAcceleration[i] = event.values[i];
            }
            gravityCheck = true;
        }
        if (sensor.getType() == Sensor.TYPE_GRAVITY && gravityCheck) {
            for (int i = 0; i < 3; i++) {
                acceleration[i] = tempAcceleration[i] - event.values[i];
            }
            gravityCheck = false;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void getLastLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (MapsActivity.locationPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            currentLocation = (Location) task.getResult();
                        }
                    }
                });
            }
        } catch (SecurityException e) {

        }
    }

    private void onNewLocationResult(Location location) {
        currentLocation = location;
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(interval);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    private double latLngDistance(LatLng currLatLng, LatLng destinationLatLng) {
        // All units in meters.
        // 6371 is Earth's radius.
        // Return true if currLatLng is within maximum radius.
        double latDifference = Math.toRadians(destinationLatLng.latitude - currLatLng.latitude);
        double lngDifference = Math.toRadians(destinationLatLng.longitude - currLatLng.longitude);
        double res = Math.pow(Math.sin(latDifference / 2), 2);
        res += Math.cos(Math.toRadians(currLatLng.latitude)) * Math.cos(Math.toRadians(destinationLatLng.latitude)) * Math.sin(lngDifference / 2) * Math.sin(lngDifference / 2);
        res = 2 * Math.atan2(Math.sqrt(res), Math.sqrt(1 - res));
        res *= 6371000;
        if(res < 0) {
            res *= -1;
        }

        return res;
    }
}
