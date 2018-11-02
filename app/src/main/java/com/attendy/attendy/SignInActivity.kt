package com.attendy.attendy

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth


/**
 * TODO: Document
 * https://firebase.google.com/docs/auth/android/firebaseui?authuser=0
 *
 *
 *
 */


abstract class SignInActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin_ui)
    }

    private fun createSignInIntent() {
        // [START auth_fui_create_intent]
        // Choose authentication providers
        val providers = arrayListOf(
//                AuthUI.IdpConfig.EmailBuilder().build(),
//                AuthUI.IdpConfig.PhoneBuilder().build(),
                AuthUI.IdpConfig.GoogleBuilder().build()
//                AuthUI.IdpConfig.FacebookBuilder().build(),
//                AuthUI.IdpConfig.TwitterBuilder().build()
        )

        // Create and launch sign-in intent
        startActivityForResult(
                AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),
                RC_SIGN_IN)
        // [END auth_fui_create_intent]
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }


    companion object {

        private const val RC_SIGN_IN = 7
    }

}
