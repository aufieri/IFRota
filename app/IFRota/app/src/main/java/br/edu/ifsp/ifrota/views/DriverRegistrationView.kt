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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.ui.viewmodel.DriverRegistrationViewModel

@Composable
fun DriverRegistrationView(viewModel: DriverRegistrationViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.consumeSaveSuccess()
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Cadastro de Motorista", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.name,
            onValueChange = viewModel::onNameChange,
            label = { Text("Nome*") },
            isError = uiState.nameError != null,
            supportingText = { uiState.nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.cnh,
            onValueChange = viewModel::onCnhChange,
            label = { Text("CNH*") },
            isError = uiState.cnhError != null,
            supportingText = { uiState.cnhError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = uiState.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text("Telefone") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::saveDriver,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Salvar Motorista")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        LazyColumn {
            items(drivers, key = { it.id }) { driver ->
                DriverListItem(driver = driver, onDelete = { viewModel.deleteDriver(driver) })
            }
        }
    }
}

@Composable
private fun DriverListItem(driver: DriverEntity, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(driver.name, style = MaterialTheme.typography.bodyLarge)
                Text("CNH: ${driver.cnh} | ${driver.phone}", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
