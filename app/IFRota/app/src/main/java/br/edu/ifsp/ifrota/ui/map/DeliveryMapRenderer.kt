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
import br.edu.ifsp.ifrota.data.simulation.DeliveryRoutes
import br.edu.ifsp.ifrota.data.simulation.DeliveryTrip
import br.edu.ifsp.ifrota.data.simulation.VehicleStatus
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryPhase
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryUiState
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
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

const val OVERVIEW_ZOOM = 14.0
private const val DRIVER_ZOOM = 16.8
private const val DRIVER_TILT = 55.0

/** Fração da altura do mapa usada como margem superior: põe o veículo na parte de baixo. */
private const val DRIVER_TOP_PADDING = 0.45
private const val CAMERA_TRANSITION_MILLIS = 1_000

private const val VEHICLE_SOURCE = "vehicle-source"
private const val ROUTE_SOURCE = "route-source"
private const val DESTINATION_SOURCE = "destination-source"
private const val STOPS_SOURCE = "stops-source"

private const val KIND_CD = "cd"
private const val KIND_DONE = "done"
private const val KIND_CURRENT = "current"
private const val KIND_PENDING = "pending"

fun VehicleStatus.color(): Color = if (this == VehicleStatus.IN_TRANSIT) Green600 else Amber500

/** Posição desenhada do veículo (interpolada entre dois ticks). */
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
 * Desenha a entrega no MapLibre: o veículo, o trecho que falta até o destino e o destino.
 * Na navegação a câmera acompanha o veículo em primeira pessoa; nas demais fases fica
 * vista de cima e o mapa pode ser explorado livremente.
 *
 * [overviewPaddingPx] reserva espaço (esquerda, topo, direita, base) para os controles
 * sobre o mapa ao enquadrar a rota.
 */
class DeliveryMapRenderer(
    context: Context,
    private val map: MapLibreMap,
    style: Style,
    private val mapHeightPx: () -> Int,
    private val overviewPaddingPx: IntArray
) {
    private val vehicleSource = GeoJsonSource(VEHICLE_SOURCE)
    private val routeSource = GeoJsonSource(ROUTE_SOURCE)
    private val destinationSource = GeoJsonSource(DESTINATION_SOURCE)
    private val stopsSource = GeoJsonSource(STOPS_SOURCE)

    private var state: DeliveryUiState? = null
    private var pose: Pose? = null
    private var animator: ValueAnimator? = null
    private var firstPerson = false
    private var cameraTransitionUntil = 0L

    init {
        VehicleStatus.entries.forEach { style.addImage(iconName(it), vanBitmap(context, it)) }

        style.addSource(routeSource)
        style.addLayer(
            LineLayer("route", ROUTE_SOURCE).withProperties(
                PropertyFactory.lineColor(Green600.toArgb()),
                PropertyFactory.lineWidth(5f),
                // Tracejado do protótipo (12/6 px numa linha de 5 px), em unidades da largura.
                PropertyFactory.lineDasharray(arrayOf(2.4f, 1.2f)),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )

        // Halo em volta da entrega atual.
        style.addSource(destinationSource)
        style.addLayer(
            CircleLayer("destination", DESTINATION_SOURCE).withProperties(
                PropertyFactory.circleRadius(24f),
                PropertyFactory.circleColor(Green600.toArgb()),
                PropertyFactory.circleOpacity(0.18f),
                PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP)
            )
        )

        // Paradas numeradas, nas cores do protótipo: CD, entregues, atual e pendentes.
        style.addSource(stopsSource)
        style.addLayer(
            CircleLayer("stops", STOPS_SOURCE).withProperties(
                PropertyFactory.circleRadius(13f),
                PropertyFactory.circleColor(
                    Expression.match(
                        Expression.get("kind"), Expression.color(Surface1.toArgb()),
                        Expression.stop(KIND_CD, Expression.color(Green900.toArgb())),
                        Expression.stop(KIND_DONE, Expression.color(Text3.toArgb())),
                        Expression.stop(KIND_CURRENT, Expression.color(Green600.toArgb()))
                    )
                ),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor(Surface1.toArgb()),
                PropertyFactory.circlePitchAlignment(Property.CIRCLE_PITCH_ALIGNMENT_MAP)
            )
        )
        style.addLayer(
            SymbolLayer("stop-labels", STOPS_SOURCE).withProperties(
                PropertyFactory.textField(Expression.get("label")),
                PropertyFactory.textFont(arrayOf("Noto Sans Bold")),
                PropertyFactory.textSize(12f),
                PropertyFactory.textColor(
                    Expression.match(
                        Expression.get("kind"), Expression.color(Surface1.toArgb()),
                        Expression.stop(KIND_PENDING, Expression.color(Text2.toArgb()))
                    )
                ),
                PropertyFactory.textAllowOverlap(true),
                PropertyFactory.textIgnorePlacement(true)
            )
        )

        style.addSource(vehicleSource)
        style.addLayer(
            SymbolLayer("vehicle", VEHICLE_SOURCE).withProperties(
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

    fun update(newState: DeliveryUiState) {
        val old = state
        state = newState

        val target = Pose(newState.position.latitude, newState.position.longitude, newState.heading)
        val from = pose
        animator?.cancel()
        if (from == null || old == null || newState.current != old.current || newState.index <= old.index) {
            // Início, novo trecho, recomeço ou mesma posição: posiciona sem animar.
            pose = target
        } else {
            // Avançou um tick: desliza até a nova posição no tempo do tick.
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = DeliveryTrip.TICK_MILLIS
                interpolator = LinearInterpolator()
                addUpdateListener { a ->
                    pose = from.lerp(target, a.animatedValue as Float)
                    draw()
                }
                start()
            }
        }

        val shouldFollow = newState.phase == DeliveryPhase.NAVIGATING
        val backToPlanning = newState.phase == DeliveryPhase.PLANNING &&
            (old?.phase != DeliveryPhase.PLANNING || old.current != newState.current)
        if (old == null || shouldFollow != firstPerson || backToPlanning) {
            firstPerson = shouldFollow
            // Na navegação a câmera é controlada pelo app; fora dela o mapa é livre.
            map.uiSettings.setAllGesturesEnabled(!firstPerson)
            recenter(animate = old != null)
        }
        draw()
    }

    /** Volta a câmera para o veículo (navegação) ou enquadra o trecho que falta (demais fases). */
    fun recenter(animate: Boolean = true) {
        val current = pose ?: return
        val update = if (firstPerson) {
            CameraUpdateFactory.newCameraPosition(driverCamera(current))
        } else {
            overviewCamera(current)
        }
        if (animate) {
            cameraTransitionUntil = System.currentTimeMillis() + CAMERA_TRANSITION_MILLIS
            map.animateCamera(update, CAMERA_TRANSITION_MILLIS)
        } else {
            map.moveCamera(update)
        }
    }

    fun release() {
        animator?.cancel()
    }

    private fun draw() {
        val s = state ?: return
        val p = pose ?: return

        // Sem veículo escolhido não há o que desenhar no ponto de partida.
        val vehicle = s.vehicle?.let {
            Feature.fromGeometry(Point.fromLngLat(p.lon, p.lat)).apply {
                addStringProperty("icon", iconName(s.status))
                addNumberProperty("heading", p.heading)
            }
        }
        vehicleSource.setGeoJson(FeatureCollection.fromFeatures(listOfNotNull(vehicle)))

        // Linha começa no veículo (posição interpolada) e segue até a entrega atual.
        val remaining = s.remainingPath.drop(1)
        if (remaining.isEmpty()) {
            routeSource.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
        } else {
            routeSource.setGeoJson(
                LineString.fromLngLats(
                    listOf(Point.fromLngLat(p.lon, p.lat)) + remaining.map { Point.fromLngLat(it.longitude, it.latitude) }
                )
            )
        }
        destinationSource.setGeoJson(
            FeatureCollection.fromFeatures(
                listOfNotNull(s.currentDelivery?.let { Feature.fromGeometry(Point.fromLngLat(it.longitude, it.latitude)) })
            )
        )
        stopsSource.setGeoJson(stopFeatures(s))

        // Segue o veículo quadro a quadro, exceto durante uma transição animada da câmera.
        if (firstPerson && System.currentTimeMillis() >= cameraTransitionUntil) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(driverCamera(p)))
        }
    }

    private fun driverCamera(pose: Pose) = CameraPosition.Builder()
        .target(LatLng(pose.lat, pose.lon))
        .zoom(DRIVER_ZOOM)
        .bearing(pose.heading.toDouble())
        .tilt(DRIVER_TILT)
        .padding(0.0, mapHeightPx() * DRIVER_TOP_PADDING, 0.0, 0.0)
        .build()

    private fun stopFeatures(s: DeliveryUiState): FeatureCollection {
        val (cdLat, cdLon) = DeliveryRoutes.distributionCenter
        val cd = Feature.fromGeometry(Point.fromLngLat(cdLon, cdLat)).apply {
            addStringProperty("label", "CD")
            addStringProperty("kind", KIND_CD)
        }
        val stops = s.deliveries.map { d ->
            Feature.fromGeometry(Point.fromLngLat(d.longitude, d.latitude)).apply {
                addStringProperty("label", d.order.toString())
                addStringProperty(
                    "kind",
                    when {
                        d.order - 1 < s.current -> KIND_DONE
                        d.order - 1 == s.current -> KIND_CURRENT
                        else -> KIND_PENDING
                    }
                )
            }
        }
        return FeatureCollection.fromFeatures(listOf(cd) + stops)
    }

    /**
     * Vista de cima, norte para cima, enquadrando o veículo e o trecho até a entrega atual
     * (ou, com o dia concluído, todas as paradas).
     */
    private fun overviewCamera(pose: Pose): CameraUpdate {
        val s = state
        val path = when {
            s == null -> emptyList()
            s.allDone -> s.deliveries.map { LatLng(it.latitude, it.longitude) } +
                DeliveryRoutes.distributionCenter.let { (lat, lon) -> LatLng(lat, lon) }
            // Na chegada não sobra trecho: mostra o caminho que acabou de ser feito.
            s.phase == DeliveryPhase.ARRIVED -> s.trip.points.map { LatLng(it.latitude, it.longitude) }
            else -> s.remainingPath.map { LatLng(it.latitude, it.longitude) }
        }
        val points = (listOf(LatLng(pose.lat, pose.lon)) + path).distinct()
        val camera = if (points.size >= 2) {
            map.getCameraForLatLngBounds(LatLngBounds.Builder().includes(points).build(), overviewPaddingPx, 0.0, 0.0)
        } else {
            null
        }
        return CameraUpdateFactory.newCameraPosition(
            camera ?: CameraPosition.Builder()
                .target(LatLng(pose.lat, pose.lon))
                .zoom(OVERVIEW_ZOOM)
                .bearing(0.0)
                .tilt(0.0)
                .padding(0.0, 0.0, 0.0, 0.0)
                .build()
        )
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
