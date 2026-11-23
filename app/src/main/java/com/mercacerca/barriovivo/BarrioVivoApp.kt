package com.mercacerca.barriovivo

import android.app.Application
import com.mercacerca.barriovivo.data.seed.DatabaseSeeder
import com.mercacerca.barriovivo.data.sync.SincronizadorNube
import com.mercacerca.barriovivo.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File
import javax.inject.Inject

@HiltAndroidApp
class BarrioVivoApp : Application() {

    @Inject lateinit var seeder: DatabaseSeeder
    @Inject lateinit var sincronizador: SincronizadorNube
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        configurarOsmdroid()
        // Carga los 1,325 tianguis a Room en segundo plano; las pantallas observan Flows y se pintan solas.
        // Después, si hay nube, baja reseñas nuevas y sube lo pendiente (sin bloquear el arranque).
        appScope.launch {
            seeder.seedIfEmpty()
            sincronizador.sincronizar()
        }
    }

    private fun configurarOsmdroid() = Configuration.getInstance().apply {
        // La política de tiles de OSM exige un User-Agent que identifique la app.
        userAgentValue = BuildConfig.APPLICATION_ID
        // Caché de tiles en almacenamiento privado de la app: sin permiso de almacenamiento.
        osmdroidBasePath = File(cacheDir, "osmdroid")
        osmdroidTileCache = File(osmdroidBasePath, "tiles")
    }
}
