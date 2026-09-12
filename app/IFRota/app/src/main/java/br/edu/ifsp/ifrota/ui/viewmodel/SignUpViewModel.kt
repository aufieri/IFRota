package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.local.dao.DriverDao
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SignUpUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val cnh: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val showPassword: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val createdUserId: String? = null
)

class SignUpViewModel(
    private val driverDao: DriverDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    fun onNameChange(value: String) = _uiState.update { it.copy(name = value, nameError = null, error = null) }

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, emailError = null, error = null) }

    fun onPhoneChange(value: String) = _uiState.update { it.copy(phone = value, error = null) }

    fun onCnhChange(value: String) = _uiState.update { it.copy(cnh = value, error = null) }

    fun onPasswordChange(value: String) =
        _uiState.update { it.copy(password = value, passwordError = null, error = null) }

    fun onConfirmPasswordChange(value: String) =
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null, error = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun signUp() {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Informe seu nome completo" else null
        val emailError = when {
            state.email.isBlank() -> "Informe seu e-mail"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email.trim()).matches() ->
                "E-mail inválido"
            else -> null
        }
        val passwordError = when {
            state.password.isBlank() -> "Crie uma senha"
            state.password.length < 6 -> "A senha precisa de ao menos 6 caracteres"
            else -> null
        }
        val confirmError = when {
            state.confirmPassword.isBlank() -> "Repita a senha"
            state.confirmPassword != state.password -> "As senhas não conferem"
            else -> null
        }

        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmError
                )
            }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val email = state.email.trim()
                val result = auth.createUserWithEmailAndPassword(email, state.password).await()
                val user = result.user ?: error("Conta criada sem usuário associado.")

                withContext(NonCancellable) {
                    user.updateProfile(
                        userProfileChangeRequest { displayName = state.name.trim() }
                    ).await()

                    driverDao.upsert(
                        DriverEntity(
                            id = user.uid,
                            name = state.name.trim(),
                            email = email,
                            phone = state.phone.trim(),
                            cnh = state.cnh.trim(),
                            isSynced = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                _uiState.update { it.copy(isLoading = false, createdUserId = user.uid) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toSignUpMessage()) }
            }
        }
    }

    fun consumeSignUp() = _uiState.update { it.copy(createdUserId = null) }
}

private fun Exception.toSignUpMessage(): String = when (this) {
    is FirebaseAuthUserCollisionException -> "Já existe uma conta com esse e-mail."
    is FirebaseAuthWeakPasswordException -> "Senha muito fraca. Use ao menos 6 caracteres."
    else -> toMessage()
}

class SignUpViewModelFactory(
    private val driverDao: DriverDao
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return SignUpViewModel(driverDao) as T
    }
}
