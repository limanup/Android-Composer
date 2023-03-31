package edu.utap.limanup.androidcomposer.auth

import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirestoreAuthLiveData : LiveData<FirebaseUser?>() {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val authStateListener = FirebaseAuth.AuthStateListener {
        value = firebaseAuth.currentUser
    }

    fun updateUser() {
        value = firebaseAuth.currentUser
        Log.d(javaClass.simpleName, "XXX updated user to ${value?.displayName}")
    }
    fun getCurrentUser() : FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun signOutUser() {
        firebaseAuth.signOut()
        Log.d(javaClass.simpleName, "XXX user signed out.")
    }

    override fun onActive() {
        super.onActive()
        firebaseAuth.addAuthStateListener(authStateListener)
    }

    override fun onInactive() {
        super.onInactive()
        firebaseAuth.removeAuthStateListener(authStateListener)
    }
}