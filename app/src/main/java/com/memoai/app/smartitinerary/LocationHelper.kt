package com.memoai.app.smartitinerary

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

object LocationHelper {
    fun captureCurrentLocation(context: Context): Pair<Double, Double>? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val locations = providers.mapNotNull { provider ->
            runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
        }
        val best = locations.maxByOrNull { it.time } ?: return null
        return best.latitude to best.longitude
    }

    fun distanceMeters(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(aLat, aLng, bLat, bLng, results)
        return results[0]
    }

    fun hasMovedFromHome(
        context: Context,
        homeLat: Double?,
        homeLng: Double?,
        thresholdMeters: Float = 800f
    ): Boolean {
        if (homeLat == null || homeLng == null) return false
        val current = captureCurrentLocation(context) ?: return false
        return distanceMeters(homeLat, homeLng, current.first, current.second) >= thresholdMeters
    }
}
