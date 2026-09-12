package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val showPassword: Boolean = false,
    val error: String? = null,
    val isLoading: Boolean = false,
    val loggedInUserId: String? = null
)

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value, error = null) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value, error = null) }

    fun togglePasswordVisibility() = _uiState.update { it.copy(showPassword = !it.showPassword) }

    fun signIn() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Preencha seu e-mail e senha para continuar.") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                val result = auth.signInWithEmailAndPassword(state.email.trim(), state.password).await()
                _uiState.update {
                    it.copy(isLoading = false, loggedInUserId = result.user?.uid)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.toMessage()) }
            }
        }
    }

    fun consumeLogin() = _uiState.update { it.copy(loggedInUserId = null) }
}

internal fun Exception.toMessage(): String = when (this) {
    is FirebaseAuthInvalidUserException -> "Não encontramos uma conta com esse e-mail."
    is FirebaseAuthInvalidCredentialsException -> "E-mail ou senha incorretos."
    else -> message ?: "Não foi possível concluir. Tente novamente."
}
