package br.edu.ifsp.ifrota.views

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition
import br.edu.ifsp.ifrota.data.tracking.TrackingPhase
import br.edu.ifsp.ifrota.data.tracking.TrackingRepository
import br.edu.ifsp.ifrota.notification.GeofenceNotifier
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.map.FleetMapRenderer
import br.edu.ifsp.ifrota.ui.map.MAP_STYLE_URL
import br.edu.ifsp.ifrota.ui.map.OVERVIEW_ZOOM
import br.edu.ifsp.ifrota.ui.map.rememberMapViewWithLifecycle
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Amber800
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Red600
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.AdminFenceEvent
import br.edu.ifsp.ifrota.ui.viewmodel.AdminTrackingViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.AdminTrackingViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.TrackedDriver
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Área administrativa: mapa ao vivo da frota, lista de entregadores com o que cada um está
 * fazendo e os eventos de perímetro, que também chegam como notificação.
 */
@Composable
fun AdminScaffold(userId: String, accountEmail: String) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    val notifier = remember(context) { GeofenceNotifier(context.applicationContext) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!notifier.canNotify() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val viewModel: AdminTrackingViewModel = viewModel(
        key = "admin-$userId",
        factory = AdminTrackingViewModelFactory(TrackingRepository(), userId, notifier::notify)
    )
    // collectAsState (e não ...WithLifecycle): continua observando em segundo plano, para as
    // notificações de perímetro chegarem com o app minimizado.
    val state by viewModel.state.collectAsState()

    val mapView = rememberMapViewWithLifecycle()
    var renderer by remember { mutableStateOf<FleetMapRenderer?>(null) }
    val mapPadding = with(density) { 48.dp.roundToPx() }.let { intArrayOf(it, it, it, it) }

    DisposableEffect(mapView) {
        var cancelled = false
        mapView.getMapAsync { map ->
            if (cancelled) return@getMapAsync
            map.cameraPosition = CameraPosition.Builder()
                .target(LatLng(DistributionCenter.LATITUDE, DistributionCenter.LONGITUDE))
                .zoom(OVERVIEW_ZOOM - 1)
                .build()
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                if (!cancelled) renderer = FleetMapRenderer(context, map, style, mapPadding)
            }
        }
        onDispose {
            cancelled = true
            renderer?.release()
        }
    }
    LaunchedEffect(renderer, state.drivers) { renderer?.update(state.drivers) }

    Column(modifier = Modifier.fillMaxSize().background(Surface2)) {
        GreenHeader {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Área Administrativa", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text(
                        "$accountEmail · ${state.onlineCount} de ${state.drivers.size} online",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = {
                    scope.launch {
                        clearLocalData(context)
                        FirebaseAuth.getInstance().signOut()
                    }
                }) {
                    Icon(Icons.Filled.Logout, contentDescription = "Sair", tint = Color.White)
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1.1f).clipToBounds()) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = { renderer?.frameAll() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(Surface1, CircleShape)
                    .border(1.dp, Color(0x14000000), CircleShape)
            ) {
                Icon(Icons.Filled.CenterFocusStrong, contentDescription = "Ver toda a frota", tint = Green600)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f).navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            state.error?.let { message ->
                item {
                    Text(
                        message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Red600,
                        modifier = Modifier.fillMaxWidth()
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    )
                }
            }

            item { SectionLabel("Entregadores") }
            if (state.loaded && state.drivers.isEmpty()) {
                item {
                    Text(
                        "Nenhum entregador com o app aberto ainda. A posição aparece aqui assim que um entregador entrar no app.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Text3
                    )
                }
            }
            items(state.drivers, key = { it.id }) { driver ->
                DriverRow(driver, now = state.now, onClick = { renderer?.focus(driver.id) })
            }

            item { SectionLabel("Eventos de perímetro", modifier = Modifier.padding(top = 8.dp)) }
            if (state.events.isEmpty()) {
                item {
                    Text(
                        "Os avisos de entrada e saída de perímetro aparecem aqui e como notificação.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Text3
                    )
                }
            }
            items(state.events, key = { it.record.id }) { EventRow(it) }
        }
    }
}

@Composable
private fun DriverRow(driver: TrackedDriver, now: Long, onClick: () -> Unit) {
    val location = driver.location
    val color = when {
        !driver.online -> Text3
        location.speedKmh > 0 -> Green600
        else -> Amber500
    }
    IFRotaCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(12.dp).background(color, CircleShape))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        driver.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Text1,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (location.plate.isNotBlank()) {
                        Text(
                            " · ${location.plate}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Text3
                        )
                    }
                }
                Text(
                    statusText(driver),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (driver.online) Text1 else Text3,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    buildString {
                        if (location.totalDeliveries > 0) {
                            append("${location.doneCount}/${location.totalDeliveries} entregas · ")
                        }
                        append(
                            when {
                                driver.online -> "atualizado ${ago(now - location.updatedAt)}"
                                location.signedOut -> "saiu da conta ${ago(now - location.updatedAt)}"
                                else -> "offline · visto ${ago(now - location.updatedAt)}"
                            }
                        )
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Text3
                )
            }
        }
    }
}

private fun statusText(driver: TrackedDriver): String {
    val l = driver.location
    val target = l.deliveryOrder?.let { order ->
        "entrega #$order" + (l.deliveryRecipient?.let { " · $it" } ?: "")
    } ?: "próxima entrega"
    return when (l.phase) {
        TrackingPhase.IDLE -> if (l.plate.isBlank()) "Sem veículo selecionado" else "Sem entregas no momento"
        TrackingPhase.PLANNING -> "Aguardando saída para a $target"
        TrackingPhase.NAVIGATING -> "A caminho da $target · ${l.speedKmh} km/h"
        TrackingPhase.PAUSED -> "Pausado a caminho da $target"
        TrackingPhase.ARRIVED -> "No local da $target"
        TrackingPhase.FINISHED -> "Todas as entregas concluídas"
    }
}

private fun ago(millis: Long): String {
    val seconds = (millis / 1000).coerceAtLeast(0)
    return when {
        seconds < 60 -> "há ${seconds} s"
        seconds < 3600 -> "há ${seconds / 60} min"
        else -> "há ${seconds / 3600} h"
    }
}

private val eventTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

@Composable
private fun EventRow(event: AdminFenceEvent) {
    val entered = event.record.transition == GeofenceTransition.ENTER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (entered) Icons.Filled.Login else Icons.Filled.Logout,
            contentDescription = if (entered) "Entrou" else "Saiu",
            tint = if (entered) Green600 else Amber800,
            modifier = Modifier.size(20.dp)
        )
        Text(event.message, style = MaterialTheme.typography.bodyMedium, color = Text1, modifier = Modifier.weight(1f))
        Text(eventTime.format(Date(event.record.timestampMillis)), style = MaterialTheme.typography.labelSmall, color = Text3)
    }
}
