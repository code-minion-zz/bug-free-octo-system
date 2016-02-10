package com.example.kit.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Button;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private GoogleApiClient client;

    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private MyLocationListener mLocationListener = new MyLocationListener();
    private Geocoder mGeocoder;

    private Button mLookupButton;

    final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 124;
    final int LOCATION_REFRESH_TIME = 10;
    final int LOCATION_REFRESH_DISTANCE = 10;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_maps);
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);
            getSupportActionBar().hide();

            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            mLookupButton = (Button) findViewById(R.id.lookupButton);
            mLookupButton.setOnClickListener(this);

            client = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API).build();

        } catch (Error error) {
            spawnAlertDialog("ERROR", error.getCause().toString());
        }
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
        LatLng auckland = new LatLng(-36.8406, 174.74);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(auckland));
        setupMap();
    }

    public void onClick(View view) {
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // GPS not enabled, alert user
            spawnAlertDialog("ERROR", "GPS Location Services is not enabled, please enable GPS before trying again.");
        } else {
            // GPS enabled, display modal window, request location update, wait for response
            // allow user to cancel location update by dismissing modal window
//            spawnAlertDialog("SUCCESS", "CLICKED");
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                String address = getAddress(mLocationListener.currentLocation);
                Log.d("kit", address);
                spawnAlertDialog("SUCCESS", address);
                // display address window
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        client.connect();

    }

    @Override
    public void onStop() {
        super.onStop();

        if (client != null && client.isConnected())
        {
            client.disconnect();
        }
    }

    // following two callbacks relate to GoogleApiClient
    @Override
    public void onConnected(Bundle bundle) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            {
                mCurrentLocation = LocationServices
                        .FusedLocationApi
                        .getLastLocation(client);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);
            }
        }
        catch (Error error)
        {

        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("kit", "Permission Granted");
                    setupMap();

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("kit", "Permission Denied");
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
/// helper classes below

    void setupMap()
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true);
        }
        else
        {
            permissionCheck();
        }
    }

    void permissionCheck()
    {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION))
            {
                AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setCancelable(true);
                alertDialog.setTitle("GPS Location Required");
                alertDialog.setMessage("We need your location to display the blue dot!");
                final Activity currentActivity = this;
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(currentActivity,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
                    }
                });
                alertDialog.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]
                                {Manifest.permission.ACCESS_FINE_LOCATION},
                        PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            }
        }
        else
        {
            Log.d("kit", "no need to request permission again");
        }
    }

    void spawnAlertDialog(String title, String message)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        final Activity currentActivity = this;
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    /// returns closest address to given coords
    String getAddress(LatLng coords)
    {
        mGeocoder = new Geocoder(this);
        String stringAddress = "";

        try {
            Address address = mGeocoder
                    .getFromLocation(coords.latitude, coords.longitude, 1)
                    .get(0);

            if (address != null)
            {
                for (int i = 0; i < address.getMaxAddressLineIndex(); i++)
                {
                    stringAddress += address.getAddressLine(i);
                }
            }
            else
            {
                spawnAlertDialog("Not Found", "There are no addresses nearby your location");
            }
        }
        catch (IOException e)
        {
            spawnAlertDialog("ERROR", e.getCause().toString());
        }
        return stringAddress;
    }
}
