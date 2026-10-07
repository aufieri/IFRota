package br.edu.ifsp.ifrota.views

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.simulation.DeliveryRoutes
import br.edu.ifsp.ifrota.ui.components.ConfirmDeliverySheet
import br.edu.ifsp.ifrota.ui.components.DeliveryNote
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.components.SecondaryButton
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.components.formatDistance
import br.edu.ifsp.ifrota.ui.components.formatDuration
import br.edu.ifsp.ifrota.ui.map.DeliveryMapRenderer
import br.edu.ifsp.ifrota.ui.map.MAP_STYLE_URL
import br.edu.ifsp.ifrota.ui.map.OVERVIEW_ZOOM
import br.edu.ifsp.ifrota.ui.map.rememberMapViewWithLifecycle
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Amber800
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green50
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryPhase
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryUiState
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModel
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style

/** O painel inferior sobe sobre o mapa, como no protótipo. */
private val PanelOverlap = 16.dp

/**
 * Aba Rotas: mostra o trecho até a entrega atual. Antes de sair, a rota fica vista de cima
 * (mapa livre); durante o trecho, a câmera segue o veículo em primeira pessoa até o endereço,
 * a não ser que o trecho seja pausado ou encerrado. Cada trecho parte de onde foi feita a
 * entrega anterior.
 */
@Composable
fun RouteView(
    viewModel: DeliveryViewModel,
    onOpenVehicles: () -> Unit,
    onOpenDelivery: (Delivery) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val density = LocalDensity.current
    val mapView = rememberMapViewWithLifecycle()
    var renderer by remember { mutableStateOf<DeliveryMapRenderer?>(null) }
    var panelHeightPx by remember { mutableIntStateOf(0) }
    var confirming by remember { mutableStateOf(false) }

    // Espaço livre ao enquadrar a rota: status bar + chip no topo, botão à direita, painel embaixo.
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val overviewPadding = with(density) {
        intArrayOf(40.dp.roundToPx(), statusBarPx + 72.dp.roundToPx(), 64.dp.roundToPx(), (PanelOverlap + 40.dp).roundToPx())
    }

    UseDarkStatusBarIcons()

    DisposableEffect(mapView) {
        mapView.getMapAsync { map ->
            val (lat, lon) = DeliveryRoutes.distributionCenter
            map.cameraPosition = CameraPosition.Builder().target(LatLng(lat, lon)).zoom(OVERVIEW_ZOOM).build()
            // A bússola ficaria sob o chip; na navegação o mapa já gira sozinho.
            map.uiSettings.isCompassEnabled = false
            // Logo e atribuição sobem acima da parte do mapa coberta pelo painel.
            val overlapPx = with(density) { PanelOverlap.roundToPx() }
            map.uiSettings.apply {
                setLogoMargins(logoMarginLeft, logoMarginTop, logoMarginRight, logoMarginBottom + overlapPx)
                setAttributionMargins(
                    attributionMarginLeft, attributionMarginTop, attributionMarginRight,
                    attributionMarginBottom + overlapPx
                )
            }
            map.setStyle(Style.Builder().fromUri(MAP_STYLE_URL)) { style ->
                renderer = DeliveryMapRenderer(context, map, style, { mapView.height }, overviewPadding)
            }
        }
        onDispose { renderer?.release() }
    }
    LaunchedEffect(renderer, state) { renderer?.update(state) }

    // Voltar durante a navegação pausa o trecho em vez de sair da tela.
    BackHandler(enabled = state.phase == DeliveryPhase.NAVIGATING) { viewModel.pause() }

    val panelHeight = with(density) { panelHeightPx.toDp() }

    Box(modifier = Modifier.fillMaxSize().background(Surface2)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = (panelHeight - PanelOverlap).coerceAtLeast(0.dp))
        ) {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                RouteChip(state)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp))
                        .background(Surface1, RoundedCornerShape(16.dp))
                        .clickable { renderer?.recenter() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GpsFixed,
                        contentDescription = if (state.phase == DeliveryPhase.NAVIGATING) "Centralizar no veículo" else "Ver rota inteira",
                        tint = Green600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { panelHeightPx = it.height }
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(Surface1)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(width = 36.dp, height = 4.dp).background(BorderSubtle, CircleShape))
            }

            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val delivery = state.currentDelivery
                when {
                    delivery == null -> DayDonePanel(state)
                    state.phase == DeliveryPhase.PLANNING -> PlanningPanel(
                        state = state,
                        delivery = delivery,
                        onSelectVehicle = viewModel::selectVehicle,
                        onStart = viewModel::start,
                        onOpenVehicles = onOpenVehicles,
                        onOpenDelivery = { onOpenDelivery(delivery) }
                    )
                    state.phase == DeliveryPhase.ARRIVED -> ArrivedPanel(
                        delivery = delivery,
                        onConfirm = { confirming = true },
                        onOpenDelivery = { onOpenDelivery(delivery) }
                    )
                    else -> NavigationPanel(
                        state = state,
                        delivery = delivery,
                        onPause = viewModel::pause,
                        onResume = viewModel::resume,
                        onEnd = viewModel::end
                    )
                }
            }
        }
    }

    val delivery = state.currentDelivery
    if (confirming && delivery != null) {
        ConfirmDeliverySheet(
            delivery = delivery,
            onConfirm = {
                confirming = false
                viewModel.confirmDelivery()
            },
            onDismiss = { confirming = false }
        )
    }
}

@Composable
private fun PlanningPanel(
    state: DeliveryUiState,
    delivery: Delivery,
    onSelectVehicle: (String) -> Unit,
    onStart: () -> Unit,
    onOpenVehicles: () -> Unit,
    onOpenDelivery: () -> Unit
) {
    SectionLabel(
        text = "Próximo destino",
        trailing = {
            Text(
                text = "Ver detalhes",
                style = MaterialTheme.typography.labelMedium,
                color = Green600,
                modifier = Modifier.clickable(onClick = onOpenDelivery)
            )
        }
    )
    NextDestinationCard(
        delivery = delivery,
        eta = formatDuration(state.remainingSeconds),
        distance = formatDistance(state.remainingMeters)
    )

    if (state.vehicles.isEmpty()) {
        EmptyState(
            emoji = "🚚",
            title = "Nenhum veículo cadastrado",
            message = "Cadastre o veículo que fará as entregas na aba Veículos.",
            action = {
                TextButton(onClick = onOpenVehicles) {
                    Text("Ir para Veículos", style = MaterialTheme.typography.labelMedium, color = Green600)
                }
            }
        )
        return
    }

    if (state.vehicleLocked) {
        // O veículo foi escolhido na primeira entrega e segue o mesmo até o fim do dia.
        state.vehicle?.let { VehicleLine(it) }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel(text = "Veículo das entregas")
            if (state.vehicle == null) {
                Text(
                    text = "Selecione o veículo que você está usando",
                    style = MaterialTheme.typography.bodySmall,
                    color = Amber800
                )
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.vehicles, key = { it.id }) { vehicle ->
                    VehicleChoice(
                        vehicle = vehicle,
                        selected = vehicle.id == state.vehicle?.id,
                        onClick = { onSelectVehicle(vehicle.id) }
                    )
                }
            }
        }
    }

    PrimaryButton(
        text = "Iniciar entrega #${delivery.order}",
        onClick = onStart,
        enabled = state.canStart,
        leadingIcon = { Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(20.dp)) }
    )
}

@Composable
private fun NavigationPanel(
    state: DeliveryUiState,
    delivery: Delivery,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onEnd: () -> Unit
) {
    val vehicle = state.vehicle ?: return
    val telemetry = state.telemetry
    val paused = state.phase == DeliveryPhase.PAUSED
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "${telemetry.speedKmh}", style = MaterialTheme.typography.displaySmall, color = Text1)
            Text("km/h", style = MaterialTheme.typography.labelMedium, color = Text3)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (paused) "Entrega pausada"
                else "Faltam ${formatDistance(state.remainingMeters)} · ${formatDuration(state.remainingSeconds)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (paused) Amber800 else Text1
            )
            Text(
                text = "#${delivery.order} · ${delivery.recipient} — ${delivery.address}",
                style = MaterialTheme.typography.bodySmall,
                color = Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${vehicle.plate} · motor ${if (telemetry.engineOn) "ligado" else "desligado"} · " +
                    "portas ${if (telemetry.doorsOpen) "abertas" else "fechadas"}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Text3
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (paused) {
            PrimaryButton(text = "Continuar", onClick = onResume, modifier = Modifier.weight(1f))
        } else {
            SecondaryButton(text = "Pausar", onClick = onPause, modifier = Modifier.weight(1f).height(56.dp))
        }
        SecondaryButton(text = "Encerrar", onClick = onEnd, modifier = Modifier.weight(1f).height(56.dp))
    }
}

@Composable
private fun ArrivedPanel(delivery: Delivery, onConfirm: () -> Unit, onOpenDelivery: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Green600, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("Você chegou · entrega #${delivery.order}", style = MaterialTheme.typography.titleMedium, color = Text1)
            Text(
                text = "${delivery.recipient} — ${delivery.address}",
                style = MaterialTheme.typography.bodySmall,
                color = Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    delivery.note?.let { DeliveryNote(it) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SecondaryButton(text = "Detalhes", onClick = onOpenDelivery, modifier = Modifier.weight(1f).height(56.dp))
        PrimaryButton(
            text = "Confirmar",
            onClick = onConfirm,
            modifier = Modifier.weight(1f),
            leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun DayDonePanel(state: DeliveryUiState) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Green600, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Column {
            Text("Entregas do dia concluídas", style = MaterialTheme.typography.titleMedium, color = Text1)
            Text(
                text = "${state.doneCount} de ${state.deliveries.size} entregas feitas" +
                    (state.vehicle?.let { " com ${it.plate}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = Text3
            )
        }
    }
}

/** Carta "Próximo destino" do protótipo: ordem, destinatário, endereço, tempo e distância. */
@Composable
private fun NextDestinationCard(delivery: Delivery, eta: String, distance: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Green50, RoundedCornerShape(16.dp))
            .border(1.5.dp, Green600, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Green600, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = delivery.order.toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Color.White
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(delivery.recipient, style = MaterialTheme.typography.titleSmall, color = Text1)
            Text(
                text = delivery.address,
                style = MaterialTheme.typography.bodySmall,
                color = Text2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(delivery.neighborhood, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Text3)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = eta,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = Green600
            )
            Text(distance, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp), color = Text3)
        }
    }
}

@Composable
private fun VehicleLine(vehicle: VehicleEntity) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = Text3, modifier = Modifier.size(16.dp))
        Text(
            text = "Veículo: ${vehicle.plate} · ${vehicle.model.ifBlank { "Sem modelo" }}",
            style = MaterialTheme.typography.bodySmall,
            color = Text2
        )
    }
}

@Composable
private fun VehicleChoice(vehicle: VehicleEntity, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Green50 else Surface2, shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) Green600 else BorderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.LocalShipping,
            contentDescription = null,
            tint = if (selected) Green600 else Text3,
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = vehicle.plate,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Text1
            )
            Text(
                text = vehicle.model.ifBlank { "Sem modelo" },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Text3
            )
        }
    }
}

/** Resumo sobre o mapa, como o chip "restantes · tempo" do protótipo. */
@Composable
private fun RouteChip(state: DeliveryUiState) {
    val remaining = state.deliveries.size - state.doneCount
    val (dotColor, title, detail) = when {
        state.allDone -> Triple(Green600, "Entregas concluídas", null)
        state.phase == DeliveryPhase.PLANNING ->
            Triple(Green600, "$remaining restantes", "~${formatDuration(state.remainingDaySeconds)}")
        state.phase == DeliveryPhase.NAVIGATING ->
            Triple(Green600, "Em trânsito", "~${formatDuration(state.remainingSeconds)}")
        state.phase == DeliveryPhase.PAUSED ->
            Triple(Amber500, "Pausada", "faltam ${formatDistance(state.remainingMeters)}")
        else -> Triple(Green600, "No destino", "entrega #${state.current + 1}")
    }
    Row(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .background(Surface1, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
        Text(text = title, style = MaterialTheme.typography.labelMedium, color = Text1)
        if (detail != null) {
            Box(modifier = Modifier.size(width = 1.dp, height = 14.dp).background(BorderSubtle))
            Text(text = detail, style = MaterialTheme.typography.bodySmall, color = Text3)
        }
    }
}

/**
 * O app usa ícones claros na status bar (sobre o cabeçalho verde); aqui o mapa fica
 * atrás dela, então os ícones ficam escuros enquanto a tela estiver aberta.
 */
@Composable
private fun UseDarkStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(window, view)
        val previous = controller.isAppearanceLightStatusBars
        controller.isAppearanceLightStatusBars = true
        onDispose { controller.isAppearanceLightStatusBars = previous }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
