package com.handytools.commongooglesigninauth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), View.OnClickListener {


    companion object {
        private val TAG = "GoogleActivity"
        private val RC_SIGN_IN = 9001
    }

    private var mAuth: FirebaseAuth? = null
    internal lateinit var mGoogleSignInClient: GoogleSignInClient


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sign_in_button.setOnClickListener(this)
        sign_in_button.setSize(SignInButton.SIZE_WIDE)
        sign_out_button.setOnClickListener(this)
        disconnect_button.setOnClickListener(this)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        mAuth = FirebaseAuth.getInstance()

    }

    public override fun onStart() {

        super.onStart()

        val currentUser = mAuth!!.currentUser

        if(currentUser!=null){
            Log.d(TAG,"Currently signed in: " + currentUser.email!!)
            Toast.makeText(this@MainActivity, "Google logged in: "+ currentUser.email!!, Toast.LENGTH_LONG).show()
        }

    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == RC_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                Toast.makeText(this,"Google sign in succeeded",Toast.LENGTH_LONG).show()
                firebaseAuthWithGoogle(account!!)
            } catch (e: ApiException){
                Log.w(TAG,"Google sign in failed", e)
                Toast.makeText(this,"Google sign in failed $e",Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onClick(v: View) {

        when(v.id){
            sign_in_button.id -> signInToGoogle()
            sign_out_button.id -> signOutFromGoogle()
            disconnect_button.id -> revokeAccess()
        }

    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {

        Log.d(TAG,"firebaseAuthWithGoogle: " + acct.id!!)

        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        mAuth!!.signInWithCredential(credential).addOnCompleteListener(this){ task ->
            if(task.isSuccessful){
                val user = mAuth!!.currentUser
                Log.d(TAG,"(m) signInWithCredential: success: currentUser: "+user!!.email!!)
                Toast.makeText(this@MainActivity,"Firebase authentication succeeded", Toast.LENGTH_LONG).show()
            } else{
                Log.w(TAG,"(m) signInWithCredential: failure")
                Toast.makeText(this@MainActivity,"Firebase authentication failed", Toast.LENGTH_LONG).show()
            }

        }

    }

    fun signInToGoogle() {

        val signInIntent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)

    }

    private fun signOutFromGoogle(){

        mAuth!!.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this)
        {
            Log.w(TAG, "Signed ouf of Google")
        }

    }

    private fun revokeAccess(){
        mAuth!!.signOut()

        mGoogleSignInClient.revokeAccess().addOnCompleteListener(this)
        {
            Log.w(TAG, "Revoked Access")
        }
    }


}