package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.data.delivery.SampleDeliveries
import br.edu.ifsp.ifrota.ui.components.ConfirmDeliverySheet
import br.edu.ifsp.ifrota.ui.components.DeliveryStatusBadge
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.components.SecondaryButton
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.components.StatTile
import br.edu.ifsp.ifrota.ui.components.formatDistance
import br.edu.ifsp.ifrota.ui.components.formatDuration
import br.edu.ifsp.ifrota.ui.map.LegPreviewMap
import br.edu.ifsp.ifrota.ui.theme.Amber100
import br.edu.ifsp.ifrota.ui.theme.Amber500
import br.edu.ifsp.ifrota.ui.theme.Amber800
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green50
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Surface3
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryPhase
import br.edu.ifsp.ifrota.ui.viewmodel.DeliveryViewModel

@Composable
fun DeliveryDetailView(
    viewModel: DeliveryViewModel,
    deliveryId: String,
    onBack: () -> Unit,
    onOpenRoute: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val delivery = state.deliveries.find { it.id == deliveryId } ?: return
    val status = state.statusOf(delivery)
    val isCurrent = delivery.order - 1 == state.current
    var confirming by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Surface2)) {
        GreenHeader {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Entrega #${delivery.order}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text("Detalhes da entrega", style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IFRotaCard {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SectionLabel(text = "Destinatário", trailing = { DeliveryStatusBadge(status) })
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(52.dp).background(Green50, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Green600, modifier = Modifier.size(26.dp))
                        }
                        Column {
                            Text(delivery.recipient, style = MaterialTheme.typography.titleLarge, color = Text1)
                            Text("Destinatário", style = MaterialTheme.typography.bodySmall, color = Text3)
                        }
                    }
                    HorizontalDivider(color = Surface3)
                    InfoRow(
                        icon = Icons.Filled.Place,
                        label = "Endereço",
                        value = "${delivery.address}\n${delivery.neighborhood} — ${delivery.city}"
                    )
                    InfoRow(icon = Icons.Filled.LocalShipping, label = "Tipo de pacote", value = delivery.packageType)
                }
            }

            IFRotaCard {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SectionLabel(text = "Informações de rota")
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatTile(
                            value = formatDistance(state.legMeters(delivery)),
                            label = "Distância",
                            background = Surface2,
                            valueColor = Text1
                        )
                        StatTile(
                            value = formatDuration(state.legSeconds(delivery)),
                            label = "Tempo est.",
                            background = Surface2,
                            valueColor = Text1
                        )
                        StatTile(
                            value = "#${delivery.order}",
                            label = "Ordem",
                            background = Surface2,
                            valueColor = Text1
                        )
                    }
                    Text(
                        text = "Trecho a partir " + if (delivery.order == 1) {
                            "do CD (${SampleDeliveries.DISTRIBUTION_CENTER_NAME})"
                        } else {
                            "da entrega #${delivery.order - 1}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Text3
                    )
                }
            }

            delivery.note?.let { note ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Amber100.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .border(1.dp, Amber100, RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = Amber500, modifier = Modifier.size(18.dp))
                    Column {
                        Text("OBSERVAÇÃO", style = MaterialTheme.typography.labelSmall, color = Amber800)
                        Text(note, style = MaterialTheme.typography.bodyMedium, color = Amber800)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, BorderSubtle, RoundedCornerShape(24.dp))
            ) {
                LegPreviewMap(trip = state.legs[delivery.order - 1], modifier = Modifier.fillMaxSize())
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Green600)
                        .clickable(onClick = onOpenRoute)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.NearMe, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Text("Abrir rota", style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
        }

        // Rodapé de ações: só a entrega atual pode ser iniciada ou concluída.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface1)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                status == DeliveryStatus.DONE -> InfoFooter(icon = Icons.Filled.Check, text = "Entrega concluída")
                !isCurrent -> InfoFooter(text = "Disponível após a entrega #${delivery.order - 1}")
                state.phase == DeliveryPhase.ARRIVED -> {
                    SecondaryButton(text = "Ver rota", onClick = onOpenRoute, modifier = Modifier.weight(1f).height(56.dp))
                    PrimaryButton(
                        text = "Concluir",
                        onClick = { confirming = true },
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
                state.phase == DeliveryPhase.PLANNING -> PrimaryButton(
                    text = "Iniciar entrega",
                    onClick = {
                        // Sem veículo escolhido, a escolha é feita na tela de rota.
                        viewModel.start()
                        onOpenRoute()
                    },
                    leadingIcon = { Icon(Icons.Filled.NearMe, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                else -> SecondaryButton(text = "Ver rota", onClick = onOpenRoute, modifier = Modifier.height(56.dp))
            }
        }
    }

    if (confirming) {
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
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(32.dp).background(Green50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Green600, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Text3)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = Text2, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

@Composable
private fun InfoFooter(text: String, icon: ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Surface3, RoundedCornerShape(16.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Icon(it, contentDescription = null, tint = Text3, modifier = Modifier.padding(end = 8.dp).size(18.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp), color = Text3)
    }
}

