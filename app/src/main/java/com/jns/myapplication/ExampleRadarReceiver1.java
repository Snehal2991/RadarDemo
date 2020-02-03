package com.jns.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.radar.sdk.Radar;
import io.radar.sdk.RadarReceiver;
import io.radar.sdk.model.RadarEvent;
import io.radar.sdk.model.RadarGeofence;
import io.radar.sdk.model.RadarUser;

import static android.content.Context.BATTERY_SERVICE;

public class ExampleRadarReceiver1 extends RadarReceiver {

    private static final String TAG = "ExampleRadarReceiver";
    private static final int NOTIFICATION_ID = 1337;
    boolean isEventReceived = false;

    @Override
    public void onEventsReceived(@NonNull Context context, @NonNull RadarEvent[] events, @NonNull RadarUser user) {
        Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG onEventsReceived  " + events[0].getType());

        Log.e("TAG", "onEventsReceived: " + events[0].getType());
        Toast.makeText(context, "onEventsReceived:  " + events[0].getType(), Toast.LENGTH_SHORT).show();
        isEventReceived = true;
        for (RadarEvent event : events) {
            String eventString = Utils.stringForEvent(event);
            notify(context, "Event", eventString);
        }


    }

    @Override
    public void onLocationUpdated(Context context, Location location, RadarUser user) {

        isEventReceived = false;
        String address = getAddress(location.getLatitude(), location.getLongitude(), context);

        Toast.makeText(context, "onLocationUpdated " + address, Toast.LENGTH_SHORT).show();
        String state = "Moved to";
        if (user.getStopped()) {
            Toast.makeText(context, "onLocationUpdated user stopped at  " + address, Toast.LENGTH_SHORT).show();
            state = "Stopped at";
        }

        Log.e("TAG", " onLocationUpdated  state:: " + state + " address : " + address);
        Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG onLocationUpdated  state: " + state + " address : " + address);

        getNetworkState(context);
        getBatteryPercentage(context);

        notify(context, state, address);

        //isUserInGeofence(context, state, address, user);


    }


    public void getNetworkState(Context context) {
        ConnectivityManager connMgr =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean isWifiConn = false;
        boolean isMobileConn = false;


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && isOnline(context)) {
            for (Network network : connMgr.getAllNetworks()) {
                NetworkInfo networkInfo = connMgr.getNetworkInfo(network);
                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    isWifiConn |= networkInfo.isConnected();
                }
                if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE) {
                    isMobileConn |= networkInfo.isConnected();
                }
            }
            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG NetworkState : Wifi connected: " + isWifiConn);
            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG NetworkState : Mobile connected: " + isMobileConn);

            Log.e("TAG getNetworkState", "Wifi connected: " + isWifiConn);
            Log.e("TAG getNetworkState", "Mobile connected: " + isMobileConn);
        }

    }


    public boolean isOnline(Context context) {
        ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    public void getBatteryPercentage(Context context) {
        BatInfoReceiver batInfoReceiver = new BatInfoReceiver();

        if (Build.VERSION.SDK_INT >= 21) {
            BatteryManager bm = (BatteryManager) context.getSystemService(BATTERY_SERVICE);
            int batteryPercentage = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);

            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG >= 21 BatteryPercentage : " + batteryPercentage + "\n " +
                    "************************************* \n");

            Log.e("TAG  ", "batteryPercentage for api  >= 21  : " + batteryPercentage);


        } else {
            IntentFilter iFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(batInfoReceiver, iFilter);

            int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
            int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;

            double batteryPct = level / (double) scale;
            int batteryPercentage = (int) (batteryPct * 100);
            Utils.storeLogToTextFile(Utils.getCurrentTimeStamp(), "TAG < 21 BatteryPercentage : " + batteryPercentage + "\n " +
                    "************************************* \n" );

            Log.e("TAG  ", "batteryPercentage for api  < 21  : " + batteryPercentage);
        }

    }


    private void isUserInGeofence(Context context, String state, String address, RadarUser user) {
        RadarGeofence[] geofences = user.getGeofences();
        if (geofences != null && geofences.length > 0) {
            for (RadarGeofence geofence : geofences) {
                if (geofence != null && geofence.getTag() != null) {
                    switch (geofence.getTag()) {
                        case "park":
                            state = state + " park- ";
                            break;
                        case "attraction":
                            state = state + " attraction- ";
                            break;
                        case "restaurant":
                            state = state + " restaurant- ";
                            break;
                        default:
                            state = state + " Not listed Geofence type- ";
                    }

                }


            }
        }
        if (!isEventReceived) {
            Log.e("TAG", "no event fire location update : ");
            notify(context, state, address);
        } else {
            Log.e("TAG", " event received : ");
        }

    }

    public String getAddress(double latitude, double longitude, Context context) {
        String add = "-";
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            Address obj = addresses.get(0);
            add = obj.getAddressLine(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e("TAG", "IgetAddress OException: " + e.toString());


            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        return add;
    }

    @Override
    public void onError(@NonNull Context context, @NonNull Radar.RadarStatus status) {
        Toast.makeText(context, "onError ", Toast.LENGTH_SHORT).show();

        String statusString = Utils.stringForStatus(status);
        Log.e("TAG", "onError  " + statusString);
        notify(context, "Error", statusString);
    }

    public void notify(Context context, String title, String text) {
        Log.d(TAG, "<< Notification will be built");

        Intent intent = new Intent(context, ExampleRadarReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelName = "RadarExample";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel
                    channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(context, channelName)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setSmallIcon(io.radar.sdk.R.drawable.notification_icon_background)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .build();


        notificationManager.notify(TAG, NOTIFICATION_ID, notification);
    }

}
