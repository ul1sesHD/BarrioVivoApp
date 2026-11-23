package com.mercacerca.barriovivo.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.domain.model.Tianguis
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private fun LatLon.toGeoPoint() = GeoPoint(lat, lon)

/**
 * Mapa de OpenStreetMap (osmdroid) envuelto para Compose. Sin API key ni facturación.
 * Recibe estado y avisa clics; no guarda lógica propia.
 */
@Composable
fun OsmMap(
    tianguis: List<Tianguis>,
    miUbicacion: LatLon?,
    camara: OrdenCamara?,
    onTianguisClick: (Tianguis) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val alClic by rememberUpdatedState(onTianguisClick)
    val pin = remember { ContextCompat.getDrawable(context, R.drawable.ic_pin_tianguis) }
    val puntoAzul = remember { ContextCompat.getDrawable(context, R.drawable.ic_mi_ubicacion) }

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            minZoomLevel = 9.0
            maxZoomLevel = 19.0
            controller.setZoom(MapViewModel.ZOOM_CIUDAD)
            controller.setCenter(Geo.ZOCALO.toGeoPoint())
        }
    }

    // osmdroid necesita saber cuándo la pantalla se pausa/reanuda (descarga de tiles, sensores).
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    LaunchedEffect(tianguis) {
        mapView.overlays.removeAll { it is Marker && it.id != ID_YO }
        tianguis.forEach { t ->
            val punto = t.coordenadas ?: return@forEach // sin coordenadas: no se pinta
            mapView.overlays += Marker(mapView).apply {
                position = punto.toGeoPoint()
                icon = pin
                title = t.titulo
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ -> alClic(t); true }
            }
        }
        mapView.invalidate()
    }

    LaunchedEffect(miUbicacion) {
        mapView.overlays.removeAll { it is Marker && it.id == ID_YO }
        miUbicacion?.let {
            mapView.overlays += Marker(mapView).apply {
                id = ID_YO
                position = it.toGeoPoint()
                icon = puntoAzul
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                setInfoWindow(null)
            }
        }
        mapView.invalidate()
    }

    LaunchedEffect(camara?.id) {
        camara?.let { mapView.controller.animateTo(it.destino.toGeoPoint(), it.zoom, ANIMACION_MS) }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private const val ID_YO = "yo"
private const val ANIMACION_MS = 800L
