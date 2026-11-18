package br.edu.ifsp.ifrota.views

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.admin.AdminDriver
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.InitialsAvatar
import br.edu.ifsp.ifrota.ui.components.MetaChip
import br.edu.ifsp.ifrota.ui.components.initials
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.DriversState

/** Área administrativa: todos os motoristas cadastrados; tocar em um abre as entregas dele. */
@Composable
fun AdminDriversView(
    viewModel: AdminViewModel,
    accountEmail: String,
    onOpenDriver: (AdminDriver) -> Unit
) {
    val state by viewModel.drivers.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehiclesByDriver.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(Surface2)) {
        GreenHeader {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Administração",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = when (val s = state) {
                            is DriversState.Ready -> when (s.drivers.size) {
                                0 -> "Nenhum motorista cadastrado"
                                1 -> "1 motorista cadastrado"
                                else -> "${s.drivers.size} motoristas cadastrados"
                            }
                            else -> accountEmail
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (val s = state) {
            DriversState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Green600)
            }
            is DriversState.Error -> EmptyState(emoji = "⚠️", title = "Algo deu errado", message = s.message)
            is DriversState.Ready ->
                if (s.drivers.isEmpty()) {
                    EmptyState(
                        emoji = "👤",
                        title = "Nenhum motorista cadastrado",
                        message = "Os motoristas aparecem aqui depois de criarem a conta no aplicativo."
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(s.drivers, key = { it.id }) { driver ->
                            DriverCard(
                                driver = driver,
                                vehicleCount = vehicles[driver.id].orEmpty().size,
                                onClick = { onOpenDriver(driver) }
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun DriverCard(driver: AdminDriver, vehicleCount: Int, onClick: () -> Unit) {
    IFRotaCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InitialsAvatar(
                initials = DriverEntity(name = driver.name).initials(driver.email),
                size = 48.dp,
                cornerRadius = 16.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = driver.name.ifBlank { driver.email.substringBefore('@') },
                    style = MaterialTheme.typography.titleMedium,
                    color = Text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = driver.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Text3,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetaChip(
                        when (vehicleCount) {
                            0 -> "Sem veículo"
                            1 -> "1 veículo"
                            else -> "$vehicleCount veículos"
                        }
                    )
                    if (driver.phone.isNotBlank()) MetaChip(driver.phone)
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Text3
            )
        }
    }
}
