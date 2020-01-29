package com.jns.myapplication;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import io.radar.sdk.Radar;
import io.radar.sdk.RadarTrackingOptions;
import io.radar.sdk.model.RadarEvent;
import io.radar.sdk.model.RadarGeofence;
import io.radar.sdk.model.RadarUser;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        GoogleMap.OnMarkerClickListener, LocationListener,
        View.OnClickListener {

    private long UPDATE_INTERVAL = 5000;  /* 15 secs */
    private long FASTEST_INTERVAL =5000; /* 5 secs */
    private GoogleMap mMap;
    private Button trackOnceButton;
    private Switch trackingSwitch;
    private LocationRequest mLocationRequest;
    private GoogleApiClient googleApiClient;
    private ArrayList<LatLng> routePoints;
    private Marker mCurrLocationMarker;
    private Polyline line;
    private Location mLastLocation;
    FusedLocationProviderClient fusedLocationClient;

    private static final int REQUEST_CHECK_SETTINGS = 102;
    private LocationSettingsRequest.Builder builder;
    private LocationCallback mlocationCallback;

    private void checkLocationSetting(LocationSettingsRequest.Builder builder) {

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                // ...
                Log.e("TAG","checkLocationSetting onSuccess ");
                startLocationUpdates();
                return;
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull final Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    AlertDialog.Builder builder1 = new AlertDialog.Builder(MapsActivity.this);
                    builder1.setTitle("Continious Location Request");
                    builder1.setMessage("This request is essential to get location update continiously");
                    builder1.create();
                    builder1.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ResolvableApiException resolvable = (ResolvableApiException) e;
                            try {
                                resolvable.startResolutionForResult(MapsActivity.this,
                                        REQUEST_CHECK_SETTINGS);
                            } catch (IntentSender.SendIntentException e1) {
                                e1.printStackTrace();
                            }
                        }
                    });
                    builder1.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(MapsActivity.this, "Location update permission not granted", Toast.LENGTH_LONG).show();
                        }
                    });
                    builder1.show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                // All location settings are satisfied. The client can initialize
                // location requests here.
                startLocationUpdates();
            }
            else {
                checkLocationSetting(builder);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        String userId = initialiseRadar();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        TextView userIdTextView = findViewById(R.id.user_id_text_view);
        userIdTextView.setText(userId);
        mapFragment.getMapAsync(this);


        buildGoogleApiClient();

        trackOnceButton = findViewById(R.id.track_once_button);
        trackOnceButton.setOnClickListener(this);


        trackingSwitch = findViewById(R.id.tracking_switch);

        trackingSwitch.setChecked(Radar.isTracking());
        test();

        Log.e("TAG ", "tool version" + Build.VERSION.SDK_INT);


    }

    private void test() {

        Log.e("TAG", "tracking_switch");
        trackingSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.e("TAG", "tracking_switch change listner ");
                if(isChecked){
                    test1();

                }else {
                    Radar.stopTracking();
                }

            }
        });
    }
    public void test1(){

            getAddress(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            Radar.updateLocation(mLastLocation, new Radar.RadarCallback() {
                @Override
                public void onComplete(Radar.RadarStatus status, Location location, RadarEvent[] events, RadarUser user) {
                    Log.e("TAG", "Manual Tracking status:: " + status);
                    String statusString = Utils.stringForStatus(status);

                    if (status == Radar.RadarStatus.SUCCESS) {
                        Log.e("TAG Manual Tracking ", statusString);
                        //GeoFences
                        RadarGeofence[] geofences = user.getGeofences();
                        if (geofences != null && geofences.length > 0) {
                            for (RadarGeofence geofence : geofences) {
                                String geofenceString = geofence.getDescription();
                                Log.e("TAG Manual Tracking geofence found", geofenceString);
                                switch (geofence.getTag()) {
                                    case "park":
                                        break;
                                    case "attraction":
                                        break;
                                    case "restaurant":
                                        break;
                                    default:
                                }
                            }
                        }
                        //Event

                        if (events != null && events.length > 0) {
                            for (RadarEvent event : events) {
                                String eventString = Utils.stringForEvent(event);
                                Log.e("TAG Mannual Tracking event found", eventString);

                            }
                        }
                    } else {
                        Log.e("TAG Manual Tracking", statusString);
                    }

                }
            });


        }





    protected synchronized void buildGoogleApiClient() {
//        googleApiClient = new GoogleApiClient.Builder(this)
//                .addConnectionCallbacks(this)
//                .addOnConnectionFailedListener(this)
//                .addApi(LocationServices.API).build();
//        googleApiClient.connect();

        Context appContext = getApplicationContext();
        if (appContext == null || GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
                != ConnectionResult.SUCCESS) {
            return;
        }
        googleApiClient = new GoogleApiClient.Builder(appContext)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .build();
        googleApiClient.connect();





    }

    private String initialiseRadar() {
        String publishableKey = "prj_test_pk_efb5b840da8d411337ea1f5796f02e19d35a9577";//disney
        // "prj_test_pk_c401ca2bfc746f1582aba2e2be7e4a7b5a7dd18b";//globant

        // replace with your publishable API key

        Radar.initialize(publishableKey);

        String userId = Utils.getUserId(this);
        Log.e("TAG", "userId : " + userId);
        Radar.setUserId(userId);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            // background and targeting API level 29 and higher
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 0);
        }

        return userId;
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

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
            }
        } else {
            mMap.setMyLocationEnabled(true);
        }

    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        return true;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.track_once_button:
                trackOnceButton.setEnabled(false);

                Radar.trackOnce(new Radar.RadarCallback() {
                    @Override
                    public void onComplete(@NonNull Radar.RadarStatus status,
                            Location location,
                            RadarEvent[] events,
                            RadarUser user) {
                        trackOnceButton.setEnabled(true);

                        String statusString = Utils.stringForStatus(status);
                        Snackbar.make(trackOnceButton, statusString, Snackbar.LENGTH_SHORT).show();

                        if (status == Radar.RadarStatus.SUCCESS) {
                            Log.e("TAG", statusString);
                            //GeoFences
                            RadarGeofence[] geofences = user.getGeofences();
                            if (geofences != null && geofences.length > 0) {
                                for (RadarGeofence geofence : geofences) {
                                    String geofenceString = geofence.getDescription();
                                    Log.e("TAG geofence found", geofenceString);
                                    switch (geofence.getTag()) {
                                        case "park":
                                            break;
                                        case "attraction":
                                            break;
                                        case "restaurant":
                                            break;
                                        default:
                                    }
                                }
                            }
                            //Event

                            if (events != null && events.length > 0) {
                                for (RadarEvent event : events) {
                                    String eventString = Utils.stringForEvent(event);
                                    Log.e("TAG event found", eventString);
                                }
                            }
                        } else {
                            Log.e("TAG", statusString);
                        }
                    }
                });
                break;
            case R.id.tracking_switch:

                break;
            default:

        }
    }

    public void getAddress(double latitude, double longitude) {

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Address obj = addresses.get(0);
            String add = obj.getAddressLine(0);


            Log.e("TAG", "Address" + add);
            // Toast.makeText(this, "Address=>" + add,
            // Toast.LENGTH_SHORT).show();

            // TennisAppActivity.showDialog(add);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

//
//    @Override
//    public void onConnected(Bundle bundle) {
//        mLocationRequest = new LocationRequest();
//        mLocationRequest.setInterval(1);
//        mLocationRequest.setFastestInterval(1);
//        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_FINE_LOCATION)
//                == PackageManager.PERMISSION_GRANTED) {
//            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, mLocationRequest, this);
//            fusedLocationClient.requestLocationUpdates(locationRequest,
//                    locationCallback,
//                    Looper.getMainLooper());
//        }
//    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
//        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
//        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

         fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        fetchLastLocation();

         mlocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                Log.e("TAG","onLocationResult");
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update UI with location data
                    // ...
                    mLastLocation=location;

                   // getAddress(location.getLatitude(),location.getLongitude());
                    test1();
                }
            }

     
        };

        mLocationRequest = createLocationRequest();
        builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);
        checkLocationSetting(builder);




//        if(mLastLocation!=null)
//        {
//            getAddress(mLastLocation.getLatitude(), mLastLocation.getLongitude());
//
//        }
//
//        startLocationUpdates();


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    // permission was denied, show alert to explain permission
                  Log.e("TAG ","onRequestPermissionsResult Show permission alert");
                }else{
                    //permission is granted now start a background service
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fetchLastLocation();
                    }
                }
            }
        }
    }


        protected LocationRequest createLocationRequest() {
            LocationRequest mLocationRequest = LocationRequest.create();
            mLocationRequest.setInterval(30000);
            mLocationRequest.setFastestInterval(10000);
            mLocationRequest.setSmallestDisplacement(30);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
            return mLocationRequest;
        }


    private void fetchLastLocation() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    Activity#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for Activity#requestPermissions for more details.
                //                    Toast.makeText(MainActivity.this, "Permission not granted, Kindly allow permission", Toast.LENGTH_LONG).show();

                Log.e("TAG ","showPermissionAlert");
                return;
            }
        }
        fusedLocationClient.getLastLocation()
                           .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                               @Override
                               public void onSuccess(Location location) {
                                   // Got last known location. In some rare situations this can be null.
                                   if (location != null) {
                                       // Logic to handle location object
                                       Log.e("TAG LAST LOCATION: ", location.toString()); // You will get your last location here
                                   }
                               }
                           });

    }


    private void startLocationUpdates() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "Enable Permissions", Toast.LENGTH_LONG).show();
        }

//        LocationServices.FusedLocationApi.requestLocationUpdates(
//                googleApiClient, mLocationRequest, this);


        fusedLocationClient.requestLocationUpdates(mLocationRequest,
                mlocationCallback,
                null /* Looper */);

    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.e("TAG", "onLocationChanged");
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }
        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(18));

        //stop location updates
        if (googleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
        }

    }

    @Override
    public void onMapLongClick(LatLng latLng) {

    }

    @Override
    public void onMarkerDragStart(Marker marker) {

    }

    @Override
    public void onMarkerDrag(Marker marker) {

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {

    }
}
