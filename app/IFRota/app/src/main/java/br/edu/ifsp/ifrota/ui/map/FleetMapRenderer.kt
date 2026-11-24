package br.edu.ifsp.ifrota.ui.map

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import br.edu.ifsp.ifrota.R
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.geofencing.DeliveryGeofences
import br.edu.ifsp.ifrota.data.simulation.Geo
import br.edu.ifsp.ifrota.data.tracking.DriverTracker
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.TrackedDriver
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

private const val FLEET_SOURCE = "fleet-source"
private const val FLEET_FENCES_SOURCE = "fleet-fences-source"
private const val FLEET_STOPS_SOURCE = "fleet-stops-source"

/** Um pouco menos que o intervalo de envio: a van chega antes da próxima posição. */
private const val FLEET_ANIMATION_MILLIS = DriverTracker.MOVING_INTERVAL_MILLIS - 500L
private const val FOCUS_ZOOM = 15.5

private enum class FleetIcon(val color: Color) {
    MOVING(Green600), STOPPED(Amber500), OFFLINE(Text3);

    val imageName: String get() = "fleet-${name.lowercase()}"
}

private data class FleetPose(val lat: Double, val lon: Double, val heading: Float) {
    fun lerp(to: FleetPose, f: Float): FleetPose {
        val dHeading = ((to.heading - heading + 540f) % 360f) - 180f
        return FleetPose(lat + (to.lat - lat) * f, lon + (to.lon - lon) * f, (heading + dHeading * f + 360f) % 360f)
    }
}

/**
 * Mapa da frota para o administrador: uma van por entregador (verde andando, âmbar parada,
 * cinza offline) com a placa, o CD, o destino atual de cada um e os perímetros monitorados.
 * Entre duas posições recebidas, a van desliza até a nova.
 */
class FleetMapRenderer(
    context: Context,
    private val map: MapLibreMap,
    style: Style,
    private val paddingPx: IntArray
) {
    private val fleetSource = GeoJsonSource(FLEET_SOURCE)
    private val fenceSource = GeoJsonSource(FLEET_FENCES_SOURCE)
    private val stopsSource = GeoJsonSource(FLEET_STOPS_SOURCE)

    private var drivers: List<TrackedDriver> = emptyList()
    private var poses: Map<String, FleetPose> = emptyMap()
    private var targets: Map<String, FleetPose> = emptyMap()
    private var animator: ValueAnimator? = null
    private var framed = false

    init {
        FleetIcon.entries.forEach { style.addImage(it.imageName, vanBitmap(context, it.color)) }

        style.addSource(fenceSource)
        style.addLayer(
            FillLayer("fleet-fences-fill", FLEET_FENCES_SOURCE).withProperties(
                PropertyFactory.fillColor(Green600.toArgb()),
                PropertyFactory.fillOpacity(0.08f)
            )
        )
        style.addLayer(
            LineLayer("fleet-fences-outline", FLEET_FENCES_SOURCE).withProperties(
                PropertyFactory.lineColor(Green600.toArgb()),
                PropertyFactory.lineWidth(1.5f),
                PropertyFactory.lineOpacity(0.6f),
                PropertyFactory.lineDasharray(arrayOf(3f, 2f))
            )
        )

        style.addSource(stopsSource)
        style.addLayer(
            CircleLayer("fleet-stops", FLEET_STOPS_SOURCE).withProperties(
                PropertyFactory.circleRadius(11f),
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get("kind"), Expression.color(Green600.toArgb()),
                        Expression.stop("cd", Expression.color(Green900.toArgb()))
                    )
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Surface1.toArgb())
            )
        )
        style.addLayer(
            SymbolLayer("fleet-stop-labels", FLEET_STOPS_SOURCE).withProperties(
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(10f),
                PropertyFactory.textColor(Surface1.toArgb()),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )

        style.addSource(fleetSource)
        style.addLayer(
            SymbolLayer("fleet-vans", FLEET_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconRotate(Expression.get("heading")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true),
                PropertyFactory.textField(Expression.get("plate")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(11f),
                PropertyFactory.textColor(Text1.toArgb()),
                PropertyFactory.textHaloColor(Surface1.toArgb()),
                PropertyFactory.textHaloWidth(1.5f),
                PropertyFactory.textOffset(arrayOf(0f, 2.2f)),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )
    }

    fun update(newDrivers: List<TrackedDriver>) {
        drivers = newDrivers
        drawStops()

        val newTargets = newDrivers.associate {
            it.id to FleetPose(it.location.latitude, it.location.longitude, it.location.heading)
        }
        if (newTargets != targets) {
            targets = newTargets
            val from = poses
            animator?.cancel()
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = FLEET_ANIMATION_MILLIS
                interpolator = LinearInterpolator()
                addUpdateListener { a ->
                    val f = a.animatedValue as Float
                    poses = newTargets.mapValues { (id, target) -> from[id]?.lerp(target, f) ?: target }
                    drawVans()
                }
                start()
            }
        } else {
            // Só mudou o status (ex.: ficou offline): redesenha as cores.
            drawVans()
        }

        if (!framed && newDrivers.isNotEmpty()) {
            framed = true
            frameAll(animate = false)
        }
    }

    /** Enquadra todos os entregadores e o CD. */
    fun frameAll(animate: Boolean = true) {
        val points = drivers.map { LatLng(it.location.latitude, it.location.longitude) } +
            LatLng(DistributionCenter.LATITUDE, DistributionCenter.LONGITUDE)
        val camera = if (points.distinct().size >= 2) {
            map.getCameraForLatLngBounds(LatLngBounds.Builder().includes(points).build(), paddingPx, 0.0, 0.0)
        } else {
            null
        } ?: CameraPosition.Builder().target(points.first()).zoom(OVERVIEW_ZOOM).build()
        val update = CameraUpdateFactory.newCameraPosition(camera)
        if (animate) map.animateCamera(update, 800) else map.moveCamera(update)
    }

    fun focus(driverId: String) {
        val pose = poses[driverId] ?: targets[driverId] ?: return
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.Builder().target(LatLng(pose.lat, pose.lon)).zoom(FOCUS_ZOOM).bearing(0.0).tilt(0.0).build()
            ),
            800
        )
    }

    fun release() {
        animator?.cancel()
    }

    private fun drawVans() {
        val byId = drivers.associateBy { it.id }
        vanFeatures(byId).let { fleetSource.setGeoJson(FeatureCollection.fromFeatures(it)) }
    }

    private fun vanFeatures(byId: Map<String, TrackedDriver>): List<Feature> =
        poses.mapNotNull { (id, pose) ->
            val driver = byId[id] ?: return@mapNotNull null
            val icon = when {
                !driver.online -> FleetIcon.OFFLINE
                driver.location.speedKmh > 0 -> FleetIcon.MOVING
                else -> FleetIcon.STOPPED
            }
            Feature.fromGeometry(Point.fromLngLat(pose.lon, pose.lat)).apply {
                addStringProperty("icon", icon.imageName)
                addNumberProperty("heading", pose.heading)
                addStringProperty("plate", driver.location.plate.ifBlank { driver.name })
            }
        }

    /** CD e o destino atual de cada entregador, com os perímetros que disparam os avisos. */
    private fun drawStops() {
        val (cdLat, cdLon) = DistributionCenter.position
        val stops = mutableListOf(
            Feature.fromGeometry(Point.fromLngLat(cdLon, cdLat)).apply {
                addStringProperty("label", "CD")
                addStringProperty("kind", "cd")
            }
        )
        val fences = mutableListOf(circle(cdLat, cdLon, DeliveryGeofences.CD_RADIUS_METERS))
        drivers.forEach { driver ->
            val lat = driver.location.deliveryLatitude ?: return@forEach
            val lon = driver.location.deliveryLongitude ?: return@forEach
            stops += Feature.fromGeometry(Point.fromLngLat(lon, lat)).apply {
                addStringProperty("label", driver.location.deliveryOrder?.toString().orEmpty())
                addStringProperty("kind", "delivery")
            }
            fences += circle(lat, lon, DeliveryGeofences.DELIVERY_RADIUS_METERS)
        }
        stopsSource.setGeoJson(FeatureCollection.fromFeatures(stops))
        fenceSource.setGeoJson(FeatureCollection.fromFeatures(fences.map { Feature.fromGeometry(it) }))
    }

    private fun circle(lat: Double, lon: Double, radiusMeters: Double): Polygon {
        val ring = (0..64).map { i ->
            val (pLat, pLon) = Geo.offset(lat, lon, i * 360f / 64, radiusMeters)
            Point.fromLngLat(pLon, pLat)
        }
        return Polygon.fromLngLats(listOf(ring))
    }

    private fun vanBitmap(context: Context, color: Color) = LayerDrawable(
        arrayOf(
            ContextCompat.getDrawable(context, R.drawable.ic_van_body)!!.mutate().apply { setTint(color.toArgb()) },
            ContextCompat.getDrawable(context, R.drawable.ic_van_details)!!
        )
    ).toBitmap()
}
