package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.ui.viewmodel.VehicleRegistrationViewModel

@Composable
fun VehicleRegistrationView(viewModel: VehicleRegistrationViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.consumeSaveSuccess()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Cadastro de Veículo", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.plate,
            onValueChange = viewModel::onPlateChange,
            label = { Text("Placa*") },
            isError = uiState.plateError != null,
            supportingText = { uiState.plateError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.model,
            onValueChange = viewModel::onModelChange,
            label = { Text("Modelo*") },
            isError = uiState.modelError != null,
            supportingText = { uiState.modelError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.vehicleType,
            onValueChange = viewModel::onVehicleTypeChange,
            label = { Text("Tipo (Caminhão, Furgão, ...)") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.capacityKg,
            onValueChange = viewModel::onCapacityChange,
            label = { Text("Capacidade (Kg)") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::saveVehicle,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Salvar Veículo")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn {
            items(vehicles, key = { it.id }) { vehicle ->
                VehicleListItem(vehicle = vehicle, onDelete = { viewModel.deleteVehicle(vehicle) })
            }
        }
    }
}

@Composable
private fun VehicleListItem(vehicle: VehicleEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("${vehicle.model} (${vehicle.vehicleType})", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Placa: ${vehicle.plate} | ${vehicle.capacityKg}Kg",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
