package com.attendy.attendy

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.android.synthetic.main.activity_main.*


//TODO: Document map view --> http://www.zoftino.com/android-mapview-tutorial
// TODO: Fix nullibity of mUser and mAuth

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

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



    // from raywenderlich and zoftino
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mAuth = FirebaseAuth.getInstance()

        // elvis ?:
        mUser = mAuth!!.currentUser

        if (mUser == null) {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        } else {
            mUsername = mUser!!.displayName!!
            if (mUser!!.photoUrl != null) {
                mPhotoUrl = mUser!!.photoUrl.toString()
            }
        }

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this,this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()

        Toast.makeText(this,"$mUsername has logged in.", Toast.LENGTH_LONG).show()
        //TODO : Initialze progress bar and Recyvle view

        mapViewBundle = null;
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAP_VIEW_BUNDLE_KEY);
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
        createLocationRequest()
    }


    // from raywenderlich
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
//        gMap.setMinZoomPreference(15.0.toFloat())
//        val sfLatLng = LatLng(37.7219, -122.4782)
//
//        gMap.addMarker(MarkerOptions().position(sfLatLng).title("SFSU Map Marker"))
//        gMap.moveCamera(CameraUpdateFactory.newLatLng(sfLatLng))
//        gMap.moveCamera(CameraUpdateFactory.newLatLng(sfLatLng))

        gMap.getUiSettings().setZoomControlsEnabled(true)
        gMap.setOnMarkerClickListener(this)

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

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//    }

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


    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed:$connectionResult")
        Toast.makeText(this,"Google Play services error.", Toast.LENGTH_SHORT).show()

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


    }
}
