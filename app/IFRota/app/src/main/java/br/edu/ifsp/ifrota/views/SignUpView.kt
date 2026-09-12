package br.edu.ifsp.ifrota.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.edu.ifsp.ifrota.data.local.AppDatabase
import br.edu.ifsp.ifrota.ui.components.IFRotaTextField
import br.edu.ifsp.ifrota.ui.components.PrimaryButton
import br.edu.ifsp.ifrota.ui.theme.Green600
import br.edu.ifsp.ifrota.ui.theme.Surface1
import br.edu.ifsp.ifrota.ui.theme.Text1
import br.edu.ifsp.ifrota.ui.theme.Text3
import br.edu.ifsp.ifrota.ui.viewmodel.SignUpViewModel
import br.edu.ifsp.ifrota.ui.viewmodel.SignUpViewModelFactory

@Composable
fun SignUpView(onBack: () -> Unit) {
    val context = LocalContext.current
    val driverDao = remember(context) { AppDatabase.getDatabase(context).driverDao() }
    val viewModel: SignUpViewModel = viewModel(factory = SignUpViewModelFactory(driverDao))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Green600)
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Criar cadastro",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "Seus dados ficam salvos na sua conta",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Surface1, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Seus dados",
                    style = MaterialTheme.typography.titleLarge,
                    color = Text1
                )
                Text(
                    text = "Telefone e CNH são opcionais e podem ser preenchidos depois no Perfil.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text3
                )
            }

            IFRotaTextField(
                label = "Nome completo",
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                placeholder = "Marcos Oliveira",
                capitalization = KeyboardCapitalization.Words,
                errorMessage = uiState.nameError
            )

            IFRotaTextField(
                label = "E-mail",
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = "seu@email.com",
                keyboardType = KeyboardType.Email,
                capitalization = KeyboardCapitalization.None,
                errorMessage = uiState.emailError
            )

            IFRotaTextField(
                label = "Telefone",
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                placeholder = "(11) 90000-0000",
                keyboardType = KeyboardType.Phone,
                capitalization = KeyboardCapitalization.None
            )

            IFRotaTextField(
                label = "CNH",
                value = uiState.cnh,
                onValueChange = viewModel::onCnhChange,
                placeholder = "00000000000",
                keyboardType = KeyboardType.Number,
                capitalization = KeyboardCapitalization.None
            )

            IFRotaTextField(
                label = "Senha",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                placeholder = "Mínimo de 6 caracteres",
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                isPassword = true,
                passwordVisible = uiState.showPassword,
                onTogglePassword = viewModel::togglePasswordVisibility,
                errorMessage = uiState.passwordError
            )

            IFRotaTextField(
                label = "Confirmar senha",
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                placeholder = "Repita a senha",
                keyboardType = KeyboardType.Password,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done,
                isPassword = true,
                passwordVisible = uiState.showPassword,
                errorMessage = uiState.confirmPasswordError
            )

            uiState.error?.let { ErrorBanner(message = it) }

            PrimaryButton(
                text = "Criar conta",
                onClick = viewModel::signUp,
                isLoading = uiState.isLoading
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Já tem conta?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Text3
                )
                TextButton(onClick = onBack) {
                    Text(
                        text = "Entrar",
                        style = MaterialTheme.typography.labelMedium,
                        color = Green600
                    )
                }
            }
        }
    }
}
