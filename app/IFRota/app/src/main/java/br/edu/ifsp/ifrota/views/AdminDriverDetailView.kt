package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.data.admin.AdminDriver
import br.edu.ifsp.ifrota.data.admin.AdminVehicle
import br.edu.ifsp.ifrota.data.delivery.DeliveryStatus
import br.edu.ifsp.ifrota.data.delivery.DemoDeliveryGenerator
import br.edu.ifsp.ifrota.data.local.entity.DeliveryEntity
import br.edu.ifsp.ifrota.ui.components.DeliveryOrderBadge
import br.edu.ifsp.ifrota.ui.components.DeliveryStatusBadge
import br.edu.ifsp.ifrota.ui.components.EmptyState
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.MetaChip
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.components.SecondaryButton
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.components.StatTile
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green100
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Red100
import br.edu.ifsp.ifrota.ui.theme.Red600
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Surface3
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.AdminMessage
import br.edu.ifsp.ifrota.ui.viewmodel.AdminViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.VehiclesState

/** Entregas de um motorista e a ação "Criar entregas" para ele. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDriverDetailView(
    viewModel: AdminViewModel,
    driver: AdminDriver,
    onBack: () -> Unit
) {
    val deliveries by remember(driver.id) { viewModel.deliveriesOf(driver.id) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val create by viewModel.create.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehicles.collectAsStateWithLifecycle()
    var sheetOpen by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Sucesso fecha o formulário; erro o mantém aberto para tentar de novo.
    LaunchedEffect(create.message) {
        if (create.message?.isError == false) sheetOpen = false
    }

    val pending = deliveries.count { !it.isDone }

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
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = driver.name.ifBlank { driver.email.substringBefore('@') },
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = driver.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (driver.phone.isNotBlank()) MetaChip(driver.phone)
                    if (driver.cnh.isNotBlank()) MetaChip("CNH ${driver.cnh}")
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(value = deliveries.size.toString(), label = "Atribuídas")
                    StatTile(value = pending.toString(), label = "Pendentes")
                    StatTile(value = (deliveries.size - pending).toString(), label = "Concluídas")
                }
            }

            item {
                PrimaryButton(
                    text = "Criar entregas",
                    onClick = {
                        viewModel.startCreating(driver.id)
                        sheetOpen = true
                    },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }

            val success = create.message?.takeIf { !it.isError && !sheetOpen }
            if (success != null) {
                item { MessageBanner(success, onDismiss = viewModel::consumeMessage) }
            }

            item { SectionLabel(text = "Veículos cadastrados") }

            when (val v = vehiclesState) {
                VehiclesState.Loading -> item {
                    Text("Carregando veículos…", style = MaterialTheme.typography.bodyMedium, color = Text3)
                }
                is VehiclesState.Error -> item {
                    Text(v.message, style = MaterialTheme.typography.bodyMedium, color = Red600)
                }
                is VehiclesState.Ready -> {
                    val vehicles = v.byDriver[driver.id].orEmpty()
                    if (vehicles.isEmpty()) {
                        item {
                            Text(
                                text = "Este motorista ainda não cadastrou veículos. Sem veículo ele não consegue iniciar as entregas.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Text3
                            )
                        }
                    } else {
                        items(vehicles, key = { "vehicle-${it.id}" }) { VehicleRow(it) }
                    }
                }
            }

            item { SectionLabel(text = "Entregas atribuídas") }

            if (deliveries.isEmpty()) {
                item {
                    IFRotaCard {
                        EmptyState(
                            emoji = "📦",
                            title = "Nenhuma entrega atribuída",
                            message = "Use “Criar entregas” para atribuir entregas a este motorista."
                        )
                    }
                }
            } else {
                items(deliveries, key = { it.id }) { DeliveryRow(it) }
            }
        }
    }

    if (sheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { if (!create.isCreating) sheetOpen = false },
            sheetState = sheetState,
            containerColor = androidx.compose.ui.graphics.Color.White
        ) {
            CreateDeliveriesSheet(
                driverName = driver.name.ifBlank { driver.email },
                quantity = create.quantity,
                isCreating = create.isCreating,
                error = create.message?.takeIf { it.isError },
                onQuantityChange = viewModel::onQuantityChange,
                onConfirm = { viewModel.createDeliveries(driver.id) },
                onCancel = { sheetOpen = false }
            )
        }
    }
}

@Composable
private fun CreateDeliveriesSheet(
    driverName: String,
    quantity: Int,
    isCreating: Boolean,
    error: AdminMessage?,
    onQuantityChange: (Int) -> Unit,
    onConfirm: () -> Unit,
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
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Criar entregas",
                style = MaterialTheme.typography.headlineSmall,
                color = Text1
            )
            Text(
                text = "Gera entregas de demonstração em Araraquara-SP para $driverName, depois das que ele já tem.",
                style = MaterialTheme.typography.bodyMedium,
                color = Text3
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Quantidade", style = MaterialTheme.typography.titleMedium, color = Text1)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepperButton(
                    enabled = !isCreating && quantity > DemoDeliveryGenerator.MIN_QUANTITY,
                    onClick = { onQuantityChange(quantity - 1) }
                ) { Icon(Icons.Filled.Remove, contentDescription = "Diminuir", modifier = Modifier.size(20.dp)) }
                Text(quantity.toString(), style = MaterialTheme.typography.headlineSmall, color = Text1)
                StepperButton(
                    enabled = !isCreating && quantity < DemoDeliveryGenerator.MAX_QUANTITY,
                    onClick = { onQuantityChange(quantity + 1) }
                ) { Icon(Icons.Filled.Add, contentDescription = "Aumentar", modifier = Modifier.size(20.dp)) }
            }
        }

        if (error != null) MessageBanner(error, onDismiss = null)

        PrimaryButton(
            text = if (quantity == 1) "Criar 1 entrega" else "Criar $quantity entregas",
            onClick = onConfirm,
            isLoading = isCreating
        )
        SecondaryButton(text = "Cancelar", onClick = onCancel, enabled = !isCreating)
    }
}

@Composable
private fun StepperButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) Green100 else Surface3)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides if (enabled) Green900 else Text3
        ) { content() }
    }
}

@Composable
private fun MessageBanner(message: AdminMessage, onDismiss: (() -> Unit)?) {
    val background = if (message.isError) Red100 else Green100
    val content = if (message.isError) Red600 else Green900
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(16.dp))
            .then(if (onDismiss != null) Modifier.clickable(onClick = onDismiss) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message.text,
            style = MaterialTheme.typography.bodyMedium,
            color = content,
            modifier = Modifier.weight(1f)
        )
        if (onDismiss != null) {
            Text("OK", style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

@Composable
private fun VehicleRow(vehicle: AdminVehicle) {
    IFRotaCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).background(Green100, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalShipping,
                    contentDescription = null,
                    tint = Green600,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = vehicle.plate.ifBlank { "Sem placa" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Text1
                )
                Text(
                    text = vehicle.model.ifBlank { "Modelo não informado" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (vehicle.vehicleType.isNotBlank()) MetaChip(vehicle.vehicleType)
                if (vehicle.capacityKg > 0) {
                    Text(
                        text = "%.0f kg".format(vehicle.capacityKg),
                        style = MaterialTheme.typography.labelSmall,
                        color = Text3
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryRow(delivery: DeliveryEntity) {
    val status = if (delivery.isDone) DeliveryStatus.DONE else DeliveryStatus.PENDING
    IFRotaCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeliveryOrderBadge(order = delivery.orderNumber, status = status)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = delivery.recipient,
                    style = MaterialTheme.typography.titleSmall,
                    color = Text1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${delivery.address} · ${delivery.neighborhood}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = delivery.packageType,
                    style = MaterialTheme.typography.bodySmall,
                    color = Text3
                )
            }
            DeliveryStatusBadge(status = status)
        }
    }
}
