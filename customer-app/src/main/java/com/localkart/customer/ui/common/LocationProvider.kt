package com.localkart.customer.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.localkart.common.util.LocationUtil

/** Current user location snapshot for the UI. */
data class UserLoc(val lat: Double?, val lng: Double?, val area: String) {
    val hasLocation get() = lat != null && lng != null
}

/**
 * Resolves the device location (requesting permission once) and reverse-geocodes
 * the area name. Returns a snapshot that updates as permission/location resolve.
 */
@Composable
fun rememberUserLocation(): UserLoc {
    val ctx = LocalContext.current
    var loc by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var area by remember { mutableStateOf("Locating…") }

    fun refresh() {
        val l = LocationUtil.lastKnown(ctx)
        loc = l
        area = if (l != null) (LocationUtil.areaName(ctx, l.first, l.second) ?: "Current location")
        else "Set location"
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) refresh() else area = "Location off"
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) refresh() else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    return UserLoc(loc?.first, loc?.second, area)
}
