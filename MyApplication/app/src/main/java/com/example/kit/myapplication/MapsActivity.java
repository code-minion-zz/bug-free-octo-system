package com.example.kit.myapplication;

import android.Manifest;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.app.AlertDialog;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleApiClient.ConnectionCallbacks, ActivityCompat.OnRequestPermissionsResultCallback {

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private GestureDetector mGestureDetector;

    private Location mCurrentLocation;
    private LocationManager mLocationManager;
    private MyLocationListener mLocationListener = new MyLocationListener();

    private ListView mListView;
    private FrameLayout mFetchedAddressLayout;
    private TextView mAddressLabel;
    private Button mLookupButton;

    ArrayList<String> mListItems = new ArrayList<String>();
    ArrayAdapter<String> mAdapter;

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

            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            mListView = (ListView) findViewById(R.id.locationList);
            mAdapter = new ArrayAdapter<String>(this, R.layout.list_view_element, mListItems);
            mListView.setAdapter(mAdapter);
            mLookupButton = (Button) findViewById(R.id.lookupButton);
            mLookupButton.setOnClickListener(this);
            mFetchedAddressLayout = (FrameLayout) findViewById(R.id.addressFrame);
            mAddressLabel = (TextView) mFetchedAddressLayout.getChildAt(0);

            mGestureDetector = new GestureDetector(new MyGestureListener());
            mFetchedAddressLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mGestureDetector.onTouchEvent(event);

                    return true;
                }
            });

            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addApi(LocationServices.API).build();

        } catch (Error error) {
            spawnAlertDialog("ERROR", error.getCause().toString());
        }
    }

    public boolean dispatchTouchEVent(MotionEvent ev) {
        super.dispatchTouchEvent(ev);

        return mGestureDetector.onTouchEvent(ev);
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
            if (mLocationListener.currentLocation == null) {
                spawnAlertDialog("Not Ready", "Your location could not be determined, wait a bit and try again");
                return;
            }

            // GPS enabled, display modal window, request location update, wait for response
            // allow user to cancel location update by dismissing modal window
//            spawnAlertDialog("SUCCESS", "CLICKED");
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Address address = getAddress(mLocationListener.currentLocation);

                // display address window
                showAddressWindow(address, mLocationListener.currentLocation);
                addItems(address);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mGoogleApiClient.connect();

    }

    @Override
    public void onStop() {
        super.onStop();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    // following two callbacks relate to GoogleApiClient
    @Override
    public void onConnected(Bundle bundle) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices
                        .FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);
            }
        } catch (Error error) {

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
    void showAddressWindow(Address address, LatLng coords) {
        String currentTimeString = DateFormat.getTimeInstance().format(new Date());
        String stringAddress = "";
        if (address != null) {
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                stringAddress += address.getAddressLine(i);
            }
        } else {
            spawnAlertDialog("Not Found", "There are no addresses nearby your location");
            return;
        }
        mAddressLabel.setText(stringAddress + "\nFetched at " + currentTimeString + "\n\nGPS Coordinates:\n" + coords.latitude + " " + coords.longitude);
        mFetchedAddressLayout.setVisibility(View.VISIBLE);

    }

    void setupMap() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        } else {
            permissionCheck();
        }
    }

    void permissionCheck() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
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
        } else {
            Log.d("kit", "no need to request permission again");
        }
    }

    void addItems(Address address) {
        if (address == null) return;

        String currentTimeString = DateFormat.getTimeInstance().format(new Date());
        mAdapter.add(currentTimeString + " - " + address.getAddressLine(0));
    }

    void spawnAlertDialog(String title, String message) {
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
    Address getAddress(LatLng coords) {
        Geocoder mGeocoder = new Geocoder(this);
        Address address = null;

        try {
            address = mGeocoder
                    .getFromLocation(coords.latitude, coords.longitude, 1)
                    .get(0);
        } catch (IOException e) {
            spawnAlertDialog("ERROR", e.getCause().toString());
        }
        return address;
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {


        private static final int SWIPE_MIN_DISTANCE = 20;

        private static final int SWIPE_MAX_OFF_PATH = 100;

        private static final int SWIPE_THRESHOLD_VELOCITY = 100;

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX,

                               float velocityY) {

            float dX = e2.getX() - e1.getX();

            float dY = e1.getY() - e2.getY();

            if (Math.abs(dY) < SWIPE_MAX_OFF_PATH &&

                    Math.abs(velocityX) >= SWIPE_THRESHOLD_VELOCITY &&

                    Math.abs(dX) >= SWIPE_MIN_DISTANCE) {

                if (dX > 0) {

                    Toast.makeText(getApplicationContext(), "Right Swipe",
                            Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(getApplicationContext(), "Left Swipe",
                            Toast.LENGTH_SHORT).show();

                }

                return true;

            } else if (Math.abs(dX) < SWIPE_MAX_OFF_PATH &&

                    Math.abs(velocityY) >= SWIPE_THRESHOLD_VELOCITY &&

                    Math.abs(dY) >= SWIPE_MIN_DISTANCE) {

                if (dY > 0) {

                    Toast.makeText(getApplicationContext(), "Up Swipe",
                            Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(getApplicationContext(), "Down Swipe",
                            Toast.LENGTH_SHORT).show();

                    if (mFetchedAddressLayout.getVisibility() == View.VISIBLE) {
                        Animation fadeInAnimation = AnimationUtils.loadAnimation(MapsActivity.this, R.anim.slide_down_out);
                        mFetchedAddressLayout.startAnimation(fadeInAnimation);
                        mFetchedAddressLayout.setVisibility(View.GONE);
                    }

                }

                return true;

            }

            return false;

        }
    }

    public class GPSLocation extends AsyncTask<Void, Void, Void> {
        boolean running = true;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            LocationListener mlocListener = new MyLocationListener();
            if (ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mlocListener);

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
            // Things to be done while execution of long running operation is in progress. For example updating ProgessDialog
        }

        @Override
        protected void onPostExecute(Void result)
        {
        }

        @Override
        protected Void doInBackground(Void... params) {
            boolean isDataSubmitted = false;

            while(!isDataSubmitted)
            {
//                if(longitude !=0 && latitude!=0)
//                {
//                    sendSMS();
//                    isDataSubmitted = true;
//                }
            }

            return null;
        }
    }
}
