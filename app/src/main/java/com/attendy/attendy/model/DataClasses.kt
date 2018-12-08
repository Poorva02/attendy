package com.attendy.attendy.model

data class AttendyUser( var username: String? = "",  var email: String? = "", val uid: String? = "")

data class AttendyGeofence(var requestID: String? = null,
                           var lat: Double? = null,
                           var lng: Double? = null,
                           var radius: Float? = null,
                           var expirationDuration: Long? = null,
                           var transitionType: Int? = null,
                           var loiteringDelay: Int? = null)
