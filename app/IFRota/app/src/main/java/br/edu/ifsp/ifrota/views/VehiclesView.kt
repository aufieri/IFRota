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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.IFRotaTextField
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.components.SecondaryButton
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
import br.edu.ifsp.ifrota.ui.viewmodel.VehiclesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehiclesView(viewModel: VehiclesViewModel) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isFormOpen by viewModel.isFormOpen.collectAsStateWithLifecycle()

    var vehicleToDelete by remember { mutableStateOf<VehicleEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                        text = "Veículos",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = when (vehicles.size) {
                            0 -> "Nenhum veículo cadastrado"
                            1 -> "1 veículo cadastrado"
                            else -> "${vehicles.size} veículos cadastrados"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(onClick = viewModel::openNewVehicleForm),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Cadastrar veículo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        if (vehicles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center
            ) {
                IFRotaCard {
                    EmptyState(
                        emoji = "🚚",
                        title = "Sua frota está vazia",
                        message = "Cadastre um veículo para vê-lo aqui e no Início.",
                        action = {
                            PrimaryButton(
                                text = "Cadastrar veículo",
                                onClick = viewModel::openNewVehicleForm,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 16.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(vehicles, key = { it.id }) { vehicle ->
                    VehicleCard(
                        vehicle = vehicle,
                        onEdit = { viewModel.openEditForm(vehicle) },
                        onDelete = { vehicleToDelete = vehicle }
                    )
                }
            }
        }
    }

    if (isFormOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeForm,
            sheetState = sheetState,
            containerColor = Surface1,
            dragHandle = null
        ) {
            VehicleFormSheet(
                isEditing = form.isEditing,
                plate = form.plate,
                model = form.model,
                vehicleType = form.vehicleType,
                capacityKg = form.capacityKg,
                plateError = form.plateError,
                modelError = form.modelError,
                isSaving = form.isSaving,
                onPlateChange = viewModel::onPlateChange,
                onModelChange = viewModel::onModelChange,
                onVehicleTypeChange = viewModel::onVehicleTypeChange,
                onCapacityChange = viewModel::onCapacityChange,
                onSave = viewModel::saveVehicle,
                onCancel = viewModel::closeForm
            )
        }
    }

    vehicleToDelete?.let { vehicle ->
        AlertDialog(
            onDismissRequest = { vehicleToDelete = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = Surface1,
            title = {
                Text(
                    text = "Excluir veículo?",
                    style = MaterialTheme.typography.titleLarge,
                    color = Text1
                )
            },
            text = {
                Text(
                    text = "O veículo ${vehicle.plate} sairá da sua frota. Essa ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteVehicle(vehicle)
                    vehicleToDelete = null
                }) {
                    Text(
                        text = "Excluir",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { vehicleToDelete = null }) {
                    Text(
                        text = "Cancelar",
                        style = MaterialTheme.typography.labelMedium,
                        color = Text2
                    )
                }
            }
        )
    }
}

@Composable
private fun VehicleCard(
    vehicle: VehicleEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    IFRotaCard {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Green50, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = Green600,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = vehicle.model.ifBlank { "Veículo sem modelo" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Text1
                        )
                        Text(
                            text = vehicle.plate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text2
                        )
                    }
                    SyncBadge(isSynced = vehicle.isSynced)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (vehicle.vehicleType.isNotBlank()) {
                        Chip(text = vehicle.vehicleType)
                    }
                    if (vehicle.capacityKg > 0) {
                        Chip(text = formatCapacity(vehicle.capacityKg))
                    }
                }
            }

            Column {
                IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar",
                        tint = Text3,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Excluir",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Text2,
        modifier = Modifier
            .background(Surface3, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun SyncBadge(isSynced: Boolean) {
    val (label, background, content) = if (isSynced) {
        Triple("Sincronizado", Green100, Green900)
    } else {
        Triple("Pendente", Amber100, Amber800)
    }
    Row(
        modifier = Modifier
            .background(background, CircleShape)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(content, CircleShape)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content
        )
    }
}

@Composable
private fun VehicleFormSheet(
    isEditing: Boolean,
    plate: String,
    model: String,
    vehicleType: String,
    capacityKg: String,
    plateError: String?,
    modelError: String?,
    isSaving: Boolean,
    onPlateChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    onVehicleTypeChange: (String) -> Unit,
    onCapacityChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 40.dp, height = 4.dp)
                .background(BorderSubtle, CircleShape)
        )

        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = if (isEditing) "Editar veículo" else "Novo veículo",
                style = MaterialTheme.typography.headlineSmall,
                color = Text1
            )
            Text(
                text = "Placa e modelo são obrigatórios.",
                style = MaterialTheme.typography.bodyMedium,
                color = Text3
            )
        }

        IFRotaTextField(
            label = "Placa",
            value = plate,
            onValueChange = onPlateChange,
            placeholder = "ABC1D23",
            capitalization = KeyboardCapitalization.Characters,
            errorMessage = plateError
        )

        IFRotaTextField(
            label = "Modelo",
            value = model,
            onValueChange = onModelChange,
            placeholder = "Fiorino 1.4",
            capitalization = KeyboardCapitalization.Words,
            errorMessage = modelError
        )

        IFRotaTextField(
            label = "Tipo",
            value = vehicleType,
            onValueChange = onVehicleTypeChange,
            placeholder = "Furgão, caminhão, moto...",
            capitalization = KeyboardCapitalization.Sentences
        )

        IFRotaTextField(
            label = "Capacidade (kg)",
            value = capacityKg,
            onValueChange = onCapacityChange,
            placeholder = "650",
            keyboardType = KeyboardType.Number,
            capitalization = KeyboardCapitalization.None,
            imeAction = ImeAction.Done
        )

        PrimaryButton(
            text = if (isEditing) "Salvar alterações" else "Cadastrar veículo",
            onClick = onSave,
            isLoading = isSaving
        )

        SecondaryButton(text = "Cancelar", onClick = onCancel)
    }
}
