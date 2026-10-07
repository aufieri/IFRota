package com.example.pocmapa.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pocmapa.data.SampleFleet
import com.example.pocmapa.data.VehicleSnapshot
import com.example.pocmapa.data.VehicleStatus
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private val InTransitColor = Color(0xFF2E7D32)
private val StoppedColor = Color(0xFFC62828)
internal val RouteColor = Color(0xFF1E88E5)

internal fun VehicleStatus.color() = if (this == VehicleStatus.IN_TRANSIT) InTransitColor else StoppedColor
private fun VehicleStatus.label() = if (this == VehicleStatus.IN_TRANSIT) "Em trânsito" else "Parado"

@Composable
fun FleetMapScreen(viewModel: FleetViewModel) {
    val fleet by viewModel.fleet.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var renderer by remember { mutableStateOf<FleetMapRenderer?>(null) }

    // Veículo seguido no modo motorista; null = visão da frota.
    var followedId by rememberSaveable { mutableStateOf<String?>(null) }
    // Veículo tocado no mapa (cartão de detalhes na visão da frota).
    var selectedId by remember { mutableStateOf<String?>(null) }
    val followed = fleet.find { it.vehicle.id == followedId }
    val selected = fleet.find { it.vehicle.id == selectedId }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            val (lat, lon) = SampleFleet.DISTRIBUTION_CENTER
            map.cameraPosition = CameraPosition.Builder().target(LatLng(lat, lon)).zoom(FLEET_ZOOM).build()
            map.uiSettings.isCompassEnabled = false
            map.addOnMapClickListener { point ->
                val hit = map.queryRenderedFeatures(map.projection.toScreenLocation(point), VEHICLE_LAYER)
                selectedId = hit.firstOrNull()?.getStringProperty("id")
                selectedId != null
            }
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                renderer = FleetMapRenderer(context, map, style) { mapView.height }
            }
        }
        onDispose { renderer?.release() }
    }
    LaunchedEffect(renderer, fleet) { renderer?.update(fleet) }
    LaunchedEffect(renderer, followedId) { renderer?.follow(followedId) }

    BackHandler(enabled = followedId != null) { followedId = null }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            if (followed != null) {
                DriverHud(followed, modifier = Modifier.align(Alignment.TopCenter).padding(12.dp))
                Button(
                    onClick = { followedId = null },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
                ) { Text("Visão da frota") }
            } else {
                Legend(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                selected?.let {
                    SelectedVehicleCard(
                        snapshot = it,
                        onGuide = { followedId = it.vehicle.id; selectedId = null },
                        onClose = { selectedId = null },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
                    )
                }
            }
        }
        if (followed == null) {
            HorizontalDivider()
            Text(
                "Toque em um veículo para guiar como motorista",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                items(fleet, key = { it.vehicle.id }) { snapshot ->
                    VehicleRow(snapshot) { followedId = snapshot.vehicle.id; selectedId = null }
                }
            }
        }
    }
}

/** MapView do MapLibre exige repassar todo o ciclo de vida, de onCreate a onDestroy. */
@Composable
private fun rememberMapViewWithLifecycle(): MapView {
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
            mapView.onDestroy()
        }
    }
    return mapView
}

private fun telemetryText(snapshot: VehicleSnapshot): String {
    val t = snapshot.telemetry
    return "${t.speedKmh} km/h · motor ${if (t.engineOn) "ligado" else "desligado"} · " +
        "portas ${if (t.doorsOpen) "abertas" else "fechadas"}"
}

private fun formatDistance(meters: Double): String =
    if (meters >= 1_000) "%.1f km".format(meters / 1_000) else "${(meters / 10).toInt() * 10} m"

@Composable
private fun DriverHud(snapshot: VehicleSnapshot, modifier: Modifier = Modifier) {
    val t = snapshot.telemetry
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${t.speedKmh}", fontSize = 36.sp, fontWeight = FontWeight.Bold)
                Text("km/h", style = MaterialTheme.typography.labelMedium)
            }
            Column(modifier = Modifier.padding(start = 20.dp)) {
                Text(
                    if (snapshot.status == VehicleStatus.STOPPED) {
                        if (t.doorsOpen) "Parado · entrega em andamento" else "Parado"
                    } else {
                        "Próxima parada em ${formatDistance(snapshot.distanceToNextStopMeters())}"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${snapshot.vehicle.plate} · ${snapshot.vehicle.model}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectedVehicleCard(
    snapshot: VehicleSnapshot,
    onGuide: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(snapshot.status)
                Text(
                    "${snapshot.vehicle.plate} · ${snapshot.vehicle.model} — ${snapshot.status.label()}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                telemetryText(snapshot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onClose) { Text("Fechar") }
                TextButton(onClick = onGuide) { Text("Guiar") }
            }
        }
    }
}

@Composable
private fun Legend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), MaterialTheme.shapes.small)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        VehicleStatus.entries.forEach { status ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status)
                Text(
                    status.label(),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusDot(status: VehicleStatus) {
    Box(modifier = Modifier.size(12.dp).background(status.color(), CircleShape))
}

@Composable
private fun VehicleRow(snapshot: VehicleSnapshot, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusDot(snapshot.status)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                "${snapshot.vehicle.plate} · ${snapshot.vehicle.model} — ${snapshot.status.label()}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                telemetryText(snapshot),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
