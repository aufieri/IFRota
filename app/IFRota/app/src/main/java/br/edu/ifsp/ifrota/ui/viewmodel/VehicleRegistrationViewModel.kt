package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.edu.ifsp.ifrota.data.local.entity.VehicleEntity
import br.edu.ifsp.ifrota.data.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class VehicleRegistrationUiState(
    val plate: String = "",
    val model: String = "",
    val vehicleType: String = "",
    val capacityKg: String = "",
    val plateError: String? = null,
    val modelError: String? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class VehicleRegistrationViewModel(
    private val repository: VehicleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(VehicleRegistrationUiState())
    val uiState: StateFlow<VehicleRegistrationUiState> = _uiState.asStateFlow()

    val vehicles: StateFlow<List<VehicleEntity>> = repository.getAllVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPlateChange(value: String) {
        _uiState.update { it.copy(plate = value, plateError = null) }
    }

    fun onModelChange(value: String) {
        _uiState.update { it.copy(model = value, modelError = null) }
    }

    fun onVehicleTypeChange(value: String) {
        _uiState.update { it.copy(vehicleType = value) }
    }

    fun onCapacityChange(value: String) {
        _uiState.update { it.copy(capacityKg = value) }
    }

    fun saveVehicle() {
        val state = _uiState.value

        val plateError = if (state.plate.isBlank()) "Placa é obrigatória" else null
        val modelError = if (state.model.isBlank()) "Modelo é obrigatório" else null

        if (plateError != null || modelError != null) {
            _uiState.update { it.copy(plateError = plateError, modelError = modelError) }
            return
        }

        _uiState.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val vehicle = VehicleEntity(
                id = UUID.randomUUID().toString(),
                plate = state.plate.trim(),
                model = state.model.trim(),
                vehicleType = state.vehicleType.trim(),
                capacityKg = state.capacityKg.toDoubleOrNull() ?: 0.0
            )
            repository.saveVehicle(vehicle)
            _uiState.value = VehicleRegistrationUiState(saveSuccess = true)
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }

    fun consumeSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
