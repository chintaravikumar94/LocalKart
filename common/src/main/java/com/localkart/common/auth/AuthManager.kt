package com.localkart.common.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.localkart.common.model.User
import com.localkart.common.model.UserRole
import kotlinx.coroutines.tasks.await

/**
 * Wraps Firebase Google Sign-In + user profile bootstrap in Firestore.
 * Usage:
 *   val intent = AuthManager.googleSignInIntent(context, webClientId)
 *   // launch intent, then onResult -> AuthManager.handleResult(data, role)
 */
object AuthManager {

    val currentUid: String? get() = Firebase.auth.currentUser?.uid
    val isLoggedIn: Boolean get() = Firebase.auth.currentUser != null

    fun googleSignInIntent(context: Context, webClientId: String): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)   // from google-services.json / Firebase console
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso).signInIntent
    }

    /** Exchange the Google account for a Firebase credential and ensure a user doc exists. */
    suspend fun handleResult(data: Intent?, role: UserRole): Result<User> = runCatching {
        val account = GoogleSignIn.getSignedInAccountFromIntent(data).await()
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        val authResult = Firebase.auth.signInWithCredential(credential).await()
        val fbUser = authResult.user ?: error("No Firebase user")

        val ref = Firebase.firestore.collection("users").document(fbUser.uid)
        val existing = ref.get().await()
        val user = if (existing.exists()) {
            existing.toObject(User::class.java)!!
        } else {
            User(
                uid = fbUser.uid,
                name = fbUser.displayName ?: "",
                email = fbUser.email ?: "",
                photoUrl = fbUser.photoUrl?.toString() ?: "",
                role = role
            ).also { ref.set(it).await() }
        }
        user
    }

    /** Loads the currently signed-in user's profile doc (or null). */
    suspend fun currentUser(): User? {
        val uid = currentUid ?: return null
        return runCatching {
            Firebase.firestore.collection("users").document(uid).get().await()
                .toObject(User::class.java)
        }.getOrNull()
    }

    fun signOut() = Firebase.auth.signOut()
}
