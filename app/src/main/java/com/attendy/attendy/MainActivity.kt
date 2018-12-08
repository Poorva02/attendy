package com.attendy.attendy

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.ContextCompat.startActivity
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.attendy.attendy.R.id.mapView
import com.attendy.attendy.model.AttendyGeofence
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.BaseGmsClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*


// TODO: Document map view --> http://www.zoftino.com/android-mapview-tutorial
//    https://www.raywenderlich.com/7372-geofencing-api-tutorial-for-android
//    https://code.tutsplus.com/tutorials/how-to-work-with-geofences-on-android--cms-26639
// TODO: Fix nullibity and using !!


// TODO: Check if initialize to null for values for geofence
class MainActivity : AppCompatActivity(),
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapClickListener,
        ResultCallback<Status> {


    // TODO: Fix null initializer for mUser
    private var mAuth: FirebaseAuth? = null
    private var mUser: FirebaseUser? = null
    private lateinit var mUsername: String
    private lateinit var mPhotoUrl: String
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mMapView: MapView
    private lateinit var gMap: GoogleMap
    private  var mapViewBundle: Bundle? = null
    private val MAP_VIEW_BUNDLE_KEY = "MapViewBundleKey"
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var lastLocation: Location

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private lateinit var geoFenceMarker: Marker
    private lateinit var geofenceLimits: Circle
    private lateinit var geofencePendingIntent: PendingIntent
    lateinit var geofencingClient: GeofencingClient

    lateinit var geofenceList: MutableList<Geofence>

    private var userHasPunchedIn: Boolean = false

    private lateinit var timeInString: String
    private lateinit var timeInDate: LocalDateTime
    private lateinit var timeOutString: String
    private lateinit var timeOutDate: LocalDateTime
    private lateinit var mDatabaseRef: DatabaseReference
    var hasEnteredGeofence = false





    // from raywenderlich and zoftino
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        ShowPunchesButton.setOnClickListener {
//            val intent = Intent(this,ShowMyPunches::class.java)
//            startActivity(intent)
//        }




        mAuth = FirebaseAuth.getInstance()

        // elvis ?:
        mUser = mAuth!!.currentUser

        if (mUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        } else {
            mUsername = mUser!!.displayName!!
            if (mUser!!.photoUrl != null) {
                mPhotoUrl = mUser!!.photoUrl.toString()
            }
        }

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .addApi(LocationServices.API)
                .build()

        Toast.makeText(this, "$mUsername has logged in.", Toast.LENGTH_LONG).show()
        //TODO : Initialze progress bar and Recyvle view

        mapViewBundle = null
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY)
        }

        mMapView = findViewById(R.id.mapView)
        mMapView.onCreate(mapViewBundle)
        mMapView.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }

        mDatabaseRef = FirebaseDatabase.getInstance().reference

        createLocationRequest()
        setupDate()
        setupUsername()
        setupPunchIn()
        setUpPunchOut()



    }

    private fun setupDate() {
        //display date
        val calendar = Calendar.getInstance()
        val currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)
        val currentDateTextView = findViewById<TextView>(R.id.currentDateTextView)
        currentDateTextView.text = currentDate
    }

    private fun setupUsername() {
        //display username
        val TextViewUsername = findViewById<TextView>(R.id.nameTextView)
        TextViewUsername.text = "$mUsername"
    }

    private fun setupPunchIn() {
        //display punch in time
        val buttonPunchIn = findViewById<Button>(R.id.punchInButton)

        buttonPunchIn.setOnClickListener {
            if(inGeofence) {
                val calendar = Calendar.getInstance()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                timeInDate = LocalDateTime.now()
                timeInString = timeInDate.format(formatter)

                val textView = findViewById<TextView>(R.id.punchInTextView)
                textView.text = timeInString
                userHasPunchedIn = true
                buttonPunchIn.isEnabled = false
            }


        }
    }

    private fun setUpPunchOut(){
        //display punch out time
        val buttonPunchOut = findViewById<Button>(R.id.punchOutButton)
        buttonPunchOut.setOnClickListener {
            if ( userHasPunchedIn ) {
                if (inGeofence) {
                    val calendar = Calendar.getInstance()
                    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                    timeOutDate = LocalDateTime.now()
                    timeOutString = timeOutDate.format(formatter)

                    val timeOutTextView = findViewById<TextView>(R.id.punchOutTextView)
                    timeOutTextView.text = timeOutString

                    val timeIntervalTextView = findViewById<TextView>(R.id.totalHoursTextView)


                    // TODO: Update to display correct interval.
                    // Sometimes is off by one...
                    var duration = ChronoUnit.SECONDS.between(timeInDate, timeOutDate)
                    Log.d(TAG,"setupPunchOut:: \ntimeInDate: $timeInDate, \ntimeOutDate: $timeOutDate, \nduration: $duration")

                    var day = duration/(24*3600)
//                duration = duration%(24*3600)

                    val hours = duration/3600
                    duration%=3600

                    val minutes = duration/60
                    duration%=60
                    val seconds = duration


                    val timeIntervalString = "${String.format("%02d", hours)}:${String.format("%02d", minutes)}:${String.format("%02d", seconds)}"

                    timeIntervalTextView.text = "Hours worked: $timeIntervalString"

                    buttonPunchOut.isEnabled = false

                    val currentDate = DateFormat.getDateInstance(DateFormat.FULL).format(calendar.time)

                    mDatabaseRef.child("users").child(mUser!!.uid).child(currentDate).addListenerForSingleValueEvent(object : ValueEventListener {

                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            sendPunchInfoToDatabase(currentDate, duration)
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.w(TAG, "Failed to read value.", error.toException())
                        }
                    })
                }
            }
        }
    }

    private fun sendPunchInfoToDatabase(currentDate: String, duration: Long) {

        val dataBaseRef = mDatabaseRef
                .child("users")
                .child(mUser!!.uid)
                .child("punches")
                .child(currentDate)
        val punchInRef = dataBaseRef.child("punchInTime")
        punchInRef.setValue(timeInDate)

        val punchOutRef = dataBaseRef.child("punchOutTime")
        punchOutRef.setValue(timeOutDate)

        val durationRef = dataBaseRef.child("duration")
        durationRef.setValue(duration)

    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    // from raywenderlich
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        gMap.uiSettings.isZoomControlsEnabled = true
        gMap.setOnMarkerClickListener(this)
        gMap.setOnMapClickListener(this)

        setUpMap()




    }

    // from raywenderlich
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        gMap.isMyLocationEnabled = true

        fusedLocationProviderClient.lastLocation.addOnSuccessListener(this) { location ->
            if (location!=null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng)
                gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

            }
            setupInitialGeofences()
        }
    }

    // from raywenderlich
    private fun placeMarkerOnMap(location: LatLng) {
        // 1
        val markerOptions = MarkerOptions().position(location)
        // 2
        gMap.addMarker(markerOptions)
    }

    // from raywenderlich
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    // from raywenderlich
    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest)

        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }

        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(this,
                            REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // from raywenderlich
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    // from zoftino
    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        var mapViewBundle = outState.getBundle(MAP_VIEW_BUNDLE_KEY)
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle(MAP_VIEW_BUNDLE_KEY, mapViewBundle)
        }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    // from zoftino and raywenderlich
    override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
        mapView.onResume()
    }

    // from zoftino
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    // from zoftino
    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    // from zoftino and raywenderlich
    override fun onPause() {
        mapView.onPause()
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onPause()
    }

    // from zoftino
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    // from zoftino
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    // from raywenderlich
    override fun onMarkerClick(p0: Marker?) = false

    override fun onMapClick(latLng: LatLng?) {
        Log.d(TAG, "onMapClick("+latLng +")")
        print("onMapClick"+latLng)
        markerForGeofence(latLng!!)
        startGeofence()

//        Toast.makeText(this,"Map was clicked.", Toast.LENGTH_LONG).show()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this,"Google Play services error.", Toast.LENGTH_SHORT).show()

    }

    // from code.tutsplus
    private fun markerForGeofence(latLng: LatLng) {
        val title = latLng.latitude.toString() +  ", " + latLng.longitude.toString()
        // Define marker options
        val markerOptions =  MarkerOptions()
                .position(latLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                .title(title)
        if ( gMap!=null ) {
            // Remove last geoFenceMarker
//            if (geoFenceMarker != null) {
//                geoFenceMarker.remove();
//            }
            geoFenceMarker = gMap.addMarker(markerOptions)
            gMap.addMarker(markerOptions)

        }
        drawGeofence()
    }

    // from code.tutsplus
    override fun onResult(status: Status) {
        if (status.isSuccess) {
            // Save geofence
            Log.d(TAG, "onResult: status: $status")
            drawGeofence()
//            startGeofence()
        } else {
            // Handle error
        }
    }

    private fun setupInitialGeofences() {

        val geofencesDatabaseRef = mDatabaseRef
                .child("users")
                .child(mUser!!.uid)
                .child("geofences")
                .addListenerForSingleValueEvent(object: ValueEventListener {

                    override fun onCancelled(p0: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }

                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        Log.d(TAG,"snapshot: \n$dataSnapshot")
                        val children = dataSnapshot.children

                        for (child in children) {
                            val value = child.getValue(AttendyGeofence::class.java)

                            val geofence = createGeofence(LatLng(value!!.lat!!, value.lng!!),
                                    value.radius!!)
                            val geofenceRequest = createGeofenceRequest(geofence)
                            Log.d(TAG,"OLD GEOFENCE REF: \n$geofenceRequest")

                            val latLng = LatLng(value.lat!!, value.lng!!)
                            markerForGeofence(latLng!!)
                            addGeofence(geofenceRequest)

                        }
                    }
                })


//        val geofence = createGeofence(geoFenceMarker.position, GEOFENCE_RADIUS.toFloat())
//        val geofenceRequest = createGeofenceRequest(geofence)
//        addGeofence(geofenceRequest)
//
//        saveGeofenceToDatabase(geofence, geoFenceMarker.position)

    }

    //TODO FIX THIS: geofencemarker is causing null pointer from lateinit.... never initialized
    // from code.tutsplus
    // Start Geofence creation process
    private fun startGeofence() {
        if (geoFenceMarker != null) {
            val geofence = createGeofence(geoFenceMarker.position, GEOFENCE_RADIUS.toFloat())
            val geofenceRequest = createGeofenceRequest(geofence)
            addGeofence(geofenceRequest)

            saveGeofenceToDatabase(geofence, geoFenceMarker.position)
        }
    }


    private fun saveGeofenceToDatabase(geofence: Geofence, latLng: LatLng) {


        mDatabaseRef.child("users").child(mUser!!.uid).addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val dataBaseRef = mDatabaseRef.child("users").child(mUser!!.uid).child("geofences").child(geofence.requestId)

                val newGeofence = AttendyGeofence(geofence.requestId,
                        latLng.latitude,
                        latLng.longitude,
                        GEOFENCE_RADIUS.toFloat(),
                        Geofence.NEVER_EXPIRE, GeofencingRequest.INITIAL_TRIGGER_ENTER, // May cause error
                        45)
                dataBaseRef.setValue(newGeofence)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    // from code.tutsplus
    // Create a Geofence
    private fun createGeofence(latLng: LatLng, radius: Float): Geofence {
        return Geofence.Builder()
                .setRequestId("${latLng.toDBstring()}")
                .setCircularRegion(latLng.latitude, latLng.longitude, radius)
                .setExpirationDuration(3_600_000)
                .setTransitionTypes( GeofencingRequest.INITIAL_TRIGGER_ENTER
                        or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                .setLoiteringDelay(45)
                .build()
    }

    private fun LatLng.toDBstring(): String {
        Log.d(TAG, "STRING: ${this.toString().replace('.','_')} ")

        val lat = this.latitude.toString().replace('.','_')
        val long = this.longitude.toString().replace('.','_')

        return "$lat, $long"
    }

    // from code.tutsplus
    // Create a Geofence Request
    private fun createGeofenceRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence( geofence )
                .build()
    }

    // from code.tutsplus
    // Add the created GeofenceRequest to the device's monitoring list
    private fun addGeofence(request: GeofencingRequest) {
        Log.d(TAG, "addGeofence")
        if ( checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this)
    }

    // from code.tutsplus
    private fun createGeofencePendingIntent(): PendingIntent{

//        if (geofencePendingIntent != null) {
//            return geofencePendingIntent
//        }

        val intent = Intent(this, GeofenceTransitionsIntentService::class.java)
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }


    private fun checkPermission(): Boolean {
        return(ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    // from code.tutsplus
    // Draw Geofence circle on GoogleMap
    private fun drawGeofence() {
//        if (geofenceLimits != null) {
//            geofenceLimits.remove()
//        }
        val circleOptions: CircleOptions = CircleOptions()
                .center(geoFenceMarker.position)
                .radius(GEOFENCE_RADIUS)
                .strokeColor(Color.argb(50,70,70,70))
                .fillColor(Color.argb(0,0,0,0))
        geofenceLimits = gMap.addCircle( circleOptions )
        gMap.addCircle(circleOptions)
    }



    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {

        private val TAG = "MainActivity"
        // Used to load the 'native-lib' library on application startup
        init {
            System.loadLibrary("native-lib")
        }

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"
        private val GEOFENCE_RADIUS: Double = 50.0 // in meters

        private val NOTIFICATION_MSG = "NOTIFICATION MSG"
        private val STATUS_MSG = "STATUS MSG"
        private var inGeofence: Boolean = false




        fun newIntent(context: Context, latLng: LatLng): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_LAT_LNG, latLng)
            return intent
        }

        fun makeNotificationIntent(context: Context, msg: String): Intent {
            var intent = Intent(context, MainActivity::class.java)
            intent.putExtra(NOTIFICATION_MSG, msg)
            return intent
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle presses on the action bar menu items
        when (item.itemId) {
            R.id.showmypunches -> {
                val intent1 = Intent(this, ShowMyPunches::class.java)
                startActivity(intent1)
                true
            }
            R.id.aboutus -> {
                val intent2 = Intent(this, AboutUs::class.java)
                startActivity(intent2)
                true
            }
            R.id.signout -> {
                FirebaseAuth.getInstance().signOut();
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }



        fun makeGeofenceStatusIntent(context: Context, msg: String): Intent {
            var intent = Intent(context, MainActivity::class.java)
            intent.putExtra(STATUS_MSG, msg)
            return intent
        }

        fun setInGeofence(bool: Boolean) {
            Log.d(TAG, "setInGeofence: $bool")
            inGeofence = bool
        }
    }
}
}