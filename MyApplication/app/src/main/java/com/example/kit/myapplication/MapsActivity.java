package com.example.kit.myapplication;

//region "imports"
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.app.AlertDialog;
import android.view.ViewGroup;
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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
//endregion

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks,
        ActivityCompat.OnRequestPermissionsResultCallback,
        OnGeoLookupCompleted {

    //region "other classes that arent widgets"
    private GoogleApiClient mGoogleApiClient;
    private GestureDetector mGestureDetector;
    private GeoLookup mGeoLookup;
    private LocationManager mLocationManager;
    private MyLocationListener mLocationListener = new MyLocationListener();
    //endregion

    //region "widgets"
    private GoogleMap mMap;
    private ListView mListView;
    private FrameLayout mFetchedAddressLayout;
    private TextView mAddressLabel;
    private Button mLookupButton;
    private FrameLayout mPleaseWaitLayout;
    //endregion

    //region "lists and adapters"
    ArrayList<String> mListItems = new ArrayList<String>();
    ArrayAdapter<String> mAdapter;
    //endregion

    //region "constants"
    final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 124;
    final int LOCATION_REFRESH_TIME = 10;
    final int LOCATION_REFRESH_DISTANCE = 1;
    //endregion

    //region "initialization"
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_maps);
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFragment.getMapAsync(this);

            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            decorView.setSystemUiVisibility(uiOptions);

            mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            KitSqlHelper dbHandler = new KitSqlHelper(this);
            mListItems = dbHandler.getAllEntries();
            mListView = (ListView) findViewById(R.id.locationList);
            mAdapter = new LocationsAdapter(this, R.layout.list_view_element, mListItems);
            mListView.setAdapter(mAdapter);
            mLookupButton = (Button) findViewById(R.id.lookupButton);
            mLookupButton.setOnClickListener(this);
            mFetchedAddressLayout = (FrameLayout) findViewById(R.id.addressFrame);
            mAddressLabel = (TextView) findViewById(R.id.addressLabel);
            mPleaseWaitLayout = (FrameLayout) findViewById(R.id.pleaseWaitLayout);

            mGestureDetector = new GestureDetector(new SwipeGestureListener());
            mFetchedAddressLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    mGestureDetector.onTouchEvent(event);

                    return true;
                }
            });

//            FrameLayout topLayout = (FrameLayout)findViewById(R.id.pleaseWaitLayout);
            mPleaseWaitLayout.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    // touching anywhere at all cancels lookup
                    if (mPleaseWaitLayout.isShown())
                    {
                        mPleaseWaitLayout.setVisibility(View.INVISIBLE);
                        cancelLookup();
                    }

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
    //endregion

    //region "callbacks"
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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mGeoLookup = new GeoLookup(this, this, mLocationListener.currentLocation);
                mGeoLookup.execute();
                mPleaseWaitLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    public void cancelLookup() {
        if (mGeoLookup != null)
        {
            mGeoLookup.cancel(true);
            mGeoLookup = null;
        }
    }

    public void onGeoLookupCompleted() {
        // show window, add item
        showAddressWindow(mGeoLookup.address, mLocationListener.currentLocation);
        addItems(mGeoLookup.address);

        if (mPleaseWaitLayout.isShown())
        {
            mPleaseWaitLayout.setVisibility(View.INVISIBLE);
            cancelLookup();
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

        commitChanges();
    }

    // following two callbacks relate to GoogleApiClient
    @Override
    public void onConnected(Bundle bundle) {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_REFRESH_TIME, LOCATION_REFRESH_DISTANCE, mLocationListener);
            }
        } catch (Error error) {
            spawnAlertDialog("ERROR", error.toString());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay!
                    Log.d("kit", "Permission Granted");
                    setupMap();

                } else {

                    // permission denied, boo!
                    Log.d("kit", "Permission Denied");
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    protected void onPause()
    {
        super.onPause();

        commitChanges();
    }
    //endregion

    //region "commit changes"

    void commitChanges()
    {
        KitSqlHelper dbHelper = new KitSqlHelper(this);
        dbHelper.recreateTable();
        dbHelper.saveAllEntries(mListItems);
    }
    //endregion

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
        mListItems.add(0, currentTimeString + " - " + address.getAddressLine(0));
        mAdapter.notifyDataSetChanged();
    }

    void spawnAlertDialog(String title, String message) {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setMessage(message);
        alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }

    class SwipeGestureListener extends GestureDetector.SimpleOnGestureListener {


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

    public class LocationsAdapter extends ArrayAdapter<String> {
        public LocationsAdapter(Context context, int res, ArrayList<String> locations) {
            super(context, 0, locations);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            String prevLocation = getItem(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_view_element, parent, false);
            }

            TextView tvLocation = (TextView)convertView.findViewById(R.id.tvLocation);

            tvLocation.setText(prevLocation);

            return convertView;
        }
    }
}
