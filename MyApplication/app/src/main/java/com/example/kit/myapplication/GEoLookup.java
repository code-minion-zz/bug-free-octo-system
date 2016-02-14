package com.example.kit.myapplication;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;

/**
 * Asynchronous Address Lookup
 *
 * Created by Kit on 14/02/2016.
 */

public class GeoLookup extends AsyncTask<Void, Void, Void> {
    private OnGeoLookupCompleted listener;
    public Address address;
    private Context context;
    private LatLng coords;

    public GeoLookup(OnGeoLookupCompleted listener, Context context, LatLng coords)
    {
        this.listener = listener;
        this.context = context;
        this.coords = coords;
    }

    /// returns closest address to given coords
    Address getAddress(LatLng coords) {
        Geocoder mGeocoder = new Geocoder(context);
        Address address = null;

        try {
            address = mGeocoder
                    .getFromLocation(coords.latitude, coords.longitude, 1)
                    .get(0);
        } catch (IOException e) {
            Log.d("E", e.toString());
        }
        return address;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
        // Things to be done while execution of long running operation is in progress. For example updating ProgessDialog

    }

    @Override
    protected void onPostExecute(Void result)
    {
        listener.onGeoLookupCompleted();
    }

    @Override
    protected Void doInBackground(Void... params) {

        address = getAddress(coords);

        return null;
    }
}