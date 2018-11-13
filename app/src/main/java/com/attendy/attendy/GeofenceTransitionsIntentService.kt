
package com.attendy.attendy


import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.attendy.attendy.BuildConfig
import com.attendy.attendy.MainActivity
import com.attendy.attendy.R
import com.google.android.gms.maps.model.LatLng

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {

    private val TAG = "GeofenceTransitionsIntentService"
    private val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"


    // ...
    override fun onHandleIntent(intent: Intent?) {
//        val geofencingEvent = GeofencingEvent.fromIntent(intent)
//        if (geofencingEvent.hasError()) {
//            val errorMessage = GeofenceErrorMessages.getErrorString(this,
//                    geofencingEvent.errorCode)
//            Log.e(TAG, errorMessage)
//            return
//        }
//
//        // Get the transition type.
//        val geofenceTransition = geofencingEvent.geofenceTransition
//
//        // Test that the reported transition was of interest.
//        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
//        geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
//
//            // Get the geofences that were triggered. A single event can trigger
//            // multiple geofences.
//            val triggeringGeofences = geofencingEvent.triggeringGeofences
//
//            // Get the transition details as a String.
////            val geofenceTransitionDetails = getGeofenceTransitionDetails(
////                    this,
////                    geofenceTransition,
////                    triggeringGeofences
////            )
//            // TODO fix the geofenchTransitionDetails
//            val geofenceTransitionDetails = geofencingEvent.geofenceTransition
//
//            // Send notification and log the transition details.
//            sendNotification(this, geofenceTransitionDetails, LatLng(0))
//            Log.i(TAG, geofenceTransitionDetails)
//        } else {
//            // Log the error.
//            Log.e(TAG, getString(111111,
//                    geofenceTransition))
//        }
    }



    private fun sendNotification(context: Context, message: String, latLng: LatLng) {
        val notificationManager = context
                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
            val name = context.getString(R.string.app_name)
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(channel)
        }

        val intent = MainActivity.newIntent(context.applicationContext, latLng)

        val stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(MainActivity::class.java)
                .addNextIntent(intent)
        val notificationPendingIntent = stackBuilder
                .getPendingIntent(getUniqueId(), PendingIntent.FLAG_UPDATE_CURRENT)

        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(message)
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true)
                .build()

        notificationManager.notify(getUniqueId(), notification)
    }




    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())
}











//package com.attendy.attendy
//
//import android.app.*
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.support.v4.app.NotificationCompat
//import android.util.Log
//import com.google.android.gms.location.Geofence
//import com.google.android.gms.location.GeofencingEvent
//import com.google.android.gms.maps.model.LatLng
//
//class GeofenceTransitionsIntentService : IntentService("GeoTrIntentService") {
//
//
//    private val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"
//
//
//    companion object {
//        private const val LOG_TAG = "GeoTrIntentService"
//    }
//
//    override fun onHandleIntent(intent: Intent?) {
//        // 1  obtain the GeofencingEvent object using the intent.
//        val geofencingEvent = GeofencingEvent.fromIntent(intent)
//        // 2 If there’s an error, you log it
//        if (geofencingEvent.hasError()) {
//            val errorMessage = GeofenceErrorMessages.getErrorString(this,
//                    geofencingEvent.errorCode)
//            Log.e(LOG_TAG, errorMessage)
//            return
//        }
//        // 3 Otherwise, you handle the event
//        handleEvent(geofencingEvent)
//    }
//
//    private fun handleEvent(event: GeofencingEvent) {
//        // 1 first check if the transition is related to entering a geofence.
//        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
//            // 2 If the user creates overlapping geofences, there may be multiple triggering events,
//            // so, here, you pick only the first reminder object
//            val reminder = getFirstReminder(event.triggeringGeofences)
//            val message = reminder?.message
//            val latLng = reminder?.latLng
//            if (message != null && latLng != null) {
//                // 3 You then show the notification using the message from the reminder.
//                sendNotification(this, message, latLng)
//            }
//        }
//    }
//    private fun getFirstReminder(triggeringGeofences: List<Geofence>): Reminder? {
//        val firstGeofence = triggeringGeofences[0]
//        return (application as ReminderApp).getRepository().get(firstGeofence.requestId)
//    }
//
//    private fun sendNotification(context: Context, message: String, latLng: LatLng) {
//        val notificationManager = context
//                .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
//                && notificationManager.getNotificationChannel(NOTIFICATION_CHANNEL_ID) == null) {
//            val name = context.getString(R.string.app_name)
//            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID,
//                    name,
//                    NotificationManager.IMPORTANCE_DEFAULT)
//
//            notificationManager.createNotificationChannel(channel)
//        }
//
//        val intent = MainActivity.newIntent(context.applicationContext, latLng)
//
//        val stackBuilder = TaskStackBuilder.create(context)
//                .addParentStack(MainActivity::class.java)
//                .addNextIntent(intent)
//        val notificationPendingIntent = stackBuilder
//                .getPendingIntent(getUniqueId(), PendingIntent.FLAG_UPDATE_CURRENT)
//
//        val notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
//                .setSmallIcon(R.mipmap.ic_launcher)
//                .setContentTitle(message)
//                .setContentIntent(notificationPendingIntent)
//                .setAutoCancel(true)
//                .build()
//
//        notificationManager.notify(getUniqueId(), notification)
//    }
//
//
//
//
//    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())
//
//}
