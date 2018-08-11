package com.jayjaewonyoo.jy476.bike_eyetap;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Path;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static android.net.NetworkCapabilities.TRANSPORT_CELLULAR;
import static android.net.NetworkCapabilities.TRANSPORT_WIFI;
import static com.jayjaewonyoo.jy476.bike_eyetap.CalculationService.BROADCAST_ACTION;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static GoogleMap mMap;
    private static final String fineLocation = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String coarseLocation = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static boolean locationPermissionGranted = false;
    private static final int locationPermissionRequestCode = 1234;
    private FusedLocationProviderClient fusedLocationProviderClient;
    public static final float standardZoom = 15f;
    public static LatLng currLatLng;
    //public List<LatLng> mapPoints;
    private PowerManager.WakeLock wakeLock;
    public static PolylineOptions mapLine;
    private boolean computingPath;

    public static String acceleration;
    public static String currSpeed;
    public static String maxSpeed;
    public static String distance;
    public static String avgSpeed;
    public static String time;
    public static String mapAngle;
    public static String mapDistance;

    //public LatLng prevLatLng;

    public static boolean running;

    private static final String TAG = "MapsActivity";
    private Intent intent;

    ArrayList<LatLng> directionsPath;
    public static PolylineOptions directionsLine;
    public boolean directionsShown;
    public LatLng destinationLatLng;

    public static SharedPreferences sharedPreferences;
    /* The following is the Shared Preferences Structure:
        accelerationPreference  : Whether the acceleration checkbox is checked.
        currSpeedPreference     : Whether the Current Speed checkbox is checked.
        maxSpeedPreference      : Whether the Max Speed checkbox is checked.
        avgSpeedPreference      : Whether the Average Speed checkbox is checked.
        timePreference          : Whether the Time checkbox is checked.
        distancePreference      : Whether the Distance Travelled checkbox is checked.
     */

    public static boolean accelerationChecked;
    public static boolean currSpeedChecked;
    public static boolean maxSpeedChecked;
    public static boolean avgSpeedChecked;
    public static boolean timeChecked;
    public static boolean distanceChecked;

    public static boolean bluetoothSend;
    BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver bluetoothBroadcastReceiver1 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, bluetoothAdapter.ERROR);

                switch(state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE);
        accelerationChecked = sharedPreferences.getBoolean("accelerationPreference", true);
        currSpeedChecked = sharedPreferences.getBoolean("currSpeedPreference", true);
        maxSpeedChecked = sharedPreferences.getBoolean("maxSpeedPreference", true);
        avgSpeedChecked = sharedPreferences.getBoolean("avgSpeedPreference", true);
        timeChecked = sharedPreferences.getBoolean("timePreference", true);
        distanceChecked = sharedPreferences.getBoolean("distancePreference", true);

        running = false;
        PowerManager powerManager = (PowerManager)getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakelockTag");
        mapLine = null;

        directionsPath = new ArrayList<>();
        directionsShown = false;
        computingPath = false;

        acceleration = "0.00";
        currSpeed = "0.00";
        maxSpeed = "0.00";
        distance = "0.00";
        avgSpeed = "0.00";
        time = "0.00";
        mapAngle = "Waiting";
        mapDistance = "Waiting";

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothSend = true;
        enableBluetooth();
        if(!bluetoothAdapter.isEnabled()) {
            bluetoothSend = false;
            Toast.makeText(getBaseContext(), "Cannot communicate with EyeTap until Bluetooth is enabled.", Toast.LENGTH_SHORT).show();
        } else {
            bluetoothSend = true;
        }

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        /*SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);*/

        getLocationPermission();

        intent = new Intent(this, CalculationService.class);

        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (running) {
                    button.setText(R.string.buttonStart);
                    running = false;
                    getCurrLocation();
                    placeMarker(currLatLng, getString(R.string.stopLocation));
                    //mMap.addPolyline(mapLine);
                    wakeLock.release();
                    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    directionsLine = null;
                    directionsShown = false;

                    startService(intent);
                    registerReceiver(broadcastReceiver, new IntentFilter(CalculationService.BROADCAST_ACTION));
                } else {
                    if(directionsShown) {
                        if(computingPath) {
                            Toast.makeText(getBaseContext(), "Currently computing path. Please do not press start.", Toast.LENGTH_SHORT).show();
                        } else {
                            if(!bluetoothAdapter.isEnabled()) {
                                bluetoothSend = false;
                                Toast.makeText(getBaseContext(), "Cannot communicate with EyeTap until Bluetooth is enabled.", Toast.LENGTH_SHORT).show();
                            } else {
                                bluetoothSend = true;
                            }

                            /*TextView accelerationValue = findViewById(R.id.accelerationValue);
                            TextView currSpeedValue = findViewById(R.id.currentSpeedValue);
                            TextView maxSpeedValue = findViewById(R.id.maxSpeedValue);
                            TextView distanceValue = findViewById(R.id.distanceValue);
                            accelerationValue.setText("0.00");
                            currSpeedValue.setText("0.00");
                            maxSpeedValue.setText("0.00");
                            distanceValue.setText("0.00");*/
                            acceleration = "0.00";
                            currSpeed = "0.00";
                            maxSpeed = "0.00";
                            distance = "0.00";
                            avgSpeed = "0.00";
                            time = "0.00";
                            mapAngle = "Waiting";
                            mapDistance = "Waiting";

                            button.setText(R.string.buttonStop);
                            running = true;
                            mMap.clear();
                            if (directionsLine != null) {
                                mMap.addPolyline(directionsLine);
                            }
                            getCurrLocation();
                            placeMarker(currLatLng, getString(R.string.startLocation));
                            wakeLock.acquire();
                            mapLine = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                            startService(intent);
                            registerReceiver(broadcastReceiver, new IntentFilter(CalculationService.BROADCAST_ACTION));
                        }
                    } else {
                        Toast.makeText(getBaseContext(), "Click destination on map to begin.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(bluetoothBroadcastReceiver1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        //reset();
    }

    private void getLocationPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(), fineLocation) == PackageManager.PERMISSION_GRANTED) {
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(), coarseLocation) == PackageManager.PERMISSION_GRANTED) {
                locationPermissionGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, locationPermissionRequestCode);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, locationPermissionRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch(requestCode) {
            case locationPermissionRequestCode: {
                if(grantResults.length > 0) {
                    for(int i = 0; i < grantResults.length; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = false;
                            return;
                        }
                    }
                    locationPermissionGranted = true;
                    initMap();
                }
            }
        }
    }

    private void initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        /*// Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        if(locationPermissionGranted) {
            getCurrLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationPermissionRequestCode);
                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
            Toast.makeText(this, "Click destination on map.", Toast.LENGTH_SHORT).show();

            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                @Override
                public void onMapClick(LatLng latLng) {
                    if(!running) {
                        if(isConnectedWifi()) {
                            computingPath = true;

                            if (directionsPath.size() == 2) {
                                directionsPath.clear();
                                mMap.clear();
                            }

                            destinationLatLng = latLng;

                            directionsPath.add(currLatLng);
                            directionsPath.add(latLng);

                            MarkerOptions markerOptionsStart = new MarkerOptions();
                            MarkerOptions markerOptionsEnd = new MarkerOptions();
                            markerOptionsStart.position(currLatLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                            markerOptionsEnd.position(latLng).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
                            mMap.addMarker(markerOptionsStart);
                            mMap.addMarker(markerOptionsEnd);

                            String url = getRequestUrl(directionsPath.get(0), directionsPath.get(1));
                            TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                            taskRequestDirections.execute(url);

                            directionsShown = true;
                        } else {
                            Toast.makeText(getBaseContext(), "Not connected to Wi-Fi. Please connect to begin.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        }
    }

    private String getRequestUrl(LatLng start, LatLng end) {
        String stringStart = "origin=" + start.latitude + "," + start.longitude;
        String stringEnd = "destination=" + end.latitude + "," + end.longitude;
        String stringSensor = "sensor=false";
        String stringMode = "mode=bicycling"; //mode=walking
        String param = stringStart + "&" + stringEnd + "&" + stringSensor + "&" + stringMode;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + param;
        return url;
    }

    private String requestDirections(String requestURL) throws IOException {
        String responseString = "";
        InputStream inputStream = null;
        HttpURLConnection httpURLConnection = null;
        try {
            URL url = new URL(requestURL);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.connect();

            inputStream = httpURLConnection.getInputStream();
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            StringBuffer stringBuffer = new StringBuffer();
            String line = "";
            while((line = bufferedReader.readLine()) != null) {
                stringBuffer.append(line);
            }

            responseString = stringBuffer.toString();
            bufferedReader.close();
            inputStreamReader.close();
        } catch(Exception e){

        } finally {
            if(inputStream != null) {
                inputStream.close();
            }
            httpURLConnection.disconnect();
        }
        return responseString;
    }

    public void mapToValues(View view) {
        Intent valuesActivityIntent = new Intent(this, ValuesScreen.class);
        valuesActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        final int result = 1;
        startActivityForResult(valuesActivityIntent, result);
    }

    public void mapToSettings(View view) {
        if(!running) {
            Intent settingsActivityIntent = new Intent(this, SettingsScreen.class);
            settingsActivityIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            final int result = 1;
            startActivityForResult(settingsActivityIntent, result);
        } else {
            Toast.makeText(getApplicationContext(), "Press stop to see settings.", Toast.LENGTH_SHORT).show();
        }
    }

    public class TaskRequestDirections extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... strings) {
            String responseString = "";
            try {
                responseString = requestDirections(strings[0]);
            } catch(IOException e) {
                e.printStackTrace();
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            TaskParser taskParser = new TaskParser();
            taskParser.execute(s);
        }
    }

    public class TaskParser extends AsyncTask<String, Void, List<List<HashMap<String, String>>> > {
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... strings) {
            JSONObject jsonObject = null;
            List<List<HashMap<String, String>>> routes = null;
            try {
                jsonObject = new JSONObject(strings[0]);
                DirectionsParser directionsParser = new DirectionsParser();
                routes = directionsParser.parse(jsonObject);

                Log.d("JSON Status", jsonObject.getString("status"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> lists) {
            ArrayList points = null;
            directionsLine = null;
            for(List<HashMap<String, String>> path: lists) {
                points = new ArrayList();
                directionsLine = new PolylineOptions();

                for(HashMap<String, String> point: path) {
                    double lat = Double.parseDouble(point.get("lat"));
                    double lon = Double.parseDouble(point.get("lon"));

                    points.add(new LatLng(lat, lon));
                }

                directionsLine.addAll(points).width(5).color(Color.RED).geodesic(true);
            }

            if(directionsLine != null) {
                mMap.addPolyline(directionsLine);
                Toast.makeText(getApplicationContext(), "App is now ready.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Direction not found.", Toast.LENGTH_SHORT).show();
            }
            computingPath = false;
        }
    }

    private void getCurrLocation() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if(locationPermissionGranted) {
                Task location = fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {

                            //LatLng tempLatLng = currLatLng;

                            Location currentLocation = (Location) task.getResult();
                            currLatLng = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            moveCamera(currLatLng, standardZoom);

                            /*if(running) {
                                PolylineOptions mapLine = new PolylineOptions().add(tempLatLng).add(currLatLng).width(5).color(Color.BLUE).geodesic(true);
                                mMap.addPolyline(mapLine);
                            }*/
                        } else {
                            Toast.makeText(MapsActivity.this, "Unable to get location.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch(SecurityException e) {

        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void placeMarker(LatLng latLng, String message) {
        MarkerOptions marker = new MarkerOptions().position(latLng).title(message);
        mMap.addMarker(marker);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateUI(intent);
        }
    };

    private void updateUI(Intent intent) {
        if(running) {
            acceleration = intent.getStringExtra("acceleration");
            currSpeed = intent.getStringExtra("currSpeed");
            maxSpeed = intent.getStringExtra("maxSpeed");
            distance = intent.getStringExtra("distance");
            avgSpeed = intent.getStringExtra("avgSpeed");
            time = intent.getStringExtra("time");

            /*TextView accelerationValue = findViewById(R.id.accelerationValue);
            TextView currSpeedValue = findViewById(R.id.currentSpeedValue);
            TextView maxSpeedValue = findViewById(R.id.maxSpeedValue);
            TextView distanceValue = findViewById(R.id.distanceValue);

            accelerationValue.setText(acceleration);
            currSpeedValue.setText(currSpeed);
            maxSpeedValue.setText(maxSpeed);
            distanceValue.setText(distance);*/

            if (locationPermissionGranted) {
                Bundle extras = intent.getExtras();

                //LatLng tempLatLng = currLatLng;

                currLatLng = new LatLng(extras.getDouble("latitude"), extras.getDouble("longitude"));
                moveCamera(currLatLng, standardZoom);

                //mapLine = new PolylineOptions().add(tempLatLng).add(currLatLng).width(5).color(Color.BLUE).geodesic(true);
                //mapLine = new PolylineOptions().addAll(mapLine.getPoints()).add(currLatLng).width(5).color(Color.BLUE).geodesic(true);
                //mMap.addPolyline(mapLine);

                if(mapLine != null) {
                    //mMap.addPolyline(mapLine); // Uncomment this one.
                    //CalculationService.mapLine = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
                }
            }

            mapAngle = intent.getStringExtra("map angle");
            mapDistance = intent.getStringExtra("map distance");
            if (mapAngle == "Recalculating.") {
                if(isConnectedWifi()) {
                    directionsPath.clear();
                    directionsPath.add(currLatLng);
                    directionsPath.add(destinationLatLng);
                    String url = getRequestUrl(directionsPath.get(0), directionsPath.get(1));
                    TaskRequestDirections taskRequestDirections = new TaskRequestDirections();
                    taskRequestDirections.execute(url);
                    CalculationService.directionPoints = directionsLine.getPoints();
                } else {
                    Toast.makeText(getBaseContext(), "No connection to Wi-Fi. Cannot recalculate route.", Toast.LENGTH_SHORT).show();
                }
            } else if (mapAngle == "Reached") {

            } else {
                // On path:

                // Testing, remove later:
                /*currSpeedValue.setText(mapDistance);
                maxSpeedValue.setText(mapAngle);*/
            }
        }
    }

    private boolean isConnectedWifi() {
        //boolean connected;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo currNetwork = connectivityManager.getActiveNetworkInfo();
        boolean connected = currNetwork != null && currNetwork.isConnected();

        return connected;
    }

    public void enableBluetooth() {
        if(bluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "This phone does not have Bluetooth capability. Cannot communicate with EyeTap.", Toast.LENGTH_SHORT).show();
            bluetoothSend = false;
        } else if(!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(enableBluetoothIntent);
            IntentFilter bluetoothIntent = new IntentFilter(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            registerReceiver(bluetoothBroadcastReceiver1, bluetoothIntent);
        }
    }

    public void reset() {
        /*TextView accelerationValue = findViewById(R.id.accelerationValue);
        TextView currSpeedValue = findViewById(R.id.currentSpeedValue);
        TextView maxSpeedValue = findViewById(R.id.maxSpeedValue);
        TextView distanceValue = findViewById(R.id.distanceValue);
        accelerationValue.setText("0.00");
        currSpeedValue.setText("0.00");
        maxSpeedValue.setText("0.00");
        distanceValue.setText("0.00");*/
        CalculationService.runningService = false;
        CalculationService.gravityCheck = false;

        for(int i = 0; i < 3; i++) {
            CalculationService.acceleration[i] = 0;
            CalculationService.currSpeed[i] = 0;
        }
        CalculationService.maxSpeed = 0;
        CalculationService.distance = 0;
        CalculationService.avgSpeed = 0;
        CalculationService.time = 0;
        //stopService(intent);
        mapLine = new PolylineOptions().width(5).color(Color.BLUE).geodesic(true);
        running = false;
        final Button button = findViewById(R.id.button);
        button.setText(R.string.buttonStart);
        directionsLine = null;
        directionsShown = false;
        mMap.clear();
    }
}
