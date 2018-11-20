package com.attendy.attendy

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider


/**
 * TODO: Document
 * https://firebase.google.com/docs/auth/android/firebaseui?authuser=0
 */

class SignInActivity: AppCompatActivity(), View.OnClickListener, GoogleApiClient.OnConnectionFailedListener{
    private val REQUEST_CODE_SIGN_IN = 4242
    private val TAG = "SignInActivity"
    private lateinit var mGoogleApiClient: GoogleApiClient
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mSignInButton: SignInButton
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        mSignInButton = findViewById(R.id.sign_in_button)
        mSignInButton.setOnClickListener(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        mAuth = FirebaseAuth.getInstance()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Handle the result of the sign-in activity
        if (requestCode == REQUEST_CODE_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)

            if (result.isSuccess) {
                // TODO: fix the !! for acccount = result.signInAccount!!
                val account = result.signInAccount!!
                firebaseAuthWithGoogle(account)
            } else {
                Log.e(TAG, "Google Sign In failed")
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this) { task ->
                    Log.d(TAG, "signInCredential:onComplete:"+ task.isSuccessful)

                    if (!task.isSuccessful) {
                        Log.w(TAG, "signInWithCredential", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    } else {
                        startActivity(Intent( this, MainActivity::class.java))
                        finish()
                    }
                }
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.sign_in_button -> signIn()
            else -> {
                print("no on click")
            }
        }
    }

    private fun signIn() {
        val signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient)
        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.d(TAG, "onConnectionFailed$connectionResult")
        Toast.makeText(this,"Google Play Services error.", Toast.LENGTH_SHORT).show()
    }
}

