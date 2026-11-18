package com.example.pocgeofencing.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pocgeofencing.data.GeofenceEvent
import com.example.pocgeofencing.data.GeofenceTransition
import com.example.pocgeofencing.data.SampleFleet
import com.example.pocgeofencing.data.SampleGeofences
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GeofenceScreen(viewModel: GeofenceViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mapView = rememberMapViewWithLifecycle()
    var renderer by remember { mutableStateOf<GeofenceMapRenderer?>(null) }

    var canNotify by remember { mutableStateOf(viewModel.canNotify()) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = viewModel.canNotify()
    }
    LaunchedEffect(Unit) {
        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            val (lat, lon) = SampleFleet.DISTRIBUTION_CENTER
            map.cameraPosition = CameraPosition.Builder().target(LatLng(lat, lon - 0.01)).zoom(FLEET_ZOOM).build()
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                renderer = GeofenceMapRenderer(context, style)
            }
        }
        onDispose { renderer?.release() }
    }
    LaunchedEffect(renderer, state) { renderer?.update(state) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).clipToBounds()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            Legend(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }

        if (!canNotify && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Notificações desativadas: os eventos aparecem só na lista.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                        Text("Permitir")
                    }
                }
            }
        }

        HorizontalDivider()
        Text(
            "Perímetros",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        val vehicles = state.fleet.map { it.vehicle }
        state.geofences.forEach { fence ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Dot(if (fence.id in state.occupied) FenceOccupiedColor else FenceIdleColor)
                Text(
                    "${fence.name} · ${fence.radiusMeters.toInt()} m",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp).weight(1f)
                )
                Text(
                    SampleGeofences.targetLabel(fence, vehicles),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
        Text(
            "Eventos",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )
        if (state.events.isEmpty()) {
            Text(
                "Aguardando um veículo cruzar um perímetro…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(0.6f)) {
            items(state.events, key = { "${it.timestampMillis}-${it.vehicleId}-${it.geofence.id}-${it.transition}" }) {
                EventRow(it)
            }
        }
    }
}

private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

@Composable
private fun EventRow(event: GeofenceEvent) {
    val enter = event.transition == GeofenceTransition.ENTER
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (enter) "▶ ENTROU" else "◀ SAIU",
            style = MaterialTheme.typography.labelMedium,
            color = if (enter) InTransitColor else StoppedColor,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(event.message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            timeFormat.format(Date(event.timestampMillis)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
        LegendItem(FenceIdleColor, "Perímetro livre")
        LegendItem(FenceOccupiedColor, "Veículo dentro")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Dot(color)
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun Dot(color: Color) {
    Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
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
