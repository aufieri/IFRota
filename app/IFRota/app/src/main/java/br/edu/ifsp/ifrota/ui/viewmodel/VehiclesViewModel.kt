package br.edu.ifsp.ifrota.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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

data class VehicleFormState(
    val editingId: String? = null,
    val plate: String = "",
    val model: String = "",
    val vehicleType: String = "",
    val capacityKg: String = "",
    val plateError: String? = null,
    val modelError: String? = null,
    val isSaving: Boolean = false
) {
    val isEditing: Boolean get() = editingId != null
}

class VehiclesViewModel(
    private val repository: VehicleRepository
) : ViewModel() {

    private val _form = MutableStateFlow(VehicleFormState())
    val form: StateFlow<VehicleFormState> = _form.asStateFlow()

    private val _isFormOpen = MutableStateFlow(false)
    val isFormOpen: StateFlow<Boolean> = _isFormOpen.asStateFlow()

    val vehicles: StateFlow<List<VehicleEntity>> = repository.getMyVehicles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onPlateChange(value: String) =
        _form.update { it.copy(plate = value.uppercase(), plateError = null) }

    fun onModelChange(value: String) = _form.update { it.copy(model = value, modelError = null) }

    fun onVehicleTypeChange(value: String) = _form.update { it.copy(vehicleType = value) }

    fun onCapacityChange(value: String) =
        _form.update { it.copy(capacityKg = value.filter { c -> c.isDigit() || c == '.' || c == ',' }) }

    fun openNewVehicleForm() {
        _form.value = VehicleFormState()
        _isFormOpen.value = true
    }

    fun openEditForm(vehicle: VehicleEntity) {
        _form.value = VehicleFormState(
            editingId = vehicle.id,
            plate = vehicle.plate,
            model = vehicle.model,
            vehicleType = vehicle.vehicleType,
            capacityKg = if (vehicle.capacityKg > 0) vehicle.capacityKg.toString() else ""
        )
        _isFormOpen.value = true
    }

    fun closeForm() {
        _isFormOpen.value = false
        _form.value = VehicleFormState()
    }

    fun saveVehicle() {
        val state = _form.value

        val plateError = if (state.plate.isBlank()) "Placa é obrigatória" else null
        val modelError = if (state.model.isBlank()) "Modelo é obrigatório" else null

        if (plateError != null || modelError != null) {
            _form.update { it.copy(plateError = plateError, modelError = modelError) }
            return
        }

        _form.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val vehicle = VehicleEntity(
                id = state.editingId ?: UUID.randomUUID().toString(),
                plate = state.plate.trim(),
                model = state.model.trim(),
                vehicleType = state.vehicleType.trim(),
                capacityKg = state.capacityKg.replace(',', '.').toDoubleOrNull() ?: 0.0
            )

            if (state.isEditing) {
                repository.updateVehicle(vehicle)
            } else {
                repository.saveVehicle(vehicle)
            }

            closeForm()
        }
    }

    fun deleteVehicle(vehicle: VehicleEntity) {
        viewModelScope.launch { repository.deleteVehicle(vehicle) }
    }
}

class VehiclesViewModelFactory(
    private val repository: VehicleRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(VehiclesViewModel::class.java)) {
            "Unknown ViewModel class: ${modelClass.name}"
        }
        return VehiclesViewModel(repository) as T
    }
}
