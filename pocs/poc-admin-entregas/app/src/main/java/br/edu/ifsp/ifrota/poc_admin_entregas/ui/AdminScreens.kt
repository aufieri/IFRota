package br.edu.ifsp.ifrota.poc_admin_entregas.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.poc_admin_entregas.data.admin.AdminDriver
import br.edu.ifsp.ifrota.poc_admin_entregas.data.admin.AdminRepository
import br.edu.ifsp.ifrota.poc_admin_entregas.data.delivery.DemoDeliveryGenerator
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.RouteMetrics
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.AdminViewModelFactory
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.DriversState

/**
 * Área do administrador: motoristas cadastrados e, dentro de cada um, as entregas dele e a
 * ação "Criar entregas". Só é mostrada quando o papel lido do servidor é administrador; o
 * que o servidor permite de fato é decidido pelas regras do Firestore (`firestore.rules`).
 */
@Composable
fun AdminScreens(userId: String, email: String, onLogout: () -> Unit) {
    val viewModel: AdminViewModel = viewModel(
        key = "admin-$userId",
        factory = AdminViewModelFactory(remember { AdminRepository() })
    )
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }

    val selected = (drivers as? DriversState.Ready)?.drivers?.find { it.id == selectedId }
    if (selected != null) {
        DriverDetail(viewModel = viewModel, driver = selected, onBack = { selectedId = null })
    } else {
        DriversList(
            state = drivers,
            email = email,
            onOpen = { selectedId = it.id },
            onLogout = onLogout
        )
    }
}

@Composable
private fun DriversList(
    state: DriversState,
    email: String,
    onOpen: (AdminDriver) -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Administração", style = MaterialTheme.typography.headlineSmall)
                Text(email, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = onLogout) { Text("Sair") }
        }

        when (state) {
            DriversState.Loading -> CircularProgressIndicator()
            is DriversState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error)
            is DriversState.Ready ->
                if (state.drivers.isEmpty()) {
                    Text("Nenhum motorista cadastrado.")
                } else {
                    Text("${state.drivers.size} motoristas cadastrados", style = MaterialTheme.typography.titleSmall)
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.drivers, key = { it.id }) { driver ->
                            Card(modifier = Modifier.fillMaxWidth().clickable { onOpen(driver) }) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        driver.name.ifBlank { driver.email.substringBefore('@') },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(driver.email, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        listOfNotNull(
                                            driver.phone.takeIf { it.isNotBlank() },
                                            driver.cnh.takeIf { it.isNotBlank() }?.let { "CNH $it" }
                                        ).joinToString(" · "),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
        }
    }
}

@Composable
private fun DriverDetail(viewModel: AdminViewModel, driver: AdminDriver, onBack: () -> Unit) {
    val deliveries by remember(driver.id) { viewModel.deliveriesOf(driver.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val create by viewModel.create.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onBack) { Text("← Motoristas") }
        Text(
            driver.name.ifBlank { driver.email.substringBefore('@') },
            style = MaterialTheme.typography.headlineSmall
        )
        Text(driver.email, style = MaterialTheme.typography.bodySmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Criar entregas", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Quantidade: ${create.quantity}")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.onQuantityChange(create.quantity - 1) },
                            enabled = !create.isCreating && create.quantity > DemoDeliveryGenerator.MIN_QUANTITY
                        ) { Text("−") }
                        OutlinedButton(
                            onClick = { viewModel.onQuantityChange(create.quantity + 1) },
                            enabled = !create.isCreating && create.quantity < DemoDeliveryGenerator.MAX_QUANTITY
                        ) { Text("+") }
                    }
                }
                Button(
                    onClick = {
                        viewModel.startCreating(driver.id)
                        viewModel.createDeliveries(driver.id)
                    },
                    enabled = !create.isCreating,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (create.isCreating) "Criando…" else "Criar entregas") }

                create.message?.let { message ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            message.text,
                            modifier = Modifier.weight(1f),
                            color = if (message.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = viewModel::consumeMessage) { Text("OK") }
                    }
                }
            }
        }

        Text(
            "Entregas atribuídas (${deliveries.size}, ${deliveries.count { !it.isDone }} pendentes)",
            style = MaterialTheme.typography.titleSmall
        )
        if (deliveries.isEmpty()) {
            Text("Nenhuma entrega atribuída a este motorista.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(deliveries, key = { it.id }) { AdminDeliveryRow(it) }
            }
        }
    }
}

@Composable
private fun AdminDeliveryRow(delivery: DeliveryEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("#${delivery.orderNumber} · ${delivery.recipient}", style = MaterialTheme.typography.titleSmall)
            Text("${delivery.address} · ${delivery.neighborhood}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${delivery.packageType} · trecho de ${RouteMetrics.formatKm(RouteMetrics.pathMeters(delivery.legPath))} · " +
                    if (delivery.isDone) "Concluída" else "Pendente",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
