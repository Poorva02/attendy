
package com.attendy.attendy


import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.support.v4.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import com.attendy.attendy.MainActivity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent




// TODO: Document map view --> http://www.zoftino.com/android-mapview-tutorial
//    https://code.tutsplus.com/tutorials/how-to-work-with-geofences-on-android--cms-26639
//
class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {

    private val TAG = "GeofenceTransIntentServ"
    val GEOFENCE_NOTIFICATION_ID = 0

    private val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"


    // ...
    override fun onHandleIntent(intent: Intent?) {
        Log.d(TAG, "geofence handled.")

        val geofencingEvent: GeofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent.hasError()) {
            val errorMessage: String = getErrorString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
        }


        val geofenceTransition: Int = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER
                || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {

            val triggeringGeofenes = geofencingEvent.triggeringGeofences
            val geofenceTransitionDetails = getGeofenceTrasitionDetails(geofenceTransition, triggeringGeofenes)

            sendNotification(geofenceTransitionDetails)

        }
    }

    private fun getGeofenceTrasitionDetails(geofenceTransion: Int, triggeringGeofences: List<Geofence>): String {
        Log.i(TAG, "getGeofenceTrasitionDetails " + geofenceTransion)


        val triggeringGeofencesList = ArrayList<Geofence>()

        for (geofence in triggeringGeofences) {
            triggeringGeofencesList.add(geofence)
        }

        var status: String? = null

        if (geofenceTransion == Geofence.GEOFENCE_TRANSITION_ENTER) {
            status = "Entering"
        } else if (geofenceTransion == Geofence.GEOFENCE_TRANSITION_EXIT) {
            status = "Exiting"
        }

        return status + TextUtils.join(", ", triggeringGeofencesList)
    }

    private fun sendNotification(msg: String) {
        Log.i(TAG, "sendNotification " + msg)
        val notificationIntent = MainActivity.makeNotificationIntent(applicationContext, msg)

        var stackBuilder: TaskStackBuilder = TaskStackBuilder.create(this)
        stackBuilder.addParentStack(MainActivity::class.java)
        stackBuilder.addNextIntent(notificationIntent)

        val notificationPendingIntent: PendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannel = createNotificationChannel(notificationManager)

        notificationManager.notify(GEOFENCE_NOTIFICATION_ID,
                createNotification(msg, notificationPendingIntent, notificationChannel.id))
    }

    private fun createNotification(msg: String, notificationPendingIntent: PendingIntent, notificationChannelId: String): Notification {
        Log.i(TAG, "createNotiification  " + msg)

        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
        notificationBuilder
                .setSmallIcon(R.drawable.ic_map_pin_marked)
                .setColor(Color.RED)
                .setContentTitle("Geofence Notification!")
                .setContentText(msg)
                .setContentIntent(notificationPendingIntent)
                .setDefaults(Notification.DEFAULT_LIGHTS)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setDefaults(Notification.DEFAULT_SOUND)
                .setAutoCancel(true)

        return notificationBuilder.build()
    }

    private fun createNotificationChannel(notificationManager: NotificationManager): NotificationChannel {
        val channelId = "attendy_channel_id"
        val channelName = "Attendy Channel"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val notificationChannel = NotificationChannel(channelId, channelName, importance)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.RED
        notificationChannel.enableVibration(true)
        notificationChannel.vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager.createNotificationChannel(notificationChannel)

        return notificationChannel
    }



    private fun getErrorString(errorCode: Int): String {
        when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> return "Geofence not available."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> return "Too many geofences."
            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> return "Too many pending intents."
            else -> return "Unknown error."
        }
    }
    private fun getUniqueId() = ((System.currentTimeMillis() % 10000).toInt())
}
