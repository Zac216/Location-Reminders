package gocrew.locationreminders;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.app.Notification.DEFAULT_ALL;
import static android.app.Notification.EXTRA_NOTIFICATION_ID;
import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class RSSPullService extends Service {

    static final String CHANNEL_ID = "Location Reminders";
    DatabaseHelper myDb;
    Integer farReminders, closeReminders;
    long UPDATE_INTERVAL;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        myDb = new DatabaseHelper(this);
        UPDATE_INTERVAL = 900000;
        createNotificationChannel();
        startServiceInForeground();
        startLocationUpdates();
        myDb = new DatabaseHelper(this);
        //Toast.makeText(this, "service Started", Toast.LENGTH_SHORT).show();
        return START_STICKY;
    } //called when the service is started

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    } //default function (do not delete)

    protected void startLocationUpdates() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        //long UPDATE_INTERVAL = 900000;
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        long FASTEST_INTERVAL = 2000;
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        onLocationChanged(locationResult.getLastLocation());
                    }
                },
                Looper.myLooper());
    } //start the loop to check the user's location

    protected void stopLocationUpdates() {
        getFusedLocationProviderClient(this).removeLocationUpdates( new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                onLocationChanged(locationResult.getLastLocation());
            }
        });
    }

    public void onLocationChanged(Location location) {
        try {

            Cursor res = myDb.getAllData();

            //Toast.makeText(this, "onLocationUpdates", Toast.LENGTH_SHORT).show();
            
            if (res.getCount() == 0) {
                //Toast.makeText(this, "Service Stopping", Toast.LENGTH_SHORT).show();
                stopService(new Intent(this, RSSPullService.class));   
            }
                

            farReminders = 0;
            closeReminders = 0;
            Location tmpLocation = new Location("");
            while (res.moveToNext()) {
                String latOrLng = res.getString(4);

                double lat = Double.parseDouble(latOrLng.substring(latOrLng.indexOf("(") + 1, latOrLng.indexOf(",")));
                double lng = Double.parseDouble(latOrLng.substring(latOrLng.indexOf(",") + 1, latOrLng.indexOf(")")));
                tmpLocation.setLatitude(lat);
                tmpLocation.setLongitude(lng);

                if (location.distanceTo(tmpLocation) > 60000) {
                    farReminders++;
                    continue;
                }
                if (location.distanceTo(tmpLocation) < 5)
                    closeReminders++;

                Integer radius = 0;
                if (res.getInt(6) == 0)
                    radius = 25;
                if (res.getInt(6) == 1)
                    radius = 100;
                if (res.getInt(6) == 2)
                    radius = 1000;

                //Toast.makeText(this, "distance=" + location.distanceTo(tmpLocation), Toast.LENGTH_SHORT).show();

                if (location.distanceTo(tmpLocation) < radius)
                    notifyUser(res);
            }

            if (farReminders == res.getCount()) { //all reminders are over 60 kilometers away
                stopLocationUpdates();
                UPDATE_INTERVAL = 3600000; //60 minutes
                startLocationUpdates();
            }
            if (farReminders < res.getCount() && closeReminders == 0 && UPDATE_INTERVAL != 900000) {
                stopLocationUpdates();
                UPDATE_INTERVAL = 900000; //15 minutes
                startLocationUpdates();
            }
            if (closeReminders > 0 && UPDATE_INTERVAL != 120000) {
                stopLocationUpdates();
                UPDATE_INTERVAL = 120000; //2 minutes
                startLocationUpdates();
            }

        } catch (Exception e) {
            Toast.makeText(this, "onLocationChanged: " + e.toString(), Toast.LENGTH_LONG).show();
        }
    } //checks if the user is at the same location as a reminder

    public void notifyUser(Cursor res) {

        Integer now = (int) new Date().getTime();

        if (now - res.getInt(5) > 1800000) { //30 minutes have passed since the user was last notified 1800000


            //Intent foo = new Intent(this, notificationHandler.class);
            //foo.putExtra("delete reminder", res.getInt(0));
            //foo.setAction(Intent.ACTION_BATTERY_LOW);
            //PendingIntent snoozeFoo = PendingIntent.getBroadcast(this, 0, foo, 0);


            String title = res.getString(1);
            String description = res.getString(2);

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this, CHANNEL_ID)
                            .setSmallIcon(R.mipmap.launchertwo_round)
                            .setContentTitle("Location Reminder")
                            .setAutoCancel(true)
                            .setPriority(NotificationCompat.PRIORITY_HIGH);
                            //.addAction(R.drawable.common_full_open_on_phone, "Delete Reminder", snoozeFoo);

            if (description.equals(""))
                mBuilder.setContentText(title);
            else
                mBuilder.setContentText(title + ": " + description);

            Intent resultIntent = new Intent(this, MainActivity.class);
            PendingIntent resultPendingIntent =
                    PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentIntent(resultPendingIntent);

            int mNotificationId = res.getInt(0);
            NotificationManager mNotifyMgr =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            assert mNotifyMgr != null;
            mNotifyMgr.notify(mNotificationId, mBuilder.build());

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String foo = prefs.getString("ringtone_preference_1", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString());
            if (!foo.equals("")) {
                Uri ringtoneUri = Uri.parse(foo);
                Ringtone ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
                ringtone.play();
            }








            myDb.updateData(res.getInt(0), res.getString(1), res.getString(2), res.getString(3), res.getString(4), now, res.getInt(6));

        }
    } //creates notification to alert user (if it has been more than 30 min since last notification) and updates when user was notified

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Location Reminders";
            String description = "Called when a location reminder needs to be triggered";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setSound(null, null);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            assert notificationManager != null;
            notificationManager.createNotificationChannel(channel);
        }
    } //required to use notifications on android 8+
    private void startServiceInForeground() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(R.mipmap.launchertwo_round)
                        .setContentTitle("Location Reminder")
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentIntent(resultPendingIntent)
                        .setContentText("hi");


        startForeground(0, mBuilder.build());
    }


}