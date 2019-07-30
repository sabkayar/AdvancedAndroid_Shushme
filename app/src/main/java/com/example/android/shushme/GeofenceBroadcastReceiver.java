package com.example.android.shushme;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.net.ConnectException;

public class GeofenceBroadcastReceiver extends BroadcastReceiver {

    private static final int NOTIFICATION_ID = 100;
    private static final String CHANNEL_ID = "geofence_channel";
    private static final String LOG_TAG =GeofenceBroadcastReceiver.class.getSimpleName() ;


    /***
     * Handles the Broadcast message sent when the Geofence Transition is triggered
     * Careful here though, this is running on the main thread so make sure you start an AsyncTask for
     * anything that takes longer than say 10 second to run
     *
     * @param context
     * @param intent
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // Get the Geofence Event from the Intent sent through
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            Log.e(LOG_TAG, String.format("Error code : %d", geofencingEvent.getErrorCode()));
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        // Check which transition type has triggered this event
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            setRingerMode(context, AudioManager.RINGER_MODE_SILENT);
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            setRingerMode(context, AudioManager.RINGER_MODE_NORMAL);
        } else {
            // Log the error.
            Log.e(LOG_TAG, String.format("Unknown transition : %d", geofenceTransition));
            // No need to do anything else
            return;
        }
        // Send the notification
        sendNotification(context, geofenceTransition);
    }


    /**
     * Posts a notification in the notification bar when a transition is detected
     * Uses different icon drawables for different transition types
     * If the user clicks the notification, control goes to the MainActivity
     *
     * @param context        The calling context for building a task stack
     * @param transitionType The geofence transition type, can be Geofence.GEOFENCE_TRANSITION_ENTER
     *                       or Geofence.GEOFENCE_TRANSITION_EXIT
     */
    private void sendNotification(Context context, int transitionType) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(context, MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);


        //Create Notification channel and return channel id if sdk >= Android Oreo else return null
        String channelId = createNotificationChannel(context);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context, channelId);
        // Check the transition type to display the relevant icon image
        if (transitionType == Geofence.GEOFENCE_TRANSITION_ENTER) {
            notificationBuilder.setSmallIcon(R.drawable.ic_volume_off_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_off_white_24dp))
                    .setContentTitle(context.getString(R.string.silent_mode_activated));
        } else if (transitionType == Geofence.GEOFENCE_TRANSITION_EXIT) {
            notificationBuilder.setSmallIcon(R.drawable.ic_volume_up_white_24dp)
                    .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                            R.drawable.ic_volume_up_white_24dp))
                    .setContentTitle(context.getString(R.string.back_to_normal));

        }

        notificationBuilder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true);


        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
// notificationId is a unique int for each notification that you must define
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private String createNotificationChannel(Context context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = context.getString(R.string.channel_name);
            String description = context.getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            notificationChannel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                return CHANNEL_ID;
            } else {
                return null;
            }

        } else {
            return null;
        }
    }


    /**
     * Changes the ringer mode on the device to either silent or back to normal
     *
     * @param context The context to access AUDIO_SERVICE
     * @param mode    The desired mode to switch device to, can be AudioManager.RINGER_MODE_SILENT or
     *                AudioManager.RINGER_MODE_NORMAL
     */
    private void setRingerMode(Context context, int mode) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // Check for DND permissions for API 24+
        if (android.os.Build.VERSION.SDK_INT < 24 ||
                (android.os.Build.VERSION.SDK_INT >= 24 && nm.isNotificationPolicyAccessGranted())) {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            audioManager.setRingerMode(mode);
        }
    }
}
