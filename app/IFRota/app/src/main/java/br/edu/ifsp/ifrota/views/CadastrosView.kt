package br.edu.ifsp.ifrota.views

import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.data.local.AppDatabase
import br.edu.ifsp.ifrota.data.repository.DriverRepository
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import br.edu.ifsp.ifrota.data.sync.SyncManager
import br.edu.ifsp.ifrota.ui.viewmodel.DriverRegistrationViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.DriverRegistrationViewModelFactory
import br.edu.ifsp.ifrota.ui.viewmodel.VehicleRegistrationViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.VehicleRegistrationViewModelFactory
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastrosView(onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val syncManager = remember { SyncManager(database.driverDao(), database.vehicleDao()) }
    val vehicleRepository = remember { VehicleRepository(database.vehicleDao(), syncManager) }
    val driverRepository = remember { DriverRepository(database.driverDao(), syncManager) }

    DisposableEffect(Unit) {
        syncManager.startListening()
        coroutineScope.launch { syncManager.syncAll() }

        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                coroutineScope.launch { syncManager.syncAll() }
            }
        }
        connectivityManager?.registerDefaultNetworkCallback(networkCallback)

        onDispose {
            syncManager.stopListening()
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
    }

    val vehicleViewModel = viewModel<VehicleRegistrationViewModel>(
        factory = VehicleRegistrationViewModelFactory(vehicleRepository)
    )
    val driverViewModel = viewModel<DriverRegistrationViewModel>(
        factory = DriverRegistrationViewModelFactory(driverRepository)
    )

    var selectedTab by remember { mutableIntStateOf(0) }

    Column {
        TopAppBar(
            title = { Text("Cadastros") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                }
            }
        )

        SecondaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Veículos") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Motoristas") }
            )
        }

        if (selectedTab == 0) {
            VehicleRegistrationView(viewModel = vehicleViewModel)
        } else {
            DriverRegistrationView(viewModel = driverViewModel)
        }
    }
}
