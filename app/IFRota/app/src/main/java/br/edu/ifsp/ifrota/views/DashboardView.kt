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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.components.StatTile
import br.edu.ifsp.ifrota.ui.components.initials
import br.edu.ifsp.ifrota.ui.theme.Amber100
import br.edu.ifsp.ifrota.ui.theme.Amber800
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green100
import br.edu.ifsp.ifrota.ui.theme.Green50
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Surface3
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.DashboardViewModel
import java.util.Calendar

@Composable
fun DashboardView(
    viewModel: DashboardViewModel,
    accountEmail: String,
    onOpenProfile: () -> Unit,
    onOpenVehicles: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    val greeting = remember { greetingForNow() }
    val displayName = profile?.name?.takeIf { it.isNotBlank() }
        ?: accountEmail.substringBefore('@')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface2)
    ) {
        GreenHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$greeting,",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(onClick = onOpenProfile),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.initials(accountEmail),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FleetSummaryCard(
                vehicles = vehicles,
                modifier = Modifier.offset(y = (-12).dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Próxima entrega")
                IFRotaCard {
                    EmptyState(
                        emoji = "🗺️",
                        title = "Nenhuma entrega atribuída",
                        message = "As rotas e entregas entram nas próximas etapas. "
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(
                    text = "Meus veículos",
                    trailing = {
                        TextButton(onClick = onOpenVehicles) {
                            Text(
                                text = "Ver todos",
                                style = MaterialTheme.typography.labelMedium,
                                color = Green600
                            )
                        }
                    }
                )

                if (vehicles.isEmpty()) {
                    IFRotaCard {
                        EmptyState(
                            emoji = "🚚",
                            title = "Nenhum veículo cadastrado",
                            message = "Cadastre seu primeiro veículo na aba Veículos."
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        vehicles.take(3).forEach { vehicle ->
                            VehicleSummaryRow(
                                vehicle = vehicle,
                                onClick = onOpenVehicles
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FleetSummaryCard(vehicles: List<VehicleEntity>, modifier: Modifier = Modifier) {
    val total = vehicles.size
    val synced = vehicles.count { it.isSynced }
    val pending = total - synced
    val capacity = vehicles.sumOf { it.capacityKg }

    IFRotaCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sua frota",
                    style = MaterialTheme.typography.titleMedium,
                    color = Text2
                )
                Text(
                    text = if (total == 1) "1 veículo" else "$total veículos",
                    style = MaterialTheme.typography.labelMedium,
                    color = Green600
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(
                    value = total.toString(),
                    label = "Cadastrados",
                    background = Green100,
                    valueColor = Green900,
                    labelColor = Green900.copy(alpha = 0.75f),
                    bordered = false
                )
                StatTile(
                    value = pending.toString(),
                    label = "Aguardando sincronia",
                    background = Amber100,
                    valueColor = Amber800,
                    labelColor = Amber800.copy(alpha = 0.75f),
                    bordered = false
                )
                StatTile(
                    value = if (capacity > 0) formatCapacity(capacity) else "—",
                    label = "Capacidade total",
                    background = Surface3,
                    valueColor = Text2,
                    labelColor = Text3,
                    bordered = false
                )
            }
        }
    }
}

@Composable
private fun VehicleSummaryRow(vehicle: VehicleEntity, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface1, RoundedCornerShape(16.dp))
            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Green50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalShipping,
                contentDescription = null,
                tint = Green600,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vehicle.model.ifBlank { "Veículo sem modelo" },
                style = MaterialTheme.typography.titleSmall,
                color = Text1
            )
            Text(
                text = vehicle.plate,
                style = MaterialTheme.typography.bodySmall,
                color = Text3
            )
        }
        if (!vehicle.isSynced) {
            Text(
                text = "Pendente",
                style = MaterialTheme.typography.labelSmall,
                color = Amber800,
                modifier = Modifier
                    .background(Amber100, CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

private fun greetingForNow(): String =
    when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 0..11 -> "Bom dia"
        in 12..17 -> "Boa tarde"
        else -> "Boa noite"
    }

internal fun formatCapacity(kg: Double): String = when {
    kg >= 1000 -> "${(kg / 1000).formatOneDecimal()} t"
    else -> "${kg.formatOneDecimal()} kg"
}

private fun Double.formatOneDecimal(): String =
    if (this % 1.0 == 0.0) toInt().toString() else String.format("%.1f", this).replace('.', ',')
