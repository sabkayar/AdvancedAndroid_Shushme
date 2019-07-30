package com.example.android.shushme;

/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.Manifest;
import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.android.shushme.provider.PlaceContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.FetchPlaceRequest;
import com.google.android.libraries.places.api.net.FetchPlaceResponse;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnSuccessListener<FetchPlaceResponse>, OnFailureListener {

    // Constants
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 111;
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;

    // Member variables
    private PlaceListAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private PlacesClient mPlacesClient;
    private GeoFencing mGeofencing;
    private boolean mIsEnabled;
    private List<Place> mPlaces = new ArrayList<>();
    private int mPlaceListSize;

    /**
     * Called when the activity is starting
     *
     * @param savedInstanceState The Bundle that contains the data supplied in onSaveInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO -- Initialize Places API.
        Places.initialize(getApplicationContext(), BuildConfig.API_KEY);
        // Create a new Places client instance.
        mPlacesClient = Places.createClient(this);


        Switch onOffSwitch = findViewById(R.id.enable_switch);
        mIsEnabled = getPreferences(MODE_PRIVATE).getBoolean(getString(R.string.setting_enabled), false);
        onOffSwitch.setChecked(mIsEnabled);

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
                editor.putBoolean(getString(R.string.setting_enabled), isChecked);
                mIsEnabled = isChecked;
                editor.apply();

                if (isChecked) mGeofencing.registerAllGeofences();
                else mGeofencing.unRegisterAllGeofences();
            }
        });
        // DONE (9) Create a boolean SharedPreference to store the state of the "Enable Geofences" switch
        // and initialize the switch based on the value of that SharedPreference


        // DONE (10) Handle the switch's change event and Register/Unregister geofences based on the value of isChecked
        // as well as set a private boolean mIsEnabled to the current switch's state

        // Build up the LocationServices API client
        // Uses the addApi method to request the LocationServices API
        // Also uses enableAutoManage to automatically when to connect/suspend the client

        GoogleApiClient client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();

        // Set up the recycler view
        mRecyclerView = (RecyclerView) findViewById(R.id.places_list_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new PlaceListAdapter(this, new ArrayList<Place>());
        mRecyclerView.setAdapter(mAdapter);

        mGeofencing = new GeoFencing(this);


        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.START | ItemTouchHelper.END | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                String where = PlaceContract.PlaceEntry.COLUMN_PLACE_ID + "=?";
                String[] selectionArgs = new String[]{mAdapter.getPlaces().get(position).getId()};
                mGeofencing.unRegisterAllGeofences();
                getContentResolver().delete(PlaceContract.PlaceEntry.CONTENT_URI, where, selectionArgs);
                mGeofencing.registerAllGeofences();
                refreshPlacesData();

            }
        }).attachToRecyclerView(mRecyclerView);

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "API Client connection successful!");
        refreshPlacesData();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "API Client connection suspended!");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "API Client connection failed!");
    }

    public void onLocationPermissionClicked(View view) {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSIONS_REQUEST_FINE_LOCATION);
    }

    @Override
    protected void onResume() {
        super.onResume();
        CheckBox locationPermissions = findViewById(R.id.permission_check_box);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissions.setChecked(false);
        } else {
            locationPermissions.setChecked(true);
            locationPermissions.setEnabled(false);
        }

        CheckBox ringerPermissions = findViewById(R.id.permission_ringer_mode);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if (nm != null) {
            if (Build.VERSION.SDK_INT >= 24 && !nm.isNotificationPolicyAccessGranted()) {
                ringerPermissions.setChecked(false);
            } else {
                ringerPermissions.setChecked(true);
                ringerPermissions.setEnabled(false);
            }
        }
    }


    public void onAddPlaceButtonClicked(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, getString(R.string.need_location_permission_message), Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, getString(R.string.location_permissions_granted_message), Toast.LENGTH_LONG).show();


        // Set the fields to specify which types of place data to
        // return after the user has made a selection.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME);

        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());

                //Data inserted into database
                ContentValues contentValues = new ContentValues();
                contentValues.put(PlaceContract.PlaceEntry.COLUMN_PLACE_ID, place.getId());
                getContentResolver().insert(PlaceContract.PlaceEntry.CONTENT_URI, contentValues);

                refreshPlacesData();
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
                Log.i(TAG, "The user canceled the operation.");

            }
        }
    }

    private void refreshPlacesData() {

        mPlaces.clear();
        mPlaceListSize = 0;
        Uri uri = PlaceContract.PlaceEntry.CONTENT_URI;
        final Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        );

        if (cursor == null || cursor.getCount() == 0) return;

        mPlaceListSize = cursor.getCount();
        while (cursor.moveToNext()) {
            String placeId = cursor.getString(cursor.getColumnIndex(PlaceContract.PlaceEntry.COLUMN_PLACE_ID));
            final Place tempPlace;
            // Specify the fields to return.
            List<Place.Field> placeFields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG);
            // Construct a request object, passing the place ID and fields array.
            FetchPlaceRequest request = FetchPlaceRequest.builder(placeId, placeFields).build();
            mPlacesClient.fetchPlace(request).addOnSuccessListener(this);
            mPlacesClient.fetchPlace(request).addOnFailureListener(this);
        }

    }

    @Override
    public void onSuccess(FetchPlaceResponse response) {
        Place place = response.getPlace();
        mPlaces.add(place);
        Log.i(TAG, "Place found: " + place.getName());
        if (mPlaces.size() == mPlaceListSize) {
            mAdapter.updatePlaces(mPlaces);
            mGeofencing.updateGeoFencesList(mPlaces);
            mGeofencing.registerAllGeofences();
        }
    }

    @Override
    public void onFailure(@NonNull Exception exception) {
        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            int statusCode = apiException.getStatusCode();
            // Handle error with given status code.
            Log.e(TAG, "Place not found: " + exception.getMessage());
        }
    }

    public void onRingModePermissionClicked(View view) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        }
    }
}
