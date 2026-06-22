package com.localkart.seller

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.localkart.common.auth.AuthManager
import com.localkart.common.model.User
import com.localkart.common.model.UserRole
import com.localkart.common.ui.theme.LocalKartTheme
import com.localkart.seller.nav.SellerRoot
import com.localkart.seller.ui.common.SellerAuthScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LocalKartTheme(seller = true) {
                val context = LocalContext.current
                val scope = rememberCoroutineScope()
                var user by remember { mutableStateOf<User?>(null) }
                var checking by remember { mutableStateOf(AuthManager.isLoggedIn) }
                var pendingRole by remember { mutableStateOf(UserRole.STORE_OWNER) }

                // Return to login when the user logs out from anywhere in the app.
                DisposableEffect(Unit) {
                    val stop = AuthManager.addAuthListener { signedIn ->
                        if (!signedIn) { user = null; checking = false }
                    }
                    onDispose { stop() }
                }

                // Notification permission (Android 13+) + register FCM token once signed in.
                val notifPerm = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { }
                LaunchedEffect(user != null) {
                    if (user != null) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        com.localkart.common.notify.PushTokens.register()
                    }
                }

                val webClientId = remember {
                    val id = context.resources.getIdentifier(
                        "default_web_client_id", "string", context.packageName
                    )
                    if (id != 0) context.getString(id) else ""
                }

                // If already signed in from a previous session, load the saved role.
                LaunchedEffect(Unit) {
                    if (AuthManager.isLoggedIn) {
                        user = AuthManager.currentUser()
                        checking = false
                    }
                }

                val launcher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    scope.launch {
                        val res = AuthManager.handleResult(result.data, pendingRole)
                        if (res.isSuccess) {
                            user = res.getOrNull()
                        } else {
                            Toast.makeText(
                                context,
                                "Sign-in failed: ${res.exceptionOrNull()?.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                when {
                    user != null -> SellerRoot(role = user!!.role)
                    checking -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    else -> SellerAuthScreen(
                        onGoogleSignIn = { role ->
                            if (webClientId.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    "Enable Google sign-in in Firebase, then re-download google-services.json",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                pendingRole = role
                                launcher.launch(AuthManager.googleSignInIntent(context, webClientId))
                            }
                        }
                    )
                }
            }
        }
    }
}
