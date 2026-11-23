package com.mercacerca.barriovivo.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.mercacerca.barriovivo.core.geo.LatLon
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

interface LocationProvider {
    fun tienePermiso(): Boolean

    /** Ubicación actual, o null si no hay permiso / GPS apagado / error. Nunca lanza. */
    suspend fun ubicacionActual(): LatLon?
}

class FusedLocationProvider(
    private val context: Context,
    private val client: FusedLocationProviderClient,
) : LocationProvider {

    override fun tienePermiso(): Boolean = PERMISOS.any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission") // Se revisa en tienePermiso() justo antes.
    override suspend fun ubicacionActual(): LatLon? {
        if (!tienePermiso()) return null
        return try {
            val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
                ?: client.lastLocation.await()
            loc?.let { LatLon(it.latitude, it.longitude) }
        } catch (e: CancellationException) {
            throw e // nunca tragarse la cancelación de la corrutina
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        val PERMISOS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )
    }
}
