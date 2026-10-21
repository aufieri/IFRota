package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.delivery.Delivery
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.ui.components.DeliveryNote
import br.edu.ifsp.ifrota.ui.components.DeliveryOrderBadge
import br.edu.ifsp.ifrota.ui.components.DeliveryStatusBadge
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.MetaChip
import br.edu.ifsp.ifrota.ui.components.formatDistance
import br.edu.ifsp.ifrota.ui.components.formatDuration
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryUiState
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModel

private enum class DeliveryFilter(val label: String, val status: DeliveryStatus?) {
    ALL("Todas", null),
    ACTIVE("Em andamento", DeliveryStatus.ACTIVE),
    PENDING("Pendentes", DeliveryStatus.PENDING),
    DONE("Concluídas", DeliveryStatus.DONE)
}

/** Aba Entregas: as entregas do dia na ordem em que devem ser feitas. */
@Composable
fun DeliveriesView(
    viewModel: DeliveryViewModel,
    onOpenDelivery: (Delivery) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(DeliveryFilter.ALL) }
    val visible = state.deliveries.filter { filter.status == null || state.statusOf(it) == filter.status }

    Column(modifier = Modifier.fillMaxSize().background(Surface2)) {
        GreenHeader {
            Text("Entregas", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                text = when (state.deliveries.size) {
                    0 -> "Nenhuma entrega atribuída"
                    1 -> "1 entrega atribuída"
                    else -> "${state.deliveries.size} entregas atribuídas"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DeliveryFilter.entries.forEach { option ->
                    FilterPill(option.label, selected = option == filter) { filter = option }
                }
            }
        }

        if (state.hasDeliveries) RouteProgressStrip(state = state, onOpenDelivery = onOpenDelivery)

        if (!state.hasDeliveries) {
            EmptyState(
                emoji = "📭",
                title = if (state.loaded) "Nenhuma entrega atribuída" else "Carregando entregas…",
                message = "Quando o administrador atribuir entregas a você, elas aparecem aqui."
            )
        } else if (visible.isEmpty()) {
            EmptyState(emoji = "📭", title = "Nenhuma entrega aqui", message = "Tente outro filtro")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(visible, key = { it.id }) { delivery ->
                    DeliveryCard(
                        delivery = delivery,
                        status = state.statusOf(delivery),
                        state = state,
                        onClick = { onOpenDelivery(delivery) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color.White else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (selected) Green600 else Color.White.copy(alpha = 0.85f)
        )
    }
}

/** Sequência do dia: um círculo por entrega, ligados pelo trecho entre elas. */
@Composable
private fun RouteProgressStrip(state: DeliveryUiState, onOpenDelivery: (Delivery) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {
        state.deliveries.forEachIndexed { i, delivery ->
            val status = state.statusOf(delivery)
            Column(
                modifier = Modifier.clickable { onOpenDelivery(delivery) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DeliveryOrderBadge(
                    order = delivery.order,
                    status = status,
                    size = 32.dp,
                    cornerRadius = 16.dp,
                    modifier = Modifier.border(
                        if (status == DeliveryStatus.ACTIVE) 2.dp else 1.5.dp,
                        if (status == DeliveryStatus.ACTIVE) Green600 else BorderSubtle,
                        CircleShape
                    )
                )
                Text(
                    // A última palavra distingue melhor os bairros ("Vila Santana", "Vila Melhado").
                    text = delivery.neighborhood.substringAfterLast(' '),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                    color = Text3,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 52.dp)
                )
            }
            if (i < state.deliveries.lastIndex) {
                Box(
                    modifier = Modifier
                        .padding(top = 15.dp, start = 4.dp, end = 4.dp)
                        .size(width = 20.dp, height = 1.5.dp)
                        .background(if (status == DeliveryStatus.DONE) Green600 else BorderSubtle)
                )
            }
        }
    }
}

@Composable
private fun DeliveryCard(
    delivery: Delivery,
    status: DeliveryStatus,
    state: DeliveryUiState,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    val active = status == DeliveryStatus.ACTIVE
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (status == DeliveryStatus.DONE) 0.72f else 1f)
            .clip(shape)
            .background(Surface1, shape)
            .border(if (active) 1.5.dp else 1.dp, if (active) Green600 else BorderSubtle, shape)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DeliveryOrderBadge(order = delivery.order, status = status)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = delivery.recipient,
                    style = MaterialTheme.typography.titleSmall,
                    color = Text1,
                    textDecoration = if (status == DeliveryStatus.DONE) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f)
                )
                DeliveryStatusBadge(status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = Text3,
                    modifier = Modifier.padding(top = 1.dp).size(14.dp)
                )
                Column {
                    Text(delivery.address, style = MaterialTheme.typography.bodyMedium, color = Text2)
                    Text(
                        text = "${delivery.neighborhood} · ${delivery.city}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Text3
                    )
                }
            }
            delivery.note?.let { DeliveryNote(it, modifier = Modifier.padding(top = 2.dp)) }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetaChip(formatDuration(state.legSeconds(delivery)), highlighted = true)
                MetaChip(formatDistance(state.legMeters(delivery)))
                MetaChip(delivery.packageType)
            }
        }
    }
}

