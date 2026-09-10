package br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel.AppViewModelFactory
import br.edu.ifsp.ifrota.poc_sqlite_firebase_sync.ui.viewmodel.SyncViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToDrivers: () -> Unit,
    onNavigateToVehicles: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: SyncViewModel = viewModel(factory = AppViewModelFactory(context))

    Scaffold(
        topBar = { TopAppBar(title = { Text("POC Sqlite + Firebase Sync") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onNavigateToDrivers, modifier = Modifier.fillMaxWidth()) {
                Text("Motoristas")
            }
            Button(onClick = onNavigateToVehicles, modifier = Modifier.fillMaxWidth()) {
                Text("Veículos")
            }
            Button(
                onClick = { viewModel.syncNow() },
                enabled = !viewModel.isSyncing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (viewModel.isSyncing) "Sincronizando..." else "Sincronizar agora")
            }
            if (viewModel.isSyncing) {
                CircularProgressIndicator()
            }
            viewModel.statusMessage?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
