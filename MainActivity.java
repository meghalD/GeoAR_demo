package com.meghal.yeppar.mygeoar;
import android.*;
import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    public static ArrayList<Point> props = new ArrayList<Point>();
    private static final String TAG = "Compass";
    private static boolean DEBUG = false;
    private Sensor mSensor;
    private DrawSurfaceView mDrawView;
    private GoogleMap map;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    LocationManager locMgr;
    static double lat;
    static double lng;
    private SensorManager mSensorManager;
    private final SensorEventListener mListener = new SensorEventListener(){
        public void onSensorChanged(SensorEvent event) {
            if (DEBUG)
                Log.d(TAG, "sensorChanged (" + event.values[0] + ", " + event.values[1] + ", " + event.values[2] + ")");
            if (mDrawView != null) {
                mDrawView.setOffset(event.values[0]);
                mDrawView.invalidate();
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        //mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView); // changed 10Apr

        locMgr = (LocationManager) this.getSystemService(LOCATION_SERVICE); // <2>
        LocationProvider high = locMgr.getProvider(locMgr.getBestProvider(
                LocationUtils.createFineCriteria(), true));

        // using high accuracy provider... to listen for updates
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
        locMgr.requestLocationUpdates(high.getName(), 0, 0f,
                new LocationListener() {
                    public void onLocationChanged(Location location) {
                        // do something here to save this new location
                        Log.d(TAG, "Location Changed");
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();
                        mDrawView.setMyLocation(latitude, longitude);
                        mDrawView.invalidate();

                    }

                    public void onStatusChanged(String s, int i, Bundle bundle) {

                    }

                    public void onProviderEnabled(String s) {
                        // try switching to a different provider
                    }

                    public void onProviderDisabled(String s) {
                        // try switching to a different provider
                    }
                });
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE); //changed 10Apr

    }

    protected void onStart() {

        mGoogleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onResume() {
        if (DEBUG)
            Log.d(TAG, "onResume");
        super.onResume();
        mSensorManager.registerListener(mListener, mSensor,
                SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    protected void onStop() {
        if (DEBUG)
            Log.d(TAG, "onStop");
        mSensorManager.unregisterListener(mListener);

        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }


    /////////// Json Reader
    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();

        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONObject readJsonFromUrl() throws IOException, JSONException {


        StringBuilder googlePlacesUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=" + lat + "," + lng);
        Log.d("print status\n", lat + "\n" + lng + "");
        googlePlacesUrl.append("&radius=" + 500);
        googlePlacesUrl.append("&types="  + "restaurant|" + "airport|" + "cafe");
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + "AIzaSyAegNRwiM4rN5vILXDqwt8PY2TvhTfCOgE");


        String url = googlePlacesUrl.toString();
        Log.d("URL ", url);
        JSONObject var5;

        Log.i("On Server", url);
        InputStream is = (new URL(url)).openStream();


        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            var5 = json;
        } finally {
            is.close();
        }


        return var5;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(60000);
        try {
            Log.e("request location ","request location");
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        } catch (SecurityException e) {
            Toast.makeText(MainActivity.this,
                    "SecurityException:\n" + e.toString(),
                    Toast.LENGTH_LONG).show();
        }

        //changed 10Apr

        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                Log.e("called update","called update");
                lat = location.getLatitude();
                lng = location.getLongitude();
            } else {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                //lat = 0.0;
                //lng = 0.0;
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        //new Listen1().execute();
        mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView);

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection Suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e("call location changed","call location  changed");
        try {
            Location loc = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location != null) {
                Log.e("called update","called update");
                lat = loc.getLatitude();
                lng = loc.getLongitude();
            } else {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                //lat = 0.0;
                //lng = 0.0;
            }
            lat = location.getLatitude();
            lng = location.getLongitude();
            Log.e("Latitude: ", "" + lat);
            Log.e("\nLongitude: ", "" + lng);

        } catch (SecurityException e) {
            e.printStackTrace();
        }
       // if ()


        new Listen1().execute();
        mDrawView = (DrawSurfaceView) findViewById(R.id.drawSurfaceView);
    }

    protected void onPause() {
        super.onPause();
    }


    private class Listen1 extends AsyncTask<Void, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... params) {


            try
            {
                JSONObject json = readJsonFromUrl();
                JSONArray array = (JSONArray) json.get("results");
                props.clear();
                for(int i =0 ; i < array.length() ; i++)
                {
                    JSONObject innerObj = (JSONObject) array.get(i);
                    String name = innerObj.optString("name");

                    JSONObject js= innerObj.optJSONObject("geometry");

                    JSONObject js1= js.optJSONObject("location");

                    String lati= js1.optString("lat");
                    String lngi= js1.optString("lng");
                    double lat2 = Double.parseDouble(lati);
                    double lng2=Double.parseDouble(lngi);
                    //Object lattest =
                    props.add(new Point(lat2,lng2,name));
                    Log.e("name" ,name );
                    Log.e("name" ,lat2+"" );
                    Log.e("name" ,lng2+"" );

                }


            } catch (Exception e) {
                Log.e("Speaker", "Speaker error", e);
            }

            return "";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //returnedText.setText(s);
            Log.d("MAP Detail", s);
            mDrawView.Refresh();
          //  mDrawView.refreshDrawableState();
        }
    }
}


