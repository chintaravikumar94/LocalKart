package com.localkart.common.util

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import java.util.Locale

/** Lightweight location helpers (no extra dependencies). */
object LocationUtil {

    /** Best last-known location from enabled providers, or null (no permission / unavailable). */
    fun lastKnown(context: Context): Pair<Double, Double>? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var best: Location? = null
            for (p in lm.getProviders(true)) {
                val l = lm.getLastKnownLocation(p) ?: continue
                if (best == null || l.accuracy < best!!.accuracy) best = l
            }
            best?.let { it.latitude to it.longitude }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /** Great-circle distance in km between two lat/lng points (Haversine). */
    fun distanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    /** Best-effort area/locality name for coordinates (or null). */
    fun areaName(context: Context, lat: Double, lon: Double): String? {
        return try {
            val a = Geocoder(context, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()
            a?.subLocality ?: a?.locality ?: a?.subAdminArea
        } catch (e: Exception) {
            null
        }
    }
}
