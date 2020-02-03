package com.jns.myapplication;

import android.content.Context;
import android.os.Environment;
import android.provider.Settings;
import android.provider.SyncStateContract;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.radar.sdk.Radar;
import io.radar.sdk.model.RadarEvent;

class Utils {
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    static String getUserId(Context context) {
        if (context == null) {
            return null;
        }

        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    static String stringForStatus(Radar.RadarStatus status) {
        switch (status) {
            case SUCCESS:
                return "Success";
            case ERROR_PUBLISHABLE_KEY:
                return "Publishable Key Error";
            case ERROR_PERMISSIONS:
                return "Permissions Error";
            case ERROR_LOCATION:
                return "Location Error";
            case ERROR_NETWORK:
                return "Network Error";
            case ERROR_UNAUTHORIZED:
                return "Unauthorized Error";
            case ERROR_SERVER:
                return "Server Error";
            default:
                return "Unknown Error";
        }

    }

    public static String getCurrentTimeStamp() {
        String currentTimeStamp = null;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT,
                    java.util.Locale.getDefault());
            currentTimeStamp = dateFormat.format(new Date());
        } catch (Exception e) {
            Log.e("FileLog", Log.getStackTraceString(e));
        }

        return currentTimeStamp;
    }


    public static void storeLogToTextFile(String time, String description) {
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "ApplicationLog");
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, "radar.txt");
            FileWriter writer = new FileWriter(file, true);
            writer.append("Time : "+time );
            writer.append(" Description : "+description +"\n");
            writer.flush();
            writer.close();
        } catch (
                IOException e) {
            e.printStackTrace();
        }

    }


    static String stringForEvent(RadarEvent event) {
        switch (event.getType()) {
            case USER_ENTERED_GEOFENCE:
                return "Entered geofence " + (event.getGeofence() != null ? event.getGeofence().getDescription() : "-");
            case USER_EXITED_GEOFENCE:
                return "Exited geofence " + (event.getGeofence() != null ? event.getGeofence().getDescription() : "-");
            case USER_ENTERED_HOME:
                return "Entered home";
            case USER_EXITED_HOME:
                return "Exited home";
            case USER_ENTERED_OFFICE:
                return "Entered office";
            case USER_EXITED_OFFICE:
                return "Exited office";
            case USER_STARTED_TRAVELING:
                return "Started traveling";
            case USER_STOPPED_TRAVELING:
                return "Stopped traveling";
            case USER_ENTERED_PLACE:
                return "Entered place " + (event.getPlace() != null ? event.getPlace().getName() : "-");
            case USER_EXITED_PLACE:
                return "Exited place " + (event.getPlace() != null ? event.getPlace().getName() : "-");
            default:
                return "-";
        }
    }

}
