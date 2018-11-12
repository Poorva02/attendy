package com.attendy.attendy

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.checkSelfPermission
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.stats.CodePackage.REMINDERS
import com.google.gson.Gson
import java.util.jar.Manifest

class ReminderRepository(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "ReminderRepository"
        private const val REMINDERS = "REMINDERS"
    }

    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun add(reminder: Reminder,
            success: () -> Unit,
            failure: (error: String) -> Unit) {
        // 1    create the geofence model using the data from the reminder
        val geofence = buildGeofence(reminder)
        if (geofence != null
                && checkSelfPermission(
                        context,
                        android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 2 You use the GeofencingClient to add the geofence
            // that you’ve just built with a geofencing request and a pending intent
            geofencingClient
                    .addGeofences(buildGeofencingRequest(geofence), geofencePendingIntent)
                    // 3   If the geofence is added successfully, you save the reminder and call the success argument
                    .addOnSuccessListener {
                        saveAll(getAll() + reminder)
                        success()
                    }
                    // 4     If there’s an error, you call the failure argument (without saving the reminder)
                    .addOnFailureListener {
                        failure(GeofenceErrorMessages.getErrorString(context, it))
                    }

        }
    }

    private fun buildGeofence(reminder: Reminder): Geofence? {
        val latitude = reminder.latLng?.latitude
        val longitude = reminder.latLng?.longitude
        val radius = reminder.radius

        if (latitude != null && longitude != null && radius != null) {
            return Geofence.Builder()
                    // 1   his id uniquely identifies the geofence within your app.
                    // We obtain this from the reminder model
                    .setRequestId(reminder.id)
                    // 2  get these from the reminder model at the top of the new method
                    .setCircularRegion(
                            latitude,
                            longitude,
                            radius.toFloat()
                    )
                    // 3To trigger an event when the user enters the geofence, use GEOFENCE_TRANSITION_ENTER.
                    // Other options are GEOFENCE_TRANSITION_EXIT and GEOFENCE_TRANSITION_DWELL

                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)

                    // 4  Use NEVER_EXPIRE so this geofence will exist until the user removes it.
                    // The other option is to enter a duration (ms) after which the geofence will expire

                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build()
        }

        return null
    }

    private fun buildGeofencingRequest(geofence: Geofence): GeofencingRequest {
        //setInitialTrigger() to set the desired behavior at the moment the geofences are added.
        // Setting the value to 0 indicates that you don’t want to trigger a GEOFENCE_TRANSITION_ENTER event
        // if the device is already inside the geofence that you’ve just added
        return GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofences(listOf(geofence))
                .build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        // when the GEOFENCE_TRANSITION_ENTER event is triggered, it’ll launch GeofenceTransitionsIntentService to handle the event
        val intent = Intent(context, GeofenceTransitionsIntentService::class.java)
        PendingIntent.getService(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun remove(reminder: Reminder,
               success: () -> Unit,
               failure: (error: String) -> Unit) {
        geofencingClient
                .removeGeofences(listOf(reminder.id))
                .addOnSuccessListener {
                    saveAll(getAll() - reminder)
                    success()
                }
                .addOnFailureListener {
                    failure(GeofenceErrorMessages.getErrorString(context, it))
                }
    }


    private fun saveAll(list: List<Reminder>) {
        preferences
                .edit()
                .putString(REMINDERS, gson.toJson(list))
                .apply()
    }

    fun getAll(): List<Reminder> {
        if (preferences.contains(REMINDERS)) {
            val remindersString = preferences.getString(REMINDERS, null)
            val arrayOfReminders = gson.fromJson(remindersString,
                    Array<Reminder>::class.java)
            if (arrayOfReminders != null) {
                return arrayOfReminders.toList()
            }
        }
        return listOf()
    }




    fun get(requestId: String?) = getAll().firstOrNull { it.id == requestId }

    fun getLast() = getAll().lastOrNull()

}