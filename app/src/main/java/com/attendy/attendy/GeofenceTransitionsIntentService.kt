
package com.attendy.attendy


import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.attendy.attendy.MainActivity
import com.google.android.gms.maps.model.LatLng

class GeofenceTransitionsIntentService : IntentService("GeofenceTransitionsIntentService") {

    private val TAG = "GeofenceTransitionsIntentService"
    private val NOTIFICATION_CHANNEL_ID = BuildConfig.APPLICATION_ID + ".channel"


    // ...
    override fun onHandleIntent(intent: Intent?) {

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
