package com.localkart.customer.ui.common

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.localkart.common.util.LocationUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** App-wide selected location (device-detected or manually chosen). Observable. */
object LocationStore {
    var lat by mutableStateOf<Double?>(null)
    var lng by mutableStateOf<Double?>(null)
    var area by mutableStateOf("Set location")
    val hasLocation get() = lat != null && lng != null
    fun set(la: Double, ln: Double, ar: String) { lat = la; lng = ln; area = ar }
}

data class UserLoc(val lat: Double?, val lng: Double?, val area: String) {
    val hasLocation get() = lat != null && lng != null
}

/** Detects device location once into LocationStore, then returns the current value. */
@Composable
fun rememberUserLocation(): UserLoc {
    val ctx = LocalContext.current
    fun detect() {
        val l = LocationUtil.lastKnown(ctx) ?: return
        LocationStore.set(l.first, l.second, LocationUtil.areaName(ctx, l.first, l.second) ?: "Current location")
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { if (it) detect() }
    LaunchedEffect(Unit) {
        if (LocationStore.hasLocation) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) detect() else launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    return UserLoc(LocationStore.lat, LocationStore.lng, LocationStore.area)
}

/** Location bar + radius selector + pro "change location" bottom sheet. */
@Composable
fun LocationBarPro(radiusKm: Int, onRadiusSelected: (Int) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    LocationBar(LocationStore.area, radiusKm, onChangeLocation = { showSheet = true }, onRadiusSelected = onRadiusSelected)
    if (showSheet) ChangeLocationSheet(onDismiss = { showSheet = false })
}

private val popularCities = listOf("Hyderabad", "Vijayawada", "Visakhapatnam", "Amalapuram", "Bengaluru", "Chennai")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeLocationSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    var query by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun useCurrent() {
        LocationUtil.lastKnown(ctx)?.let {
            LocationStore.set(it.first, it.second, LocationUtil.areaName(ctx, it.first, it.second) ?: "Current location")
            onDismiss()
        } ?: run { error = "Turn on location to use current" }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useCurrent() else error = "Location permission needed"
    }
    fun pick(name: String) {
        scope.launch {
            busy = true; error = null
            val r = withContext(Dispatchers.IO) { LocationUtil.geocode(ctx, name) }
            busy = false
            if (r != null) { LocationStore.set(r.first, r.second, r.third); onDismiss() }
            else error = "Couldn't find \"$name\""
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 28.dp)) {
            Text("Choose your location", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("Currently: ${LocationStore.area}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            FilledTonalButton(
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) useCurrent() else permLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("Use my current location")
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search area, city or pincode") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                trailingIcon = { if (query.isNotBlank()) TextButton(onClick = { pick(query.trim()) }) { Text("Go") } }
            )

            Spacer(Modifier.height(16.dp))
            Text("Popular", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularCities) { c -> AssistChip(onClick = { pick(c) }, label = { Text(c) }) }
            }

            if (busy) { Spacer(Modifier.height(14.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            error?.let { Spacer(Modifier.height(10.dp)); Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
