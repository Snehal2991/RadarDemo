package com.jns.myapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import io.radar.sdk.Radar;
import io.radar.sdk.Radar.RadarCallback;
import io.radar.sdk.RadarTrackingOptions;
import io.radar.sdk.model.RadarEvent;
import io.radar.sdk.model.RadarGeofence;
import io.radar.sdk.model.RadarUser;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";
    private boolean isEfficientTracking=false;
    private GoogleMap mMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        String publishableKey = "prj_test_sk_b56fe40a64c7bc346275c6ff6968962143ca5073"; // replace with your publishable API key
        Radar.initialize(publishableKey);

        String userId = Utils.getUserId(this);
        Radar.setUserId(userId);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);


        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
        }

        TextView userIdTextView = findViewById(R.id.user_id_text_view);
        userIdTextView.setText(userId);

        final Button trackOnceButton = findViewById(R.id.track_once_button);
        trackOnceButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackOnceButton.setEnabled(false);

                Radar.trackOnce(new RadarCallback() {
                    @Override
                    public void onComplete(@NonNull Radar.RadarStatus status, Location location, RadarEvent[] events, RadarUser user) {
                        trackOnceButton.setEnabled(true);

                        String statusString = Utils.stringForStatus(status);
                        Snackbar.make(trackOnceButton, statusString, Snackbar.LENGTH_SHORT).show();

                        if (status == Radar.RadarStatus.SUCCESS) {
                            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(),"TAG trackOnce: status  "+statusString);
                            Log.e("TAG trackOnce:", statusString);

                            RadarGeofence[] geofences = user.getGeofences();
                            if (geofences != null) {
                                for (RadarGeofence geofence : geofences) {
                                    String geofenceString = geofence.getDescription();
                                    Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(),"TAG trackOnce:  geofence Description  "+geofenceString);
                                    Log.e("TAG trackOnce : geofence Description", geofenceString);
                                }
                            }

                            if (user.getPlace() != null) {
                                String placeString = user.getPlace().getName();
                                Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(),"TAG trackOnce: place  "+placeString);
                                Log.e("TAG trackOnce:", placeString);
                            }

                            for (RadarEvent event : events) {
                                String eventString = Utils.stringForEvent(event);
                                Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(),"TAG trackOnce: event  "+eventString);

                                Log.e("TAG trackOnce:", eventString);
                            }
                        } else {
                            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(),"TAG trackOnce: status fail  "+statusString);

                            Log.e("TAG trackOnce:", statusString);
                        }
                    }
                });
            }
        });

        final Switch trackingSwitch = findViewById(R.id.tracking_switch);
        trackingSwitch.setChecked(Radar.isTracking());
        trackingSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    if(isEfficientTracking){
                        RadarTrackingOptions radarTrackingOptions = new RadarTrackingOptions.Builder()
                                .priority(Radar.RadarTrackingPriority.EFFICIENCY)
                                .offline(Radar.RadarTrackingOffline.REPLAY_STOPPED)
                                .sync(Radar.RadarTrackingSync.ALL)
                                .build();
                        Radar.startTracking(radarTrackingOptions);

                    }else {
                        RadarTrackingOptions radarTrackingOptions = new RadarTrackingOptions.Builder()
                                .priority(Radar.RadarTrackingPriority.RESPONSIVENESS)
                                .offline(Radar.RadarTrackingOffline.REPLAY_STOPPED)
                                .sync(Radar.RadarTrackingSync.ALL)
                                .build();
                        Radar.startTracking(radarTrackingOptions);


                    }

                }else {
                    Radar.stopTracking();
                }
            }
        });




        final Switch trackingEfficiencySwitch = findViewById(R.id.tracking_efficiency_switch);
        trackingEfficiencySwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                     isEfficientTracking=true;


                }else {
                     isEfficientTracking=true;
                }
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }


        LatLng blueridge = new LatLng(18.5788898, 73.7349705);
        googleMap.addMarker(new MarkerOptions().position(
                blueridge).title("The blueridge "));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                blueridge, 20));

        LatLng qubix = new LatLng(18.5789, 73.7372);
        mMap.addMarker(new MarkerOptions().position(qubix).title("The Qubix Business Park Private Limited"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(blueridge));

        UiSettings mapSettings;
        mapSettings = mMap.getUiSettings();
        mapSettings.setZoomControlsEnabled(true);
        mapSettings.isScrollGesturesEnabledDuringRotateOrZoom();
        mapSettings.isZoomControlsEnabled();
        mapSettings.setZoomControlsEnabled(true);
    }
}