package com.example.pocmapa.ui.screens

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.view.animation.LinearInterpolator
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.example.pocmapa.R
import com.example.pocmapa.data.SampleFleet
import com.example.pocmapa.data.VehicleSnapshot
import com.example.pocmapa.data.VehicleStatus
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Estilo vetorial gratuito, sem chave, com dados do OpenStreetMap. */
const val MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

const val VEHICLE_LAYER = "vehicles"
private const val VEHICLE_SOURCE = "vehicles-source"
private const val ROUTE_LAYER = "route"
private const val ROUTE_SOURCE = "route-source"

const val FLEET_ZOOM = 13.5
private const val DRIVER_ZOOM = 16.8
private const val DRIVER_TILT = 55.0

/** Fração da altura do mapa usada como margem superior: põe o veículo na parte de baixo. */
private const val DRIVER_TOP_PADDING = 0.45
private const val CAMERA_TRANSITION_MILLIS = 1_000

/** Duração da animação entre dois ticks; igual ao intervalo do simulador para o movimento ser contínuo. */
private const val TICK_ANIMATION_MILLIS = 1_500L

/** Posição desenhada de um veículo (interpolada entre dois ticks). */
private data class Pose(val lat: Double, val lon: Double, val heading: Float) {
    fun lerp(to: Pose, f: Float): Pose {
        // Gira pelo menor ângulo (ex.: de 350° para 10° passa por 0°, não por 180°).
        val dHeading = ((to.heading - heading + 540f) % 360f) - 180f
        return Pose(
            lat + (to.lat - lat) * f,
            lon + (to.lon - lon) * f,
            (heading + dHeading * f + 360f) % 360f
        )
    }
}

/**
 * Desenha a frota no MapLibre: vans como símbolos (camada [VEHICLE_LAYER]), trajeto do veículo
 * seguido como linha e câmera em primeira pessoa no modo motorista.
 */
class FleetMapRenderer(
    context: Context,
    private val map: MapLibreMap,
    private val style: Style,
    private val mapHeightPx: () -> Int
) {
    private val vehicleSource = GeoJsonSource(VEHICLE_SOURCE)
    private val routeSource = GeoJsonSource(ROUTE_SOURCE)

    private var fleet: List<VehicleSnapshot> = emptyList()
    private var poses: Map<String, Pose> = emptyMap()
    private var animator: ValueAnimator? = null

    private var followedId: String? = null
    private var cameraTransitionUntil = 0L

    init {
        VehicleStatus.entries.forEach { style.addImage(iconName(it), vanBitmap(context, it)) }

        style.addSource(routeSource)
        style.addLayer(
            LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(RouteColor.toArgb()),
                PropertyFactory.lineWidth(9f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )

        style.addSource(vehicleSource)
        style.addLayer(
            SymbolLayer(VEHICLE_LAYER, VEHICLE_SOURCE).withProperties(
                PropertyFactory.iconImage(Expression.get("icon")),
                // icon-rotate é horário a partir do norte, igual ao rumo calculado.
                PropertyFactory.iconRotate(Expression.get("heading")),
                // "map": a van gira e inclina junto com o mapa, deitada sobre a rua.
                PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                PropertyFactory.iconAllowOverlap(true),
                PropertyFactory.iconIgnorePlacement(true)
            )
        )
    }

    /** Recebe um novo tick da simulação e anima as vans da posição atual até a nova. */
    fun update(newFleet: List<VehicleSnapshot>) {
        fleet = newFleet
        val from = poses
        val to = newFleet.associate {
            it.vehicle.id to Pose(it.telemetry.latitude, it.telemetry.longitude, it.heading)
        }
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = TICK_ANIMATION_MILLIS
            interpolator = LinearInterpolator()
            addUpdateListener { a ->
                val f = a.animatedValue as Float
                poses = to.mapValues { (id, target) -> from[id]?.lerp(target, f) ?: target }
                draw()
            }
            start()
        }
    }

    /** null volta para a visão da frota. */
    fun follow(vehicleId: String?) {
        if (vehicleId == followedId) return
        followedId = vehicleId
        // No modo motorista a câmera é controlada pelo app.
        map.uiSettings.setAllGesturesEnabled(vehicleId == null)
        val camera = poses[vehicleId]?.let(::driverCamera) ?: fleetCamera()
        cameraTransitionUntil = System.currentTimeMillis() + CAMERA_TRANSITION_MILLIS
        map.animateCamera(CameraUpdateFactory.newCameraPosition(camera), CAMERA_TRANSITION_MILLIS)
        draw()
    }

    fun release() {
        animator?.cancel()
    }

    private fun draw() {
        val statusById = fleet.associate { it.vehicle.id to it.status }
        vehicleSource.setGeoJson(
            FeatureCollection.fromFeatures(poses.map { (id, pose) ->
                Feature.fromGeometry(Point.fromLngLat(pose.lon, pose.lat)).apply {
                    addStringProperty("id", id)
                    addStringProperty("icon", iconName(statusById[id] ?: VehicleStatus.STOPPED))
                    addNumberProperty("heading", pose.heading)
                }
            })
        )

        val followed = fleet.find { it.vehicle.id == followedId }
        val pose = poses[followedId]
        if (followed == null || pose == null) {
            routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        // Linha começa na van (posição interpolada) e segue até a próxima parada.
        val route = listOf(Point.fromLngLat(pose.lon, pose.lat)) +
            followed.pathToNextStop().map { Point.fromLngLat(it.longitude, it.latitude) }
        routeSource.setGeoJson(LineString.fromLngLats(route))

        // Segue a van quadro a quadro, exceto durante a transição de entrada no modo motorista.
        if (System.currentTimeMillis() >= cameraTransitionUntil) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(driverCamera(pose)))
        }
    }

    private fun driverCamera(pose: Pose) = CameraPosition.Builder()
        .target(LatLng(pose.lat, pose.lon))
        .zoom(DRIVER_ZOOM)
        .bearing(pose.heading.toDouble())
        .tilt(DRIVER_TILT)
        .padding(0.0, mapHeightPx() * DRIVER_TOP_PADDING, 0.0, 0.0)
        .build()

    private fun fleetCamera(): CameraPosition {
        val (lat, lon) = SampleFleet.DISTRIBUTION_CENTER
        return CameraPosition.Builder()
            .target(LatLng(lat, lon))
            .zoom(FLEET_ZOOM)
            .bearing(0.0)
            .tilt(0.0)
            .padding(0.0, 0.0, 0.0, 0.0)
            .build()
    }

    private fun iconName(status: VehicleStatus) = "van-${status.name.lowercase()}"

    /** Carroceria na cor do status + detalhes fixos por cima. */
    private fun vanBitmap(context: Context, status: VehicleStatus) = LayerDrawable(
        arrayOf(
            ContextCompat.getDrawable(context, R.drawable.ic_van_body)!!.mutate().apply {
                setTint(status.color().toArgb())
            },
            ContextCompat.getDrawable(context, R.drawable.ic_van_details)!!
        )
    ).toBitmap()
}
