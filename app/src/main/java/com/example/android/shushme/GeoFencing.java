package com.example.android.shushme;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingEvent;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.model.Place;

import java.util.ArrayList;
import java.util.List;
// DONE (1) Create a Geofencing class with a Context and GoogleApiClient constructor that
// initializes a private member ArrayList of Geofences called mGeofenceList

// DONE (2) Inside Geofencing, implement a public method called updateGeofencesList that
// given a PlaceBuffer will create a Geofence object for each Place using Geofence.Builder
// and add that Geofence to mGeofenceList

// DONE (3) Inside Geofencing, implement a private helper method called getGeofencingRequest that
// uses GeofencingRequest.Builder to return a GeofencingRequest object from the Geofence list

// DONE (4) Create a GeofenceBroadcastReceiver class that extends BroadcastReceiver and override
// onReceive() to simply log a message when called. Don't forget to add a receiver tag in the Manifest

// DONE (5) Inside Geofencing, implement a private helper method called getGeofencePendingIntent that
// returns a PendingIntent for the GeofenceBroadcastReceiver class

// DONE (6) Inside Geofencing, implement a public method called registerAllGeofences that
// registers the GeofencingRequest by calling LocationServices.GeofencingApi.addGeofences
// using the helper functions getGeofencingRequest() and getGeofencePendingIntent()

// DONE (7) Inside Geofencing, implement a public method called unRegisterAllGeofences that
// unregisters all geofences by calling LocationServices.GeofencingApi.removeGeofences
// using the helper function getGeofencePendingIntent()

// DONE (8) Create a new instance of Geofencing using "this" as the context and mClient as the client
public class GeoFencing implements OnSuccessListener<Void>, OnFailureListener {
    private static final long GEOFENCE_TIMEOUT = 24 * 60 * 60 * 1000;//24 hours
    private static final float GEOFENCE_RADIUS = 200;
    private static final String LOG_TAG = GeoFencing.class.getSimpleName();

    private Context mContext;
    private PendingIntent mPendingIntent;
    private List<Geofence> mGeofenceList;


    public GeoFencing(Context context) {
        mContext = context;
        mPendingIntent = null;
        mGeofenceList = new ArrayList<>();
    }

    public void registerAllGeofences() {

        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        GeofencingClient geofencingClient = LocationServices.getGeofencingClient(mContext);
        // Check that the API client is connected and that the list has Geofences in it
        if (mGeofenceList == null || mGeofenceList.size() == 0) {
            return;
        }
        geofencingClient.addGeofences(getGeoFencingRequest(), getGeofencePendingIntent()).addOnSuccessListener(this).addOnFailureListener(this);
    }

    public void unRegisterAllGeofences() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
            return;
        }
        LocationServices.getGeofencingClient(mContext).removeGeofences(getGeofencePendingIntent()).addOnSuccessListener(this).addOnFailureListener(this);
    }

    public void updateGeoFencesList(List<Place> places) {
        if (places == null || places.size() == 0) {
            return;
        }
        for (Place place : places) {
            Geofence geofence = new Geofence.Builder()
                    .setRequestId(place.getId())
                    .setExpirationDuration(GEOFENCE_TIMEOUT)
                    .setCircularRegion(place.getLatLng().latitude, place.getLatLng().longitude, GEOFENCE_RADIUS)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build();
            mGeofenceList.add(geofence);
        }
    }

    private GeofencingRequest getGeoFencingRequest() {
        if (mGeofenceList == null || mGeofenceList.size() == 0) {
            return null;
        }
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(mGeofenceList);
        return builder.build();
    }

    @Override
    public void onSuccess(Void aVoid) {
        Log.d(LOG_TAG, "Register/Unregister successfully");
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            int statusCode = apiException.getStatusCode();
            // Handle error with given status code.
            Log.e(LOG_TAG, "Register/Unregister failed :" + exception.getMessage());
        }
    }


    private PendingIntent getGeofencePendingIntent() {
        if (mPendingIntent != null) {
            return mPendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceBroadcastReceiver.class);
        mPendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return mPendingIntent;
    }

}
