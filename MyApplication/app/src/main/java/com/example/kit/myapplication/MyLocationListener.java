package com.example.kit.myapplication;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;

import java.util.Calendar;

/**
 * Created by Kit on 10/02/2016.
 */
public class MyLocationListener implements LocationListener {

    public LatLng currentLocation;
    public int lastUpdate;

    @Override
    public void onLocationChanged(Location location) {
        currentLocation = new LatLng(location.getLatitude(), location.getLongitude());
        lastUpdate = Calendar.getInstance().get(Calendar.SECOND);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
