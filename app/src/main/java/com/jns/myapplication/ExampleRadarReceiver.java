package com.jns.myapplication;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import io.radar.sdk.Radar;
import io.radar.sdk.RadarReceiver;
import io.radar.sdk.model.RadarEvent;
import io.radar.sdk.model.RadarGeofence;
import io.radar.sdk.model.RadarUser;

public class ExampleRadarReceiver extends RadarReceiver {

    private static final String TAG = "ExampleRadarReceiver";
    private static final int NOTIFICATION_ID = 1337;

    @Override
    public void onEventsReceived(@NonNull Context context, @NonNull RadarEvent[] events, @NonNull RadarUser user) {

        String title = "";
        Log.e("TAG", "onEventsReceived");
        for (RadarEvent event : events) {
            String eventString = Utils.stringForEvent(event);
            notify(context, "Event", eventString);
        }



//        for (RadarEvent event : events) {
//            String eventString = Utils.stringForEvent(event);
//            Log.e("TAG", "onEventsReceived : " + eventString);
//
//            Toast.makeText(context, eventString, Toast.LENGTH_SHORT).show();
//           // notify(context, "Event", eventString);
//
//            //            try{
//            //                switch (event.getType()) {
//            //                    case USER_ENTERED_GEOFENCE:
//            //                        title = "Entered geofence " + (
//            //                                event.getGeofence() != null ? event.getGeofence().getMetadata().getString("push_title_enter") : "-");
//            //                        break;
//            //                    case USER_EXITED_GEOFENCE:
//            //                        title = "Exited geofence " + (
//            //                                event.getGeofence() != null ? event.getGeofence().getMetadata().getString("push_title_leave") : "-");
//            //                        break;
//            //                    default:
//            //                }
//            //                notify(context, title, event.getGeofence().getMetadata() .getString("push_description").toString());
//            //
//            //
//            //            }catch (JSONException e){
//            //                e.printStackTrace();
//            //            }
//
//
//        }
    }

    @Override
    public void onLocationUpdated(Context context, Location location, RadarUser user) {

        String state = "Moved to";
        if (user.getStopped()) {
            state = "Stopped at";
            notify(context, "Location", "user stopped ");
        }
//        String locationString = String.format(Locale.getDefault(), "%s location (%f, %f) with accuracy %d meters",
//                state, location.getLatitude(), location.getLongitude(), (int)location.getAccuracy());
//        //notify(context, "Location", locationString);



        RadarGeofence[] geofences = user.getGeofences();



        Log.e("TAG", "onLocationUpdated ");


        if (geofences != null) {
            Log.e("TAG", "geofences lenght " + geofences.length);
            for (RadarGeofence radarGeofence:geofences) {


                Log.e("TAG", "radarGeofence tag : " + radarGeofence.getTag());
                notify(context,"Title",radarGeofence.getDescription());


            }


        }

//        for (RadarEvent event : events) {
//            String eventString = Utils.stringForEvent(event);
//            Log.e("TAG", "onEventsReceived : " + eventString);
//
//            Toast.makeText(context, eventString, Toast.LENGTH_SHORT).show();
//            notify(context, "Event", eventString);
//        }

//        String state = "Moved to";
//        if (user.getStopped()) {
//            state = "Stopped at";
//        }
//        String locationMessage = String.format(Locale.getDefault(), "%s location (%f, %f) with accuracy %d meters",
//                state, location.getLatitude(), location.getLongitude(), (int) location.getAccuracy());
//        notify(context, "Location", locationMessage);
    }

    @Override
    public void onError(@NonNull Context context, @NonNull Radar.RadarStatus status) {
        String statusString = Utils.stringForStatus(status);
        Log.e(TAG, statusString);
        notify(context, "Error", statusString);
    }

    private void notify(Context context, String title, String text) {
        Intent intent = new Intent(context, ExampleRadarReceiver.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelName = "RadarExample";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel
                    channel = new NotificationChannel(channelName, channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(context, channelName)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(pending)
                .setSmallIcon(io.radar.sdk.R.drawable.notify_panel_notification_icon_bg)
                .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
                .build();


        notificationManager.notify(TAG, NOTIFICATION_ID, notification);
    }

}
