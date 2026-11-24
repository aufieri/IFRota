package br.edu.ifsp.ifrota.poc_admin_entregas.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.AppDatabase
import br.edu.ifsp.ifrota.poc_admin_entregas.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.poc_admin_entregas.data.repository.DeliveryRepository
import br.edu.ifsp.ifrota.poc_admin_entregas.data.routing.RouteMetrics
import br.edu.ifsp.ifrota.poc_admin_entregas.data.sync.SyncManager
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.DriverDeliveriesViewModel
import br.edu.ifsp.ifrota.poc_admin_entregas.ui.viewmodel.DriverDeliveriesViewModelFactory
import kotlinx.coroutines.launch

/** Entregas do motorista logado: só as dele, persistidas no Room e sincronizadas com o Firestore. */
@Composable
fun DriverScreen(userId: String, email: String, onLogout: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember(context) { AppDatabase.getDatabase(context) }
    val sync = remember(userId) { SyncManager(userId, database.deliveryDao()) }
    val repository = remember(userId) { DeliveryRepository(userId, database.deliveryDao(), sync) }

    DisposableEffect(sync) {
        sync.startListening()
        scope.launch { sync.syncAll() }
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { sync.syncAll() }
            }
        }
        connectivity?.registerDefaultNetworkCallback(callback)
        onDispose {
            connectivity?.unregisterNetworkCallback(callback)
            sync.close()
        }
    }

    val viewModel: DriverDeliveriesViewModel = viewModel(
        key = "driver-$userId",
        factory = DriverDeliveriesViewModelFactory(repository)
    )
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Minhas entregas", style = MaterialTheme.typography.headlineSmall)
                Text(email, style = MaterialTheme.typography.bodySmall)
            }
            OutlinedButton(onClick = {
                scope.launch {
                    clearLocalDeliveries(context)
                    onLogout()
                }
            }) { Text("Sair") }
        }

        val list = deliveries
        when {
            list == null -> CircularProgressIndicator()
            list.isEmpty() -> Text(
                "Nenhuma entrega atribuída. Quando o administrador criar entregas para você, elas aparecem aqui.",
                style = MaterialTheme.typography.bodyMedium
            )
            else -> {
                val pending = list.count { !it.isDone }
                val pendingKm = list.filter { !it.isDone }.sumOf { RouteMetrics.pathMeters(it.legPath) }
                Text(
                    "$pending pendentes de ${list.size} · ${RouteMetrics.formatKm(pendingKm)} de rota restante",
                    style = MaterialTheme.typography.titleSmall
                )
                // A entrega da vez é a primeira ainda pendente da fila.
                val currentId = list.firstOrNull { !it.isDone }?.id
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(list, key = { it.id }) { delivery ->
                        DeliveryCard(
                            delivery = delivery,
                            canComplete = delivery.id == currentId,
                            onComplete = { viewModel.complete(delivery.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryCard(delivery: DeliveryEntity, canComplete: Boolean, onComplete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("#${delivery.orderNumber} · ${delivery.recipient}", style = MaterialTheme.typography.titleMedium)
            Text("${delivery.address} · ${delivery.neighborhood}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "${delivery.packageType} · trecho de ${RouteMetrics.formatKm(RouteMetrics.pathMeters(delivery.legPath))}",
                style = MaterialTheme.typography.bodySmall
            )
            delivery.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (delivery.isDone) "Concluída" else "Pendente", style = MaterialTheme.typography.labelLarge)
                if (!delivery.isDone) {
                    Button(onClick = onComplete, enabled = canComplete) { Text("Concluir") }
                }
            }
        }
    }
}

private suspend fun clearLocalDeliveries(context: Context) {
    AppDatabase.getDatabase(context).deliveryDao().clearAll()
}
