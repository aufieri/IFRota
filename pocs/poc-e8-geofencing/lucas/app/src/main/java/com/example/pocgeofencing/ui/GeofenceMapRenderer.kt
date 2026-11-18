package com.example.pocgeofencing.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.pocgeofencing.R
import com.example.pocgeofencing.data.Geo
import com.example.pocgeofencing.data.Geofence
import com.example.pocgeofencing.data.VehicleStatus
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon

/** Estilo vetorial gratuito, sem chave, com dados do OpenStreetMap. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
const val FLEET_ZOOM = 13.5

private const val VEHICLE_SOURCE = "vehicles-source"
private const val FENCE_SOURCE = "fences-source"
private const val TICK_ANIMATION_MILLIS = 1_500L

val InTransitColor = Color(0xFF2E7D32)
val StoppedColor = Color(0xFFC62828)
val FenceIdleColor = Color(0xFF1E88E5)
val FenceOccupiedColor = Color(0xFFF57C00)

fun VehicleStatus.color() = if (this == VehicleStatus.IN_TRANSIT) InTransitColor else StoppedColor

private data class Pose(val lat: Double, val lon: Double, val heading: Float) {
    fun lerp(to: Pose, f: Float): Pose {
        val dHeading = ((to.heading - heading + 540f) % 360f) - 180f
        return Pose(lat + (to.lat - lat) * f, lon + (to.lon - lon) * f, (heading + dHeading * f + 360f) % 360f)
    }
}

/**
 * Desenha os perímetros (círculos em metros, laranja quando há veículo dentro) e as vans,
 * animadas entre um tick e outro.
 */
class GeofenceMapRenderer(context: Context, style: Style) {

    private val vehicleSource = GeoJsonSource(VEHICLE_SOURCE)
    private val fenceSource = GeoJsonSource(FENCE_SOURCE)
    private var poses: Map<String, Pose> = emptyMap()
    private var statuses: Map<String, VehicleStatus> = emptyMap()
    private var animator: ValueAnimator? = null

    init {
        VehicleStatus.entries.forEach { style.addImage(iconName(it), vanBitmap(context, it)) }

        val fenceColor = Expression.switchCase(
            Expression.get("occupied"), Expression.color(FenceOccupiedColor.toArgb()),
            Expression.color(FenceIdleColor.toArgb())
        )
        style.addSource(fenceSource)
        style.addLayer(
            FillLayer("fences-fill", FENCE_SOURCE).withProperties(
                PropertyFactory.fillColor(fenceColor),
                PropertyFactory.fillOpacity(0.18f)
            )
        )
        style.addLayer(
            LineLayer("fences-outline", FENCE_SOURCE).withProperties(
                PropertyFactory.lineColor(fenceColor),
                PropertyFactory.lineWidth(2f)
            )
        )
        style.addLayer(
            SymbolLayer("fences-label", FENCE_SOURCE).withProperties(
                PropertyFactory.textField(Expression.get("name")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(11f),
                PropertyFactory.textColor(fenceColor),
                PropertyFactory.textHaloColor(android.graphics.Color.WHITE),
                PropertyFactory.textHaloWidth(1.5f)
            )
        )

        style.addSource(vehicleSource)
        style.addLayer(
            SymbolLayer("vehicles", VEHICLE_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                PropertyFactory.iconRotate(Expression.get("heading")),
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    fun update(state: GeofenceUiState) {
        fenceSource.setGeoJson(
            FeatureCollection.fromFeatures(state.geofences.map { fence ->
                Feature.fromGeometry(circle(fence)).apply {
                    addStringProperty("name", fence.name)
                    addBooleanProperty("occupied", fence.id in state.occupied)
                }
            })
        )

        statuses = state.fleet.associate { it.vehicle.id to it.status }
        val from = poses
        val to = state.fleet.associate {
            it.vehicle.id to Pose(it.telemetry.latitude, it.telemetry.longitude, it.heading)
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TICK_ANIMATION_MILLIS
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                poses = to.mapValues { (id, target) -> from[id]?.lerp(target, f) ?: target }
                drawVehicles()
            }
            start()
        }
    }

    fun release() {
        animator?.cancel()
    }

    private fun drawVehicles() {
        vehicleSource.setGeoJson(
            FeatureCollection.fromFeatures(poses.map { (id, pose) ->
                Feature.fromGeometry(Point.fromLngLat(pose.lon, pose.lat)).apply {
                    addStringProperty("icon", iconName(statuses[id] ?: VehicleStatus.STOPPED))
                    addNumberProperty("heading", pose.heading)
                }
            })
        )
    }

    /** Polígono de 64 lados aproximando o círculo do perímetro (raio em metros, não em pixels). */
    private fun circle(fence: Geofence): Polygon {
        val ring = (0..64).map { i ->
            val (lat, lon) = Geo.offset(fence.latitude, fence.longitude, i * 360f / 64, fence.radiusMeters)
            Point.fromLngLat(lon, lat)
        }
        return Polygon.fromLngLats(listOf(ring))
    }

    private fun iconName(status: VehicleStatus) = "van-${status.name.lowercase()}"

    private fun vanBitmap(context: Context, status: VehicleStatus) = LayerDrawable(
        arrayOf(
            ContextCompat.getDrawable(context, R.drawable.ic_van_body)!!.mutate().apply {
                setTint(status.color().toArgb())
            },
            ContextCompat.getDrawable(context, R.drawable.ic_van_details)!!
        )
    ).toBitmap()
}
