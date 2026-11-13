package de.codekorb.tankradar

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// Standort des Handys, ohne Google-Play-Dienste
object Locator {
    val PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun allowed(ctx: Context) = PERMISSIONS.any { ctx.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    // fresh = false: der letzte bekannte Standort reicht (Entfernungen zu den Favoriten)
    @SuppressLint("MissingPermission")
    suspend fun current(ctx: Context, fresh: Boolean = true): Location? {
        if (!allowed(ctx)) return null
        val lm = ctx.getSystemService(LocationManager::class.java)
        val enabled = lm.getProviders(true)
        val providers = listOf(LocationManager.FUSED_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER).filter { it in enabled }
        val last = providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (!fresh || providers.isEmpty() || last != null && System.currentTimeMillis() - last.time < 60_000) return last
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                val cancel = CancellationSignal()
                cont.invokeOnCancellation { cancel.cancel() }
                lm.getCurrentLocation(providers.first(), cancel, ctx.mainExecutor) { cont.resume(it) }
            }
        } ?: last
    }

    fun km(from: Location, s: Station): Double {
        val r = FloatArray(1)
        Location.distanceBetween(from.latitude, from.longitude, s.lat, s.lng, r)
        return r[0] / 1000.0
    }
}
