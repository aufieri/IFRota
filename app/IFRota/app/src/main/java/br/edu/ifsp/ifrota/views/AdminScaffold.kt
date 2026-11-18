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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.edu.ifsp.ifrota.core.clearLocalData
import br.edu.ifsp.ifrota.data.admin.AdminRepository
import br.edu.ifsp.ifrota.data.delivery.DistributionCenter
import br.edu.ifsp.ifrota.data.geofencing.GeofenceTransition
import br.edu.ifsp.ifrota.data.tracking.TrackingPhase
import br.edu.ifsp.ifrota.data.tracking.TrackingRepository
import br.edu.ifsp.ifrota.navigation.AdminRoutes
import br.edu.ifsp.ifrota.navigation.AdminTab
import br.edu.ifsp.ifrota.notification.GeofenceNotifier
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaBottomBar
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
import br.edu.ifsp.ifrota.ui.viewmodel.AdminUiState
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.DriversState
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
 * Área administrativa, em duas abas:
 * - **Frota**: mapa ao vivo, o que cada entregador está fazendo e os eventos de perímetro
 *   (que também chegam como notificação);
 * - **Motoristas**: todos os motoristas cadastrados e, dentro de cada um, as entregas dele e
 *   a criação de novas entregas.
 *
 * Só é alcançada quando o papel lido do servidor é administrador (ver `AuthManager`); o que
 * o servidor de fato permite é decidido pelas regras do Firestore, não por esta tela.
 */
@Composable
fun AdminScaffold(userId: String, accountEmail: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val tabs = AdminTab.entries

    val notifier = remember(context) { GeofenceNotifier(context.applicationContext) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (!notifier.canNotify() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Os dois ViewModels ficam fora do NavHost: sobrevivem à troca de abas.
    val trackingViewModel: AdminTrackingViewModel = viewModel(
        key = "admin-tracking-$userId",
        factory = AdminTrackingViewModelFactory(TrackingRepository(), userId, notifier::notify)
    )
    val adminViewModel: AdminViewModel = viewModel(
        key = "admin-$userId",
        factory = AdminViewModelFactory(remember { AdminRepository() })
    )
    // collectAsState (e não ...WithLifecycle) e aqui, fora das abas: continua observando com
    // o app em segundo plano ou na aba Motoristas, para as notificações de perímetro chegarem.
    val trackingState by trackingViewModel.state.collectAsState()

    val logout: () -> Unit = {
        scope.launch {
            clearLocalData(context)
            FirebaseAuth.getInstance().signOut()
        }
    }

    Scaffold(
        containerColor = Surface2,
        bottomBar = {
            // O detalhe do motorista ocupa a tela toda, como as telas internas do motorista.
            if (tabs.any { it.route == currentRoute }) {
                IFRotaBottomBar(
                    tabs = tabs,
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        if (tab.route != currentRoute) {
                            navController.navigate(tab.route) {
                                popUpTo(AdminTab.FLEET.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = AdminTab.FLEET.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable(AdminTab.FLEET.route) {
                AdminFleetView(state = trackingState, accountEmail = accountEmail, onLogout = logout)
            }

            composable(AdminTab.DRIVERS.route) {
                AdminDriversView(
                    viewModel = adminViewModel,
                    accountEmail = accountEmail,
                    onOpenDriver = { navController.navigate(AdminRoutes.driverDetail(it.id)) },
                    onLogout = logout
                )
            }

            composable(
                route = AdminRoutes.DRIVER_DETAIL,
                arguments = listOf(navArgument(AdminRoutes.DRIVER_ID) { type = NavType.StringType })
            ) { entry ->
                val driverId = entry.arguments?.getString(AdminRoutes.DRIVER_ID).orEmpty()
                val drivers by adminViewModel.drivers.collectAsStateWithLifecycle()
                val driver = (drivers as? DriversState.Ready)?.drivers?.find { it.id == driverId }

                if (driver != null) {
                    AdminDriverDetailView(
                        viewModel = adminViewModel,
                        driver = driver,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

/** Aba Frota: mapa ao vivo, lista de entregadores e eventos de perímetro. */
@Composable
private fun AdminFleetView(state: AdminUiState, accountEmail: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val density = LocalDensity.current

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
                IconButton(onClick = onLogout) {
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
