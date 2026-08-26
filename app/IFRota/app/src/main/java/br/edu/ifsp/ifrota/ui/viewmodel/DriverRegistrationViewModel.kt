package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.local.entity.DriverEntity
import br.edu.ifsp.ifrota.data.repository.DriverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DriverRegistrationUiState(
    val name: String = "",
    val cnh: String = "",
    val phone: String = "",
    val nameError: String? = null,
    val cnhError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class DriverRegistrationViewModel(
    private val repository: DriverRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverRegistrationUiState())
    val uiState: StateFlow<DriverRegistrationUiState> = _uiState.asStateFlow()

    val drivers: StateFlow<List<DriverEntity>> = repository.getAllDrivers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onCnhChange(value: String) {
        _uiState.update { it.copy(cnh = value, cnhError = null) }
    }

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value) }
    }

    fun saveDriver() {
        val state = _uiState.value

        val nameError = if (state.name.isBlank()) "Nome é obrigatório" else null
        val cnhError = if (state.cnh.isBlank()) "CNH é obrigatória" else null

        if (nameError != null || cnhError != null) {
            _uiState.update { it.copy(nameError = nameError, cnhError = cnhError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val driver = DriverEntity(
                id = UUID.randomUUID().toString(),
                name = state.name.trim(),
                cnh = state.cnh.trim(),
                phone = state.phone.trim()
            )
            repository.saveDriver(driver)
            _uiState.value = DriverRegistrationUiState(saveSuccess = true)
        }
    }

    fun deleteDriver(driver: DriverEntity) {
        viewModelScope.launch { repository.deleteDriver(driver) }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
