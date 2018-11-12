package com.attendy.attendy

import android.app.IntentService
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceTransitionsIntentService : IntentService("GeoTrIntentService") {

    companion object {
        private const val LOG_TAG = "GeoTrIntentService"
    }

    override fun onHandleIntent(intent: Intent?) {
        // 1  obtain the GeofencingEvent object using the intent.
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        // 2 If there’s an error, you log it
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.errorCode)
            Log.e(LOG_TAG, errorMessage)
            return
        }
        // 3 Otherwise, you handle the event
        handleEvent(geofencingEvent)
    }

    private fun handleEvent(event: GeofencingEvent) {
        // 1 first check if the transition is related to entering a geofence.
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            // 2 If the user creates overlapping geofences, there may be multiple triggering events,
            // so, here, you pick only the first reminder object
            val reminder = getFirstReminder(event.triggeringGeofences)
            val message = reminder?.message
            val latLng = reminder?.latLng
            if (message != null && latLng != null) {
                // 3 You then show the notification using the message from the reminder.
                sendNotification(this, message, latLng)
            }
        }
    }
    private fun getFirstReminder(triggeringGeofences: List<Geofence>): Reminder? {
        val firstGeofence = triggeringGeofences[0]
        return (application as ReminderApp).getRepository().get(firstGeofence.requestId)
    }

}
