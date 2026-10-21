package br.edu.ifsp.ifrota.ui.map

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import br.edu.ifsp.ifrota.data.simulation.DeliveryTrip
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Red500
import br.edu.ifsp.ifrota.ui.theme.Surface1
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** MapView do MapLibre exige repassar todo o ciclo de vida, de onCreate a onDestroy. */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context).apply { onCreate(null) } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Ao trocar de tela a activity continua ativa: desfaz os estados antes de destruir.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            // Destruir com a view ainda na tela (ex.: durante a transição do NavHost) deixa a
            // thread de render desenhar mais um quadro com o mapa já liberado: crash nativo.
            // Por isso o onDestroy espera a view sair da janela.
            if (mapView.isAttachedToWindow) {
                mapView.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: View) = Unit
                    override fun onViewDetachedFromWindow(v: View) {
                        v.removeOnAttachStateChangeListener(this)
                        mapView.onDestroy()
                    }
                })
            } else {
                mapView.onDestroy()
            }
        }
    }
    return mapView
}

/**
 * Mapa estático com um trecho da rota: linha da origem ao destino, origem em verde e
 * destino em vermelho, como a prévia do protótipo.
 */
@Composable
fun LegPreviewMap(trip: DeliveryTrip, modifier: Modifier = Modifier) {
    val mapView = rememberMapViewWithLifecycle()
    val paddingPx = with(LocalDensity.current) { 28.dp.roundToPx() }

    DisposableEffect(mapView, trip) {
        mapView.getMapAsync { map ->
            map.uiSettings.setAllGesturesEnabled(false)
            map.uiSettings.isCompassEnabled = false
            map.uiSettings.isLogoEnabled = false
            map.uiSettings.isAttributionEnabled = false
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                val points = trip.points.map { Point.fromLngLat(it.longitude, it.latitude) }
                style.addSource(GeoJsonSource("leg-source", LineString.fromLngLats(points)))
                style.addLayer(
                    LineLayer("leg", "leg-source").withProperties(
                        PropertyFactory.lineColor(Green600.toArgb()),
                        PropertyFactory.lineWidth(3.5f),
                        PropertyFactory.lineDasharray(arrayOf(2f, 1.2f)),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
                    )
                )
                style.addSource(
                    GeoJsonSource(
                        "leg-ends",
                        FeatureCollection.fromFeatures(
                            listOf(
                                Feature.fromGeometry(points.first()).apply { addStringProperty("end", "from") },
                                Feature.fromGeometry(points.last()).apply { addStringProperty("end", "to") }
                            )
                        )
                    )
                )
                style.addLayer(
                    CircleLayer("leg-ends", "leg-ends").withProperties(
                        PropertyFactory.circleRadius(7f),
                        PropertyFactory.circleColor(
                            Expression.match(
                                Expression.get("end"),
                                Expression.color(Red500.toArgb()),
                                Expression.stop(
                                    "from", Expression.color(Green600.toArgb())
                                )
                            )
                        ),
                        PropertyFactory.circleStrokeWidth(2f),
                        PropertyFactory.circleStrokeColor(Surface1.toArgb())
                    )
                )
                val bounds = LatLngBounds.Builder()
                    .includes(trip.points.map { LatLng(it.latitude, it.longitude) })
                    .build()
                map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, paddingPx))
            }
        }
        onDispose {}
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}
