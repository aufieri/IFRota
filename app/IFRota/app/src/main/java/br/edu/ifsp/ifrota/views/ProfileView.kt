package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.edu.ifsp.ifrota.ui.components.GreenHeader
import br.edu.ifsp.ifrota.ui.components.IFRotaCard
import br.edu.ifsp.ifrota.ui.components.IFRotaTextField
import br.edu.ifsp.ifrota.ui.components.InitialsAvatar
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.components.SecondaryButton
import br.edu.ifsp.ifrota.ui.components.SectionLabel
import br.edu.ifsp.ifrota.ui.components.StatTile
import br.edu.ifsp.ifrota.ui.components.initials
import br.edu.ifsp.ifrota.ui.theme.BorderSubtle
import br.edu.ifsp.ifrota.ui.theme.Green100
import br.edu.ifsp.ifrota.ui.theme.Green50
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Green900
import br.edu.ifsp.ifrota.ui.theme.Red600
import br.edu.ifsp.ifrota.ui.theme.Red50
import br.edu.ifsp.ifrota.ui.theme.RedBorder
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Surface2
import br.edu.ifsp.ifrota.ui.theme.Surface3
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text2
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileView(
    viewModel: ProfileViewModel,
    accountEmail: String,
    onLogout: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val vehicleCount by viewModel.vehicleCount.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditing.collectAsStateWithLifecycle()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val email = profile?.email?.takeIf { it.isNotBlank() } ?: accountEmail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface2)
    ) {
        GreenHeaderTitle(title = "Perfil & Configurações")

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IFRotaCard(modifier = Modifier.offset(y = (-12).dp)) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InitialsAvatar(initials = profile.initials(accountEmail))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = profile?.name?.takeIf { it.isNotBlank() }
                                ?: email.substringBefore('@'),
                            style = MaterialTheme.typography.titleLarge,
                            color = Text1
                        )
                        Text(
                            text = email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text3
                        )
                        Text(
                            text = "Entregador ativo",
                            style = MaterialTheme.typography.labelSmall,
                            color = Green900,
                            modifier = Modifier
                                .background(Green100, CircleShape)
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(onClick = viewModel::startEditing) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Editar perfil",
                            tint = Green600,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(value = vehicleCount.toString(), label = "Veículos")
                StatTile(
                    value = if (profile?.cnh?.isNotBlank() == true) "OK" else "—",
                    label = "CNH informada"
                )
                StatTile(
                    value = if (profile?.isSynced == true) "OK" else "…",
                    label = "Perfil sincronizado"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Meus dados")
                IFRotaCard {
                    InfoRow(
                        icon = Icons.Default.MailOutline,
                        label = "E-mail",
                        value = email
                    )
                    RowDivider()
                    InfoRow(
                        icon = Icons.Default.PhoneAndroid,
                        label = "Telefone",
                        value = profile?.phone?.takeIf { it.isNotBlank() } ?: "Não informado"
                    )
                    RowDivider()
                    InfoRow(
                        icon = Icons.Default.Badge,
                        label = "CNH",
                        value = profile?.cnh?.takeIf { it.isNotBlank() } ?: "Não informada"
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionLabel(text = "Aplicativo")
                IFRotaCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Versão do app",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Text1
                        )
                        Text(
                            text = "IFRota 1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = Text3
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Red50, RoundedCornerShape(16.dp))
                    .border(1.dp, RedBorder, RoundedCornerShape(16.dp))
                    .clickable(onClick = onLogout)
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Red600,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Sair da conta",
                    style = MaterialTheme.typography.labelLarge,
                    color = Red600,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }

    if (isEditing) {
        ModalBottomSheet(
            onDismissRequest = viewModel::cancelEditing,
            sheetState = sheetState,
            containerColor = Surface1,
            dragHandle = null
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
                        text = "Editar perfil",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Text1
                    )
                    Text(
                        text = "O e-mail vem da sua conta e não muda por aqui.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Text3
                    )
                }

                IFRotaTextField(
                    label = "Nome completo",
                    value = form.name,
                    onValueChange = viewModel::onNameChange,
                    placeholder = "Marcos Oliveira",
                    capitalization = KeyboardCapitalization.Words,
                    errorMessage = form.nameError
                )

                IFRotaTextField(
                    label = "Telefone",
                    value = form.phone,
                    onValueChange = viewModel::onPhoneChange,
                    placeholder = "(11) 90000-0000",
                    keyboardType = KeyboardType.Phone,
                    capitalization = KeyboardCapitalization.None
                )

                IFRotaTextField(
                    label = "CNH",
                    value = form.cnh,
                    onValueChange = viewModel::onCnhChange,
                    placeholder = "00000000000",
                    keyboardType = KeyboardType.Number,
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Done
                )

                PrimaryButton(
                    text = "Salvar",
                    onClick = viewModel::saveProfile,
                    isLoading = form.isSaving
                )

                SecondaryButton(text = "Cancelar", onClick = viewModel::cancelEditing)
            }
        }
    }
}

@Composable
private fun GreenHeaderTitle(title: String) {
    GreenHeader {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Green50, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Green600,
                modifier = Modifier.size(20.dp)
            )
        }
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Text3
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = Text2
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        thickness = 1.dp,
        color = Surface3,
        modifier = Modifier.padding(start = 74.dp)
    )
}

